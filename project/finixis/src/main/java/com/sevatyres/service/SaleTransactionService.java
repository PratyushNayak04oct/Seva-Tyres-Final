package com.sevatyres.service;

import com.sevatyres.model.Customer;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.TransactionLineItem;
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
     * Saves a sale transaction, then:
     *  1. Deducts stock from Inventory if an inventory item was linked.
     *  2. Creates a Transaction_Credit entry if there is a remaining unpaid balance.
     */
    public SaleTransaction save(SaleTransaction tx) {
        // Auto-generate bill number if blank
        if (tx.getBillNo() == null || tx.getBillNo().isBlank()) {
            tx.setBillNo("B" + System.currentTimeMillis() % 100000);
        }

        tx.computeTotal();
        SaleTransaction saved = repo.save(tx);

        // Deduct inventory stock
        if (saved.getInventoryItemId() != null && saved.getQuantity() > 0) {
            try {
                new com.sevatyres.repository.impl.JdbcInventoryRepository()
                        .adjustStock(saved.getInventoryItemId(), -saved.getQuantity());
            } catch (Exception e) {
                System.err.println("[SaleTransaction] Stock deduction failed: " + e.getMessage());
            }
        }

        // Auto-create credit entry if remaining balance > 0
        if (saved.getCreditAmount() > 0.009 && saved.getCustomerName() != null
                && !saved.getCustomerName().isBlank()) {
            try {
                // Ensure customer exists in DB
                CustomerService custService = new CustomerService();
                Customer customer = null;
                if (saved.getCustomerId() != null) {
                    customer = custService.getAll().stream()
                            .filter(c -> c.getId() == saved.getCustomerId())
                            .findFirst().orElse(null);
                }
                if (customer == null) {
                    customer = custService.addCustomer(
                            saved.getCustomerName(),
                            saved.getCustomerPhone(),
                            saved.getCustomerEmail(),
                            saved.getCustomerAddress());
                    saved.setCustomerId(customer.getId());
                    repo.update(saved);
                }

                TransactionService txnService = new TransactionService();
                String note = "Credit from Bill " + saved.getBillNo() + " — " + saved.getParticulars();
                txnService.addDebit(customer.getId(), customer.getName(), saved.getCreditAmount(), note);
            } catch (Exception e) {
                System.err.println("[SaleTransaction] Credit entry creation failed: " + e.getMessage());
            }
        }

        return saved;
    }

    public void update(SaleTransaction tx) {
        tx.computeTotal();
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
