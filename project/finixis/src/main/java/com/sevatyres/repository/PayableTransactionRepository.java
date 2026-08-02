package com.sevatyres.repository;

import com.sevatyres.model.PayableTransaction;

import java.util.List;
import java.util.Optional;

public interface PayableTransactionRepository {
    List<PayableTransaction> findAll();
    Optional<PayableTransaction> findById(int id);
    Optional<PayableTransaction> findByNumber(String txnNumber);
    /** Next unused 6-digit number (100000–999999). */
    String nextTxnNumber();
    PayableTransaction save(PayableTransaction txn);
    void delete(int id);
}
