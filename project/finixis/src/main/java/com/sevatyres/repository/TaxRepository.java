package com.sevatyres.repository;

import com.sevatyres.model.Tax;

import java.util.List;
import java.util.Optional;

public interface TaxRepository {
    List<Tax> findAll();
    List<Tax> findActive();
    Optional<Tax> findById(int id);
    Tax save(Tax tax);
    void update(Tax tax);
    void delete(int id);
}
