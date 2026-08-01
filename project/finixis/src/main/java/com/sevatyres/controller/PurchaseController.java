package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.PriceListImportService;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class PurchaseController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Button addBtn, importBtn, templateBtn;
    @FXML private TableView<PurchaseInfo> table;
    @FXML private TableColumn<PurchaseInfo, String> nameCol, brandCol, rimCol, sizeCol,
            patternCol, kindCol, codeCol, notesCol;
    @FXML private TableColumn<PurchaseInfo, Double> priceCol, rcpCol, mrpCol;
    @FXML private TableColumn<PurchaseInfo, PurchaseInfo> actionCol;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        brandCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getBrand())));
        rimCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getRimSize())));
        sizeCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getTyreSize())));
        patternCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getPattern())));
        kindCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getTyreKind())));
        codeCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getProductCode())));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("buyingPrice"));
        rcpCol.setCellValueFactory(new PropertyValueFactory<>("rcp"));
        mrpCol.setCellValueFactory(new PropertyValueFactory<>("mrp"));
        notesCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(dash(cell.getValue().getNotes())));
        actionCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));

        priceCol.setCellFactory(col -> moneyCell());
        rcpCol.setCellFactory(col -> moneyCell());
        mrpCol.setCellFactory(col -> moneyCell());

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit", new FontIcon("fas-pencil-alt"));
            private final Button delBtn = new Button("Delete", new FontIcon("fas-trash"));
            {
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                editBtn.setOnAction(e -> {
                    PurchaseInfo p = getItem();
                    if (p != null) Dialogs.showPurchaseInfo(p, updated -> load());
                });
                delBtn.setOnAction(e -> {
                    PurchaseInfo p = getItem();
                    if (p == null) return;
                    if (!Dialogs.confirm("Delete Purchase Info",
                            "Delete \"" + p.getItemName() + "\"?",
                            "This removes the buying-price record only.")) return;
                    AppServices.purchases().delete(p.getId());
                    load();
                    UiUtil.toast(App.getRoot(), "Purchase info deleted");
                });
            }
            @Override protected void updateItem(PurchaseInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(8, editBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        load();
    }

    private static TableCell<PurchaseInfo, Double> moneyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null || v <= 0 ? "" : UiUtil.money(v));
            }
        };
    }

    private static String dash(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private void load() {
        List<PurchaseInfo> all = AppServices.purchases().getAll();
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        if (!q.isEmpty()) {
            all = all.stream().filter(p -> matches(p, q)).toList();
        }
        table.getItems().setAll(all);
    }

    private static boolean matches(PurchaseInfo p, String q) {
        return contains(p.getItemName(), q)
                || contains(p.getBrand(), q)
                || contains(p.getRimSize(), q)
                || contains(p.getTyreSize(), q)
                || contains(p.getPattern(), q)
                || contains(p.getTyreKind(), q)
                || contains(p.getProductCode(), q)
                || contains(p.getNotes(), q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    @FXML private void onSearch() { load(); }

    @FXML private void onAdd() {
        Dialogs.showPurchaseInfo(null, saved -> load());
    }

    @FXML private void onTemplate() {
        try {
            Path dir = Path.of(System.getProperty("user.home"), "Downloads", "SevaTypres");
            java.nio.file.Files.createDirectories(dir);
            Path file = dir.resolve("price-list-template.csv");
            new PriceListImportService().writeTemplate(file);
            Dialogs.info("CSV Template Saved",
                    "Template saved to:\n" + file + "\n\n"
                            + "1. Open the PDF price list\n"
                            + "2. Copy/type rows into this CSV (Excel works)\n"
                            + "3. Use Import CSV on this page\n\n"
                            + "Columns: item_name, brand, rim, size, pattern, type, product_code, "
                            + "buying_price, rcp, mrp, notes, qty");
            UiUtil.toast(App.getRoot(), "Template saved to Downloads/SevaTypres");
        } catch (Exception ex) {
            Dialogs.info("Could not write template", ex.getMessage());
        }
    }

    @FXML private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import price list CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (file == null) return;

        boolean alsoInv = Dialogs.confirm("Import options",
                "Also create / update Inventory?",
                "Yes = add Purchase Info and matching tyre stock rows (uses RCP as sell price).\n"
                        + "No = Purchase Info only.");

        try {
            var result = new PriceListImportService().importCsv(file.toPath(), alsoInv);
            load();
            StringBuilder msg = new StringBuilder();
            msg.append("Purchase rows saved: ").append(result.purchased()).append("\n");
            if (alsoInv) {
                msg.append("Inventory created: ").append(result.inventoryCreated()).append("\n");
                msg.append("Inventory updated: ").append(result.inventoryUpdated()).append("\n");
            }
            if (!result.errors().isEmpty()) {
                msg.append("\nIssues (").append(result.errors().size()).append("):\n");
                result.errors().stream().limit(12).forEach(e -> msg.append("• ").append(e).append("\n"));
                if (result.errors().size() > 12) msg.append("…\n");
            }
            Dialogs.info("Import finished", msg.toString());
            UiUtil.toast(App.getRoot(), result.purchased() + " purchase rows imported");
        } catch (Exception ex) {
            Dialogs.info("Import failed", ex.getMessage() != null ? ex.getMessage() : "Unknown error");
        }
    }
}
