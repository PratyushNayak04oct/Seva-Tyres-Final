package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Customer;
import com.finixis.service.AppServices;
import com.finixis.service.CustomerService;
import com.finixis.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class AccountsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private TableView<Customer> table;
    @FXML private TableColumn<Customer, String> nameCol, contactCol;
    @FXML private TableColumn<Customer, LocalDate> sinceCol;
    @FXML private TableColumn<Customer, Double> balanceCol;
    @FXML private TableColumn<Customer, Customer> actionCol;

    private CustomerService customerService;
    private List<Customer> allCustomers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        customerService = AppServices.customers();
        allCustomers = customerService.getAll();

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        contactCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        sinceCol.setCellValueFactory(new PropertyValueFactory<>("customerSince"));
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        sinceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });

        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                getStyleClass().removeAll("text-success", "text-error", "text-normal");
                if (empty || v == null) { setText(""); setStyle(""); return; }
                setText(UiUtil.signedMoney(v));
                setStyle("-fx-font-weight:700;");
                if (v > 0) getStyleClass().add("text-success");
                else if (v < 0) getStyleClass().add("text-error");
                else getStyleClass().add("text-normal");
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            {
                openBtn.getStyleClass().addAll("btn", "btn-secondary");
                openBtn.setPadding(new javafx.geometry.Insets(6, 12, 6, 12));
                openBtn.setMinWidth(80);
                openBtn.setOnAction(e -> App.getShell().openCustomer(
                        getTableView().getItems().get(getIndex()).getId()));
            }
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setGraphic(null); return; }
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(openBtn);
                box.setAlignment(javafx.geometry.Pos.CENTER);
                box.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
                setGraphic(box);
            }
        });

        refresh(allCustomers);
    }

    private void refresh(List<Customer> list) {
        table.getItems().setAll(list);
        countLabel.setText(list.size() + " customers");
    }

    @FXML private void onSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { refresh(allCustomers); return; }
        refresh(customerService.search(q));
    }

    @FXML private void onAddCustomer() {
        Dialogs.showAddCustomer(saved -> {
            allCustomers = customerService.getAll();
            refresh(allCustomers);
        });
    }
}
