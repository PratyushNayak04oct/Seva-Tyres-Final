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
    public Optional<InventoryItem> getByName(String name)       { return repo.findByName(name); }

    public InventoryItem addItem(String name, int quantity, double unitPrice) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setReorderLevel(10);
        return addItem(item);
    }

    public InventoryItem addItem(InventoryItem item) {
        ensureUniqueName(item.getName(), 0);
        item.setReorderLevel(10);
        return repo.save(item);
    }

    public InventoryItem updateItem(InventoryItem item) {
        ensureUniqueName(item.getName(), item.getId());
        return repo.save(item);
    }

    /** Throws IllegalArgumentException if another item already uses this name. */
    private void ensureUniqueName(String name, int excludeId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        repo.findByName(name.trim()).ifPresent(existing -> {
            if (existing.getId() != excludeId) {
                throw new IllegalArgumentException(
                        "An item named \"" + name.trim()
                                + "\" already exists. Use a different name or edit the existing item.");
            }
        });
    }

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
