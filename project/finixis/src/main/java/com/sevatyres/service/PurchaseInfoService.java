package com.sevatyres.service;

import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.repository.PurchaseInfoRepository;
import com.sevatyres.repository.impl.JdbcPurchaseInfoRepository;

import java.util.List;
import java.util.Optional;

public class PurchaseInfoService {

    private final PurchaseInfoRepository repo = new JdbcPurchaseInfoRepository();

    public List<PurchaseInfo> getAll() { return repo.findAll(); }

    public Optional<PurchaseInfo> getById(int id) { return repo.findById(id); }

    public PurchaseInfo save(PurchaseInfo info) {
        if (info.getItemName() == null || info.getItemName().isBlank()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (info.getBuyingPrice() < 0) {
            throw new IllegalArgumentException("Buying price cannot be negative.");
        }
        info.setItemName(info.getItemName().trim());
        return repo.save(info);
    }

    public void delete(int id) { repo.delete(id); }
}
