package com.sevatyres.repository;

import com.sevatyres.model.SaleTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleTransactionRepository {
    List<SaleTransaction>     findAll();
    Optional<SaleTransaction> findById(int id);
    List<SaleTransaction>     findByDateRange(LocalDate from, LocalDate to);
    List<SaleTransaction>     findByCustomerId(int customerId);
    SaleTransaction           save(SaleTransaction tx);
    void                      update(SaleTransaction tx);
    void                      delete(int id);
}
