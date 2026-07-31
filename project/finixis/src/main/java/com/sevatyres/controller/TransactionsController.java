package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.Customer;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.SaleTransactionService;
import com.sevatyres.viewmodel.FileGenerationService;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Transactions page — shows Sale Transactions in a table view.
 * Features: Create, View, Edit, Invoice, Generate Report.
 */
public class TransactionsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private TextField currentBillField;
    @FXML private Button updateBillBtn;
    @FXML private Label nextBillHint;
    @FXML private TableView<SaleTransaction> table;
    @FXML private TableColumn<SaleTransaction, LocalDate>        dateCol;
    @FXML private TableColumn<SaleTransaction, String>           billNoCol, particularsCol, brandCol, paymentCol, customerCol;
    @FXML private TableColumn<SaleTransaction, Integer>          qtyCol;
    @FXML private TableColumn<SaleTransaction, Double>           totalCol;
    @FXML private TableColumn<SaleTransaction, SaleTransaction>  actionCol;

    private SaleTransactionService saleService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        saleService = AppServices.saleTransactions();

        dateCol.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        billNoCol.setCellValueFactory(new PropertyValueFactory<>("billNo"));
        particularsCol.setCellValueFactory(new PropertyValueFactory<>("particulars"));
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        paymentCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getPaymentSummary()));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        customerCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getCustomerName() != null
                        ? cell.getValue().getCustomerName() : "\u2014"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });

        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : UiUtil.money(v));
                if (!empty) setStyle("-fx-font-weight:700;");
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn    = new Button("View",    new FontIcon("fas-eye"));
            private final Button editBtn    = new Button("Edit",    new FontIcon("fas-pencil-alt"));
            private final Button invoiceBtn = new Button("Invoice", new FontIcon("fas-file-invoice"));
            {
                viewBtn.getStyleClass().addAll("btn", "btn-secondary");
                viewBtn.setGraphicTextGap(5);
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setGraphicTextGap(5);
                invoiceBtn.getStyleClass().addAll("btn", "btn-secondary");
                invoiceBtn.setGraphicTextGap(5);

                viewBtn.setOnAction(e -> {
                    SaleTransaction t = getTableView().getItems().get(getIndex());
                    Dialogs.showViewSaleTransaction(t);
                });
                editBtn.setOnAction(e -> {
                    SaleTransaction t = getTableView().getItems().get(getIndex());
                    Dialogs.showEditSaleTransaction(t, () -> loadFromDb());
                });
                invoiceBtn.setOnAction(e -> {
                    SaleTransaction t = getTableView().getItems().get(getIndex());
                    generateInvoice(t);
                });
            }
            @Override protected void updateItem(SaleTransaction t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setGraphic(null); return; }
                HBox box = new HBox(6, viewBtn, editBtn, invoiceBtn);
                box.setAlignment(javafx.geometry.Pos.CENTER);
                box.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
                setGraphic(box);
            }
        });

        loadBillNumber();
        loadFromDb();
    }

    private void loadBillNumber() {
        if (currentBillField == null) return;
        var inv = AppServices.invoiceNumbers();
        currentBillField.setText(inv.getCurrentInvoiceNumber(LocalDate.now()));
        nextBillHint.setText("Next invoice will be " + inv.peekNextInvoiceNumber(LocalDate.now()));
    }

    private void loadFromDb() {
        String q = searchField.getText().toLowerCase().trim();
        List<SaleTransaction> all = saleService.getAll();
        List<SaleTransaction> filtered = q.isEmpty() ? all : all.stream()
                .filter(t -> (t.getParticulars() != null && t.getParticulars().toLowerCase().contains(q))
                          || (t.getBrand() != null && t.getBrand().toLowerCase().contains(q))
                          || (t.getBillNo() != null && t.getBillNo().contains(q))
                          || (t.getCustomerName() != null && t.getCustomerName().toLowerCase().contains(q)))
                .toList();
        table.getItems().setAll(filtered);
        loadBillNumber();
    }

    @FXML private void onUpdateBillNumber() {
        try {
            AppServices.invoiceNumbers().setCurrentInvoiceNumber(currentBillField.getText());
            loadBillNumber();
            UiUtil.toast(App.getRoot(), "Bill number updated");
        } catch (Exception ex) {
            Dialogs.info("Invalid Bill Number",
                    ex.getMessage() != null ? ex.getMessage()
                            : "Use format ST-26/27-069");
        }
    }

    private void generateInvoice(SaleTransaction t) {
        try {
            var gf = FileGenerationService.generateSaleInvoice(t);
            AppServices.reports().saveFile(gf);
            Dialogs.showFileDownloadedDialog(gf.getFile());
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(gf.getFile());
            }
        } catch (Exception ex) {
            Dialogs.info("Invoice Error", "Could not generate invoice:\n" + ex.getMessage());
        }
    }

    @FXML private void onSearch() { loadFromDb(); }

    @FXML private void onNewTransaction() {
        Dialogs.showNewSaleTransaction(saved -> {
            loadFromDb();
            loadBillNumber();
            UiUtil.toast(App.getRoot(), "Transaction saved (Bill No: " + saved.getBillNo() + ")");
        });
    }

    @FXML private void onAddTax() {
        Dialogs.showManageTaxes(() ->
                UiUtil.toast(App.getRoot(), "Taxes updated"));
    }

    @FXML private void onGenerateReport() {
        Dialogs.showGenerateTransactionReport(saleService);
    }
}
