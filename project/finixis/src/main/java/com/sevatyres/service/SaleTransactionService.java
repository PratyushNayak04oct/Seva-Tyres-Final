package com.sevatyres.service;

import com.sevatyres.model.Customer;
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

    /**
     * Saves a single-item sale transaction, then:
     *  1. Deducts stock from Inventory if an inventory item was linked.
     *  2. Creates a Transaction_Credit entry (NOT Debit) if there is an unpaid balance.
     */
    public SaleTransaction save(SaleTransaction tx) {
        return save(tx, List.of());
    }

    /**
     * Saves a multi-item sale transaction.
     *  1. Computes total from items (or falls back to unit_price × quantity).
     *  2. Deducts stock for each linked inventory item.
     *  3. Creates a Transaction_Credit entry if there is an unpaid balance.
     */
    public SaleTransaction save(SaleTransaction tx, List<SaleTransactionItem> items) {
        if (tx.getBillNo() == null || tx.getBillNo().isBlank()) {
            tx.setBillNo("B" + System.currentTimeMillis() % 100000);
        }

        // If multi-item, override total from items list
        if (!items.isEmpty()) {
            double itemsTotal = items.stream().mapToDouble(SaleTransactionItem::getLineTotal).sum();
            tx.setTotal(itemsTotal);
            double paid = tx.getPhonePe() + tx.getAccountTransfer() + tx.getCardSwipe()
                    + tx.getBajajFinance() + tx.getCash() + tx.getCheque();
            tx.setCreditAmount(Math.max(0, itemsTotal - paid));
            // Summarise particulars from items
            if (tx.getParticulars() == null || tx.getParticulars().isBlank()) {
                StringBuilder sb = new StringBuilder();
                for (SaleTransactionItem it : items) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(it.getItemName()).append(" ×").append(it.getQuantity());
                }
                tx.setParticulars(sb.toString());
            }
            tx.setQuantity(0);
            tx.setUnitPrice(0);
        } else {
            tx.computeTotal();
        }

        SaleTransaction saved = repo.save(tx);

        // Save line items
        if (!items.isEmpty()) {
            ((JdbcSaleTransactionRepository) repo).saveItems(saved.getId(), items);
        }

        // Deduct inventory stock per item (or single item)
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

        // Create Transaction_Credit (money customer OWES us) when there is remaining balance
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
                // Correctly records as Transaction_Credit (money owed to the business)
                txnService.addCreditForSale(customer.getId(), customer.getName(), saved.getCreditAmount(), note);
            } catch (Exception e) {
                System.err.println("[SaleTransaction] Transaction_Credit creation failed: " + e.getMessage());
            }
        }

        return saved;
    }

    /**
     * Persists payment / credit changes without recomputing from unitPrice×qty.
     * Callers must set paid amounts, creditAmount and total explicitly
     * (important for multi-item bills and edit-payment flows).
     */
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
