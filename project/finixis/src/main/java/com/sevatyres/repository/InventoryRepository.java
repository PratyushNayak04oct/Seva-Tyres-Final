package com.sevatyres.repository;

import com.sevatyres.model.InventoryItem;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    List<InventoryItem>     findAll();
    Optional<InventoryItem> findById(int id);
    Optional<InventoryItem> findByBarcode(String barcode);
    InventoryItem           save(InventoryItem item);
    void                    delete(int id);
    void                    adjustStock(int itemId, int delta);
}
