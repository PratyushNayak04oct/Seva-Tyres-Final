package com.finixis.repository;

import com.finixis.model.InventoryItem;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    List<InventoryItem>     findAll();
    Optional<InventoryItem> findById(int id);
    InventoryItem           save(InventoryItem item);
    void                    delete(int id);
    void                    adjustStock(int itemId, int delta); // positive = stock in, negative = stock out
}
