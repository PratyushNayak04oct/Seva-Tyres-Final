package com.finixis.repository;

import com.finixis.model.Transaction;
import com.finixis.model.TransactionLineItem;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository {

    // --- Credit transactions ---
    List<Transaction>     findAllCredits();
    List<Transaction>     findCreditsByCustomer(int customerId);
    List<Transaction>     findCreditsByDateRange(LocalDate from, LocalDate to);
    Transaction           saveCredit(Transaction tx, List<TransactionLineItem> items);
    void                  markSettled(int transactionId);

    // --- Debit transactions ---
    List<Transaction>     findAllDebits();
    List<Transaction>     findDebitsByCustomer(int customerId);
    Transaction           saveDebit(Transaction tx);

    // --- Combined view (both types, ordered by date desc) ---
    List<Transaction>     findAll();
    List<Transaction>     findByCustomer(int customerId);
    List<Transaction>     findByDateRange(LocalDate from, LocalDate to);

    // --- Partial payment ---
    void partialPayment(int id, double amt, LocalDate date);
}
