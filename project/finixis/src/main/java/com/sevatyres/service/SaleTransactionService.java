package com.sevatyres.service;

import com.sevatyres.model.Customer;
import com.sevatyres.model.SaleTaxLine;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
import com.sevatyres.repository.SaleTransactionRepository;
import com.sevatyres.repository.impl.JdbcSaleTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for Sale Transactions.
 * On save: deducts inventory stock and auto-creates a credit entry if there is
 * a remaining unpaid balance.
 */
public class SaleTransactionService {

    private final SaleTransactionRepository repo;

    public SaleTransactionService() {
        this.repo = new JdbcSaleTransactionRepository();
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

    /**
     * Saves a multi-item sale with optional applied taxes.
     * Grand total = item subtotal + tax amount. Credit = total − paid.
     */
    public SaleTransaction save(SaleTransaction tx, List<SaleTransactionItem> items,
                                List<SaleTaxLine> taxLines) {
        if (items == null) items = List.of();
        if (taxLines == null) taxLines = List.of();
        if (tx.getBillNo() == null || tx.getBillNo().isBlank()) {
            tx.setBillNo("B" + System.currentTimeMillis() % 100000);
        }

        if (!items.isEmpty()) {
            double itemsTotal = items.stream().mapToDouble(SaleTransactionItem::getLineTotal).sum();
            int totalQty = items.stream().mapToInt(SaleTransactionItem::getQuantity).sum();
            tx.setSubtotal(itemsTotal);
            double tax = Math.max(0, tx.getTaxAmount());
            if (tax <= 0 && !taxLines.isEmpty()) {
                tax = taxLines.stream().mapToDouble(SaleTaxLine::getTaxAmount).sum();
                tx.setTaxAmount(tax);
            }
            tx.setTotal(itemsTotal + tax);
            double paid = tx.getPhonePe() + tx.getAccountTransfer() + tx.getCardSwipe()
                    + tx.getBajajFinance() + tx.getCash() + tx.getCheque();
            tx.setCreditAmount(Math.max(0, tx.getTotal() - paid));
            if (tx.getParticulars() == null || tx.getParticulars().isBlank()) {
                StringBuilder sb = new StringBuilder();
                for (SaleTransactionItem it : items) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(it.getItemName()).append(" ×").append(it.getQuantity());
                }
                tx.setParticulars(sb.toString());
            }
            tx.setQuantity(Math.max(1, totalQty));
            if (tx.getUnitPrice() <= 0 && items.size() == 1) {
                tx.setUnitPrice(items.get(0).getUnitPrice());
            } else if (items.size() > 1) {
                tx.setUnitPrice(0);
            }
        } else {
            if (tx.getSubtotal() <= 0 && tx.getUnitPrice() > 0 && tx.getQuantity() > 0) {
                tx.setSubtotal(tx.getUnitPrice() * tx.getQuantity());
            }
            tx.setTotal(tx.getSubtotal() + Math.max(0, tx.getTaxAmount()));
            double paid = tx.getPhonePe() + tx.getAccountTransfer() + tx.getCardSwipe()
                    + tx.getBajajFinance() + tx.getCash() + tx.getCheque();
            tx.setCreditAmount(Math.max(0, tx.getTotal() - paid));
        }

        SaleTransaction saved = repo.save(tx);

        if (!items.isEmpty()) {
            ((JdbcSaleTransactionRepository) repo).saveItems(saved.getId(), items);
        }
        ((JdbcSaleTransactionRepository) repo).saveTaxes(saved.getId(), taxLines);

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
