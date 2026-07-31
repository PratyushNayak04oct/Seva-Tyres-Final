package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.service.AppServices;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PurchaseController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Button addBtn, uploadPdfBtn;
    @FXML private TableView<PurchaseInfo> table;
    @FXML private TableColumn<PurchaseInfo, String> nameCol;
    @FXML private TableColumn<PurchaseInfo, Double> priceCol;
    @FXML private TableColumn<PurchaseInfo, String> notesCol;
    @FXML private TableColumn<PurchaseInfo, PurchaseInfo> actionCol;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("buyingPrice"));
        notesCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().getNotes() != null && !cell.getValue().getNotes().isBlank()
                        ? cell.getValue().getNotes() : "—"));
        actionCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));

        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : UiUtil.money(v));
            }
        });

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

    private void load() {
        List<PurchaseInfo> all = AppServices.purchases().getAll();
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        if (!q.isEmpty()) {
            all = all.stream()
                    .filter(p -> p.getItemName() != null && p.getItemName().toLowerCase().contains(q))
                    .toList();
        }
        table.getItems().setAll(all);
    }

    @FXML private void onSearch() { load(); }

    @FXML private void onAdd() {
        Dialogs.showPurchaseInfo(null, saved -> load());
    }

    @FXML private void onUploadPdf() {
        Dialogs.pickPdfAndImport(() -> {
            load();
            UiUtil.toast(App.getRoot(), "PDF items imported");
        });
    }
}
