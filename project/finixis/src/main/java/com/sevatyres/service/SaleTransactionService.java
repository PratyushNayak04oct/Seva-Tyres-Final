package com.sevatyres.service;

import com.sevatyres.model.Customer;
import com.sevatyres.model.SaleTaxLine;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
import com.sevatyres.repository.SaleTransactionRepository;
import com.sevatyres.repository.impl.JdbcSaleTransactionRepository;
import com.sevatyres.util.GstUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for Sale Transactions.
 * Prices from inventory are tax-inclusive; lines store Rate / CGST / SGST.
 * Bill numbers follow ST-26/27-070 per financial year.
 */
public class SaleTransactionService {

    private final SaleTransactionRepository repo;
    private final InvoiceNumberService invoiceNumbers;
    private final ProfitService profitService;

    public SaleTransactionService() {
        this.repo = new JdbcSaleTransactionRepository();
        this.invoiceNumbers = new InvoiceNumberService();
        this.profitService = new ProfitService();
    }

    public List<SaleTransaction> getAll() { return repo.findAll(); }
    public Optional<SaleTransaction> getById(int id) { return repo.findById(id); }
    public List<SaleTransaction> getByDateRange(LocalDate from, LocalDate to) { return repo.findByDateRange(from, to); }
    public List<SaleTransaction> getByCustomer(int customerId) { return repo.findByCustomerId(customerId); }

    public List<SaleTransactionItem> getItems(int saleId) {
        return ((JdbcSaleTransactionRepository) repo).findItemsBySaleId(saleId);
    }

    public List<SaleTaxLine> getTaxes(int saleId) {
        return ((JdbcSaleTransactionRepository) repo).findTaxesBySaleId(saleId);
    }

    public SaleTransaction save(SaleTransaction tx) {
        return save(tx, List.of(), List.of());
    }

    public SaleTransaction save(SaleTransaction tx, List<SaleTransactionItem> items) {
        return save(tx, items, List.of());
    }

    public SaleTransaction save(SaleTransaction tx, List<SaleTransactionItem> items,
                                List<SaleTaxLine> taxLines) {
        if (items == null) items = List.of();
        if (taxLines == null) taxLines = List.of();

        LocalDate saleDate = tx.getSaleDate() != null ? tx.getSaleDate() : LocalDate.now();
        tx.setSaleDate(saleDate);
        if (tx.getBillNo() == null || tx.getBillNo().isBlank()) {
            tx.setBillNo(invoiceNumbers.nextInvoiceNumber(saleDate));
        } else {
            tx.setBillNo(tx.getBillNo().trim());
            invoiceNumbers.noteIssued(tx.getBillNo(), saleDate);
        }

        if (!items.isEmpty()) {
            double taxable = 0, cgst = 0, sgst = 0, inclusive = 0;
            int totalQty = 0;
            for (SaleTransactionItem it : items) {
                // Base = Inclusive ÷ 1.18 ; GST = Inclusive − Base ; CGST = SGST
                GstUtil.LineGst split = GstUtil.splitLine(it.getUnitPrice(), it.getQuantity());
                it.setTaxableAmount(split.taxable());
                it.setCgstAmount(split.cgst());
                it.setSgstAmount(split.sgst());
                it.setLineTotal(split.inclusiveTotal());
                it.setRate(it.getQuantity() > 0
                        ? GstUtil.round2(split.taxable() / it.getQuantity()) : 0);
                taxable += split.taxable();
                cgst += split.cgst();
                sgst += split.sgst();
                inclusive += split.inclusiveTotal();
                totalQty += it.getQuantity();
            }

            double discAmt = Math.max(0, tx.getDiscountAmount());
            double discPct = Math.max(0, tx.getDiscountPercent());
            if (discAmt <= 0.009 && discPct > 0) {
                discAmt = GstUtil.round2(inclusive * discPct / 100.0);
            }

            GstUtil.Totals totals = GstUtil.totalsFromLines(
                    taxable, cgst, sgst, inclusive, discAmt);

            tx.setDiscountAmount(totals.discount());
            tx.setSubtotal(totals.taxable());
            tx.setCgstTotal(totals.cgst());
            tx.setSgstTotal(totals.sgst());
            tx.setTaxAmount(GstUtil.round2(totals.cgst() + totals.sgst()));
            tx.setTaxLabel("CGST 9% + SGST 9%");
            // Round-off column = signed amount used to reach nearest rupee
            tx.setRoundOff(totals.roundOff());
            tx.setTotal(totals.grandTotal());
            tx.setQuantity(Math.max(1, totalQty));
            tx.setNetProfit(profitService.calculateNetProfit(tx, items));

            double paid = tx.getPhonePe() + tx.getAccountTransfer() + tx.getCardSwipe()
                    + tx.getBajajFinance() + tx.getCash() + tx.getCheque();
            tx.setCreditAmount(Math.max(0, GstUtil.round2(totals.grandTotal() - paid)));

            if (tx.getParticulars() == null || tx.getParticulars().isBlank()) {
                StringBuilder sb = new StringBuilder();
                for (SaleTransactionItem it : items) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(it.getItemName()).append(" ×").append(it.getQuantity());
                }
                tx.setParticulars(sb.toString());
            }
            if (tx.getUnitPrice() <= 0 && items.size() == 1) {
                tx.setUnitPrice(items.get(0).getUnitPrice());
            } else if (items.size() > 1) {
                tx.setUnitPrice(0);
            }
        } else {
            tx.setTotal(tx.getSubtotal() + Math.max(0, tx.getTaxAmount())
                    - Math.max(0, tx.getDiscountAmount()) + tx.getRoundOff());
            double paid = tx.getPhonePe() + tx.getAccountTransfer() + tx.getCardSwipe()
                    + tx.getBajajFinance() + tx.getCash() + tx.getCheque();
            tx.setCreditAmount(Math.max(0, tx.getTotal() - paid));
            tx.setNetProfit(profitService.calculateNetProfit(tx, items));
        }

        SaleTransaction saved = repo.save(tx);

        if (!items.isEmpty()) {
            ((JdbcSaleTransactionRepository) repo).saveItems(saved.getId(), items);
        }
        List<SaleTaxLine> autoTaxes = List.of(
                new SaleTaxLine(null, "CGST", 9.0, saved.getCgstTotal()),
                new SaleTaxLine(null, "SGST", 9.0, saved.getSgstTotal())
        );
        ((JdbcSaleTransactionRepository) repo).saveTaxes(saved.getId(),
                taxLines.isEmpty() ? autoTaxes : taxLines);

        if (!items.isEmpty()) {
            var invRepo = new com.sevatyres.repository.impl.JdbcInventoryRepository();
            for (SaleTransactionItem it : items) {
                if (it.getInventoryId() != null && it.getQuantity() > 0) {
                    try { invRepo.adjustStock(it.getInventoryId(), -it.getQuantity()); }
                    catch (Exception e) {
                        System.err.println("[SaleTransaction] Stock deduction failed for item "
                                + it.getItemName() + ": " + e.getMessage());
                    }
                }
            }
        } else if (saved.getInventoryItemId() != null && saved.getQuantity() > 0) {
            try {
                new com.sevatyres.repository.impl.JdbcInventoryRepository()
                        .adjustStock(saved.getInventoryItemId(), -saved.getQuantity());
            } catch (Exception e) {
                System.err.println("[SaleTransaction] Stock deduction failed: " + e.getMessage());
            }
        }

        if (saved.getCreditAmount() > 0.009
                && saved.getCustomerName() != null && !saved.getCustomerName().isBlank()) {
            try {
                CustomerService custService = new CustomerService();
                Customer customer = null;
                if (saved.getCustomerId() != null) {
                    customer = custService.getAll().stream()
                            .filter(c -> c.getId() == saved.getCustomerId())
                            .findFirst().orElse(null);
                }
                if (customer == null) {
                    customer = custService.addCustomer(
                            saved.getCustomerName(), saved.getCustomerPhone(),
                            saved.getCustomerEmail(), saved.getCustomerAddress());
                    saved.setCustomerId(customer.getId());
                    repo.update(saved);
                }

                TransactionService txnService = new TransactionService();
                String note = "Credit from Bill " + saved.getBillNo() + " — " + saved.getParticulars();
                txnService.addCreditForSale(customer.getId(), customer.getName(), saved.getCreditAmount(), note);

                try {
                    AppServices.alerts().ensureCreditPaymentCampaign();
                    List<SaleTransactionItem> emailItems = !items.isEmpty()
                            ? items
                            : ((JdbcSaleTransactionRepository) repo).findItemsBySaleId(saved.getId());
                    AppServices.email().sendCreditSaleSummary(customer, saved, emailItems);
                } catch (Exception mailEx) {
                    System.err.println("[SaleTransaction] Credit email failed: " + mailEx.getMessage());
                }
            } catch (Exception e) {
                System.err.println("[SaleTransaction] Transaction_Credit creation failed: " + e.getMessage());
            }
        }

        return saved;
    }

    public void update(SaleTransaction tx) {
        repo.update(tx);
    }

    public void delete(int id) { repo.delete(id); }

    public double totalRevenue() {
        return repo.findAll().stream().mapToDouble(SaleTransaction::getTotal).sum();
    }

    public double totalCredit() {
        return repo.findAll().stream().mapToDouble(SaleTransaction::getCreditAmount).sum();
    }
}
