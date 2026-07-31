package com.sevatyres.repository;

import com.sevatyres.model.PurchaseInfo;

import java.util.List;
import java.util.Optional;

public interface PurchaseInfoRepository {
    List<PurchaseInfo> findAll();
    Optional<PurchaseInfo> findById(int id);
    PurchaseInfo save(PurchaseInfo info);
    void delete(int id);
}
