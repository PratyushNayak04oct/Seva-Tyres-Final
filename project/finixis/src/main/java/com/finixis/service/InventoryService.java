package com.finixis.service;

import com.finixis.model.InventoryItem;
import com.finixis.repository.InventoryRepository;
import com.finixis.repository.impl.JdbcInventoryRepository;

import java.util.List;
import java.util.Optional;

public class InventoryService {

    private final InventoryRepository repo;

    public InventoryService() { this.repo = new JdbcInventoryRepository(); }

    public List<InventoryItem>     getAll()         { return repo.findAll(); }
    public Optional<InventoryItem> getById(int id)  { return repo.findById(id); }

    public InventoryItem addItem(String name, int quantity, double unitPrice) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setReorderLevel(10);
        return repo.save(item);
    }

    public InventoryItem updateItem(InventoryItem item) { return repo.save(item); }

    public void deleteItem(int id) { repo.delete(id); }

    /** Positive delta = Stock In; negative delta = Stock Out. */
    public void adjustStock(int itemId, int delta) {
        if (delta == 0) return;
        repo.adjustStock(itemId, delta);
    }

    public int getLowStockCount() {
        return (int) repo.findAll().stream().filter(InventoryItem::isLowStock).count();
    }
}
