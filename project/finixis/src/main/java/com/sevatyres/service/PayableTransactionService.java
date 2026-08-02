package com.sevatyres.service;

import com.sevatyres.model.PayableTransaction;
import com.sevatyres.repository.PayableTransactionRepository;
import com.sevatyres.repository.impl.JdbcPayableTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PayableTransactionService {

    private final PayableTransactionRepository repo = new JdbcPayableTransactionRepository();

    public List<PayableTransaction> getAll() { return repo.findAll(); }

    public List<PayableTransaction> getByDateRange(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.MIN;
        LocalDate t = to != null ? to : LocalDate.MAX;
        return repo.findAll().stream()
                .filter(p -> p.getTxnDate() != null
                        && !p.getTxnDate().isBefore(f)
                        && !p.getTxnDate().isAfter(t))
                .collect(Collectors.toList());
    }

    public Optional<PayableTransaction> getById(int id) { return repo.findById(id); }

    /** Allocates the next 6-digit payable number (consumes one sequence value). */
    public String allocateNumber() { return repo.nextTxnNumber(); }

    public PayableTransaction save(PayableTransaction txn) {
        if (txn.getPaidTo() == null || txn.getPaidTo().isBlank()) {
            throw new IllegalArgumentException("Paid to (name) is required.");
        }
        if (txn.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (txn.getTxnDate() == null) txn.setTxnDate(LocalDate.now());
        txn.setPaidTo(txn.getPaidTo().trim());
        if (txn.getNotes() != null) txn.setNotes(txn.getNotes().trim());
        if (txn.getId() == 0 && (txn.getTxnNumber() == null || txn.getTxnNumber().isBlank())) {
            txn.setTxnNumber(repo.nextTxnNumber());
        }
        return repo.save(txn);
    }

    public void delete(int id) { repo.delete(id); }
}
