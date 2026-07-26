package com.sevatyres.service;

import com.sevatyres.model.InventoryItem;
import com.sevatyres.repository.InventoryRepository;
import com.sevatyres.repository.impl.JdbcInventoryRepository;

import java.util.List;
import java.util.Optional;

public class InventoryService {

    private final InventoryRepository repo;

    public InventoryService() { this.repo = new JdbcInventoryRepository(); }

    public List<InventoryItem>     getAll()                     { return repo.findAll(); }
    public Optional<InventoryItem> getById(int id)              { return repo.findById(id); }
    public Optional<InventoryItem> getByBarcode(String barcode) { return repo.findByBarcode(barcode); }

    public InventoryItem addItem(String name, int quantity, double unitPrice) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setReorderLevel(10);
        return repo.save(item);
    }

    public InventoryItem addItem(InventoryItem item) {
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
        return (int) repo.findAll().stream()
                .filter(i -> i.getStockStatus() != InventoryItem.StockStatus.IN_STOCK)
                .count();
    }
}
