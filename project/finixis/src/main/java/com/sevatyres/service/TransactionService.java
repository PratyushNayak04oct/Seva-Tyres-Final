package com.sevatyres.service;

import com.sevatyres.model.Transaction;
import com.sevatyres.model.TransactionLineItem;
import com.sevatyres.repository.TransactionRepository;
import com.sevatyres.repository.impl.JdbcTransactionRepository;

import java.time.LocalDate;
import java.util.List;

public class TransactionService {

    private final TransactionRepository repo;

    public TransactionService() { this.repo = new JdbcTransactionRepository(); }

    // ---- Read ----

    public List<Transaction> getAll()                             { return repo.findAll(); }
    public List<Transaction> getByCustomer(int customerId)        { return repo.findByCustomer(customerId); }
    public List<Transaction> getByDateRange(LocalDate f, LocalDate t) { return repo.findByDateRange(f, t); }
    public List<Transaction> getAllCredits()                       { return repo.findAllCredits(); }
    public List<Transaction> getCreditsByCustomer(int customerId) { return repo.findCreditsByCustomer(customerId); }
    public List<Transaction> getAllDebits()                        { return repo.findAllDebits(); }
    public List<Transaction> getDebitsByCustomer(int customerId)  { return repo.findDebitsByCustomer(customerId); }

    // ---- Write ----

    public Transaction recordPayment(int customerId, String customerName,
                                     List<TransactionLineItem> items, double paidAmount,
                                     String notes) {
        double total = items.stream().mapToDouble(TransactionLineItem::getLineTotal).sum();
        Transaction tx = new Transaction();
        tx.setCustomerId(customerId);
        tx.setCustomerName(customerName);
        tx.setType(Transaction.Type.CREDIT);
        tx.setAmount(total);
        tx.setPaidAmount(paidAmount);
        tx.setBalance(total - paidAmount);
        tx.setDate(LocalDate.now());
        tx.setDescription(notes);
        tx.setOngoing(tx.getBalance() > 0);
        return repo.saveCredit(tx, items);
    }

    /**
     * Records money the customer owes the business (credit sale).
     * Creates a Transaction_Credit row with paid_amount=0, balance=amount.
     */
    public Transaction addCreditForSale(int customerId, String customerName,
                                        double amount, String notes) {
        Transaction tx = new Transaction();
        tx.setCustomerId(customerId);
        tx.setCustomerName(customerName);
        tx.setType(Transaction.Type.CREDIT);
        tx.setAmount(amount);
        tx.setPaidAmount(0);
        tx.setBalance(amount);
        tx.setDate(java.time.LocalDate.now());
        tx.setDescription(notes);
        tx.setOngoing(true);
        return repo.saveCredit(tx, java.util.List.of());
    }

    public Transaction addDebit(int customerId, String customerName,
                                double amount, String notes) {
        Transaction tx = new Transaction();
        tx.setCustomerId(customerId);
        tx.setCustomerName(customerName);
        tx.setType(Transaction.Type.DEBIT);
        tx.setAmount(amount);
        tx.setPaidAmount(0);
        tx.setBalance(amount);
        tx.setDate(LocalDate.now());
        tx.setDescription(notes);
        tx.setOngoing(true);
        return repo.saveDebit(tx);
    }

    public void markSettled(int transactionId) { repo.markSettled(transactionId); }

    public void recordPartialPayment(int transactionId, double amount, LocalDate date) {
        repo.partialPayment(transactionId, amount, date);
    }

    // ---- Summary stats ----

    public double totalCreditOutstanding() {
        return repo.findAllCredits().stream()
                .filter(Transaction::isOngoing)
                .mapToDouble(Transaction::getBalance).sum();
    }

    public double totalDebits() {
        return repo.findAllDebits().stream()
                .mapToDouble(Transaction::getAmount).sum();
    }
}
