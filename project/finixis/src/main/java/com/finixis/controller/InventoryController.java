package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.InventoryItem;
import com.finixis.service.AppServices;
import com.finixis.service.InventoryService;
import com.finixis.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InventoryController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Label lowStockLabel;
    @FXML private Button addItemBtn, stockBtn;
    @FXML private TableView<InventoryItem> table;
    @FXML private TableColumn<InventoryItem, String> nameCol, skuCol, catCol;
    @FXML private TableColumn<InventoryItem, Number> qtyCol;
    @FXML private TableColumn<InventoryItem, Double> priceCol;
    @FXML private TableColumn<InventoryItem, InventoryItem> stockCol, actionCol;

    private InventoryService inventoryService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inventoryService = AppServices.inventory();

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("name")); // no SKU in DB — reuse name
        catCol.setCellValueFactory(new PropertyValueFactory<>("name")); // no category in DB
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        stockCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : UiUtil.money(v));
            }
        });

        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label chip = new Label(item.isLowStock() ? "Low Stock" : "In Stock");
                chip.getStyleClass().addAll("chip", item.isLowStock() ? "chip-error" : "chip-success");
                setGraphic(chip);
                setText(null);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit", new FontIcon("fas-pencil-alt"));
            private final Button delBtn  = new Button("Delete", new FontIcon("fas-trash"));
            {
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setGraphicTextGap(6);
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                delBtn.setGraphicTextGap(6);

                editBtn.setOnAction(e -> {
                    InventoryItem it = getTableView().getItems().get(getIndex());
                    Dialogs.showEditItem(it, updated -> {
                        inventoryService.updateItem(updated);
                        loadFromDb();
                        UiUtil.toast(App.getRoot(), "\"" + updated.getName() + "\" updated");
                    });
                });
                delBtn.setOnAction(e -> {
                    InventoryItem it = getTableView().getItems().get(getIndex());
                    boolean ok = Dialogs.confirm("Delete Item",
                            "Delete \"" + it.getName() + "\"?",
                            "This will remove the item from the inventory list.");
                    if (ok) {
                        try {
                            inventoryService.deleteItem(it.getId());
                            loadFromDb();
                            UiUtil.toast(App.getRoot(), "\"" + it.getName() + "\" deleted");
                        } catch (Exception ex) {
                            Dialogs.info("Cannot Delete",
                                    "Item is referenced in existing transactions and cannot be deleted.");
                        }
                    }
                });
            }
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(10, editBtn, delBtn);
                box.setAlignment(javafx.geometry.Pos.CENTER);
                box.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
                setGraphic(box);
            }
        });

        loadFromDb();
    }

    private void loadFromDb() {
        List<InventoryItem> all = inventoryService.getAll();
        long low = all.stream().filter(InventoryItem::isLowStock).count();
        lowStockLabel.setText(low + " items need reorder");
        String q = searchField.getText().toLowerCase().trim();
        List<InventoryItem> filtered = q.isEmpty() ? all
                : all.stream().filter(i -> i.getName().toLowerCase().contains(q)).toList();
        table.getItems().setAll(filtered);
    }

    @FXML private void onSearch() { loadFromDb(); }

    @FXML private void onAddItem() {
        Dialogs.showAddItem(saved -> {
            inventoryService.addItem(saved.getName(), saved.getQuantity(), saved.getUnitPrice());
            loadFromDb();
            UiUtil.toast(App.getRoot(), "\"" + saved.getName() + "\" added to inventory");
        });
    }

    @FXML private void onStock() {
        List<InventoryItem> items = inventoryService.getAll();
        Dialogs.showStockAdjust(items, (itemId, delta) -> {
            inventoryService.adjustStock(itemId, delta);
            loadFromDb();
            UiUtil.toast(App.getRoot(), "Stock updated");
        });
    }
}
