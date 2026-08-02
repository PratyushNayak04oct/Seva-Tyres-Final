package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.PayableTransaction;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.TxnListEntry;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.SaleTransactionService;
import com.sevatyres.viewmodel.FileGenerationService;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Transactions page — Receivables (sales) and Payables in one table, colour-coded by type.
 */
public class TransactionsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private TextField currentBillField;
    @FXML private Button updateBillBtn;
    @FXML private Label nextBillHint;
    @FXML private TableView<TxnListEntry> table;
    @FXML private TableColumn<TxnListEntry, TxnListEntry> typeCol, actionCol;
    @FXML private TableColumn<TxnListEntry, LocalDate> dateCol;
    @FXML private TableColumn<TxnListEntry, String> numberCol, partyCol, detailsCol, brandCol, paymentCol;
    @FXML private TableColumn<TxnListEntry, Number> qtyCol;
    @FXML private TableColumn<TxnListEntry, Number> amountCol, profitCol;

    private SaleTransactionService saleService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        saleService = AppServices.saleTransactions();

        typeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        dateCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate()));
        numberCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                dash(c.getValue().getNumber())));
        partyCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                dash(c.getValue().getParty())));
        detailsCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                dash(c.getValue().getDetails())));
        brandCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().isPayable() ? "—" : dash(c.getValue().getBrand())));
        qtyCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().getQuantity() != null ? c.getValue().getQuantity() : null));
        paymentCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                dash(c.getValue().getPayment())));
        amountCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getAmount()));
        profitCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNetProfit()));
        actionCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));

        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(TxnListEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(e.getTypeLabel());
                chip.getStyleClass().addAll("chip",
                        e.isPayable() ? "chip-payable" : "chip-receivable");
                setGraphic(chip);
                setText(null);
            }
        });

        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });

        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? (empty ? "" : "—") : String.valueOf(v.intValue()));
            }
        });

        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(""); return; }
                setText(UiUtil.money(v.doubleValue()));
                setStyle("-fx-font-weight:700;");
            }
        });

        profitCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                getStyleClass().removeAll("profit-positive", "profit-negative", "profit-zero");
                if (empty) { setText(""); return; }
                if (v == null) { setText("—"); return; }
                double d = v.doubleValue();
                setText(UiUtil.money(d));
                if (d < -0.009) getStyleClass().add("profit-negative");
                else if (d > 0.009) getStyleClass().add("profit-positive");
                else getStyleClass().add("profit-zero");
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View", new FontIcon("fas-eye"));
            private final Button editBtn = new Button("Edit", new FontIcon("fas-pencil-alt"));
            private final Button invoiceBtn = new Button("Invoice", new FontIcon("fas-file-invoice"));
            private final Button delBtn = new Button("Delete", new FontIcon("fas-trash"));
            {
                for (Button b : List.of(viewBtn, editBtn, invoiceBtn)) {
                    b.getStyleClass().addAll("btn", "btn-secondary");
                    b.setGraphicTextGap(5);
                }
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                delBtn.setGraphicTextGap(5);

                viewBtn.setOnAction(e -> {
                    TxnListEntry row = getItem();
                    if (row == null) return;
                    if (row.isPayable()) Dialogs.showViewPayableTransaction(row.getPayable());
                    else Dialogs.showViewSaleTransaction(row.getSale());
                });
                editBtn.setOnAction(e -> {
                    TxnListEntry row = getItem();
                    if (row == null) return;
                    if (row.isPayable()) {
                        Dialogs.showPayableTransaction(row.getPayable(), saved -> loadFromDb());
                    } else {
                        Dialogs.showEditSaleTransaction(row.getSale(), () -> loadFromDb());
                    }
                });
                invoiceBtn.setOnAction(e -> {
                    TxnListEntry row = getItem();
                    if (row != null && row.isReceivable()) generateInvoice(row.getSale());
                });
                delBtn.setOnAction(e -> {
                    TxnListEntry row = getItem();
                    if (row == null || !row.isPayable()) return;
                    PayableTransaction p = row.getPayable();
                    if (!Dialogs.confirm("Delete Payable",
                            "Delete payable " + p.getTxnNumber() + "?",
                            "Paid to: " + p.getPaidTo())) return;
                    AppServices.payables().delete(p.getId());
                    loadFromDb();
                    UiUtil.toast(App.getRoot(), "Payable deleted");
                });
            }
            @Override protected void updateItem(TxnListEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                HBox box = e.isPayable()
                        ? new HBox(6, viewBtn, editBtn, delBtn)
                        : new HBox(6, viewBtn, editBtn, invoiceBtn);
                box.setAlignment(javafx.geometry.Pos.CENTER);
                box.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
                setGraphic(box);
            }
        });

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(TxnListEntry item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("txn-row-receivable", "txn-row-payable");
                if (!empty && item != null) {
                    getStyleClass().add(item.isPayable() ? "txn-row-payable" : "txn-row-receivable");
                }
            }
        });

        loadBillNumber();
        loadFromDb();
    }

    private static String dash(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private void loadBillNumber() {
        if (currentBillField == null) return;
        var inv = AppServices.invoiceNumbers();
        currentBillField.setText(inv.getCurrentInvoiceNumber(LocalDate.now()));
        nextBillHint.setText("Next invoice will be " + inv.peekNextInvoiceNumber(LocalDate.now()));
    }

    private void loadFromDb() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        List<TxnListEntry> rows = new ArrayList<>();
        try {
            var profitService = new com.sevatyres.service.ProfitService();
            for (SaleTransaction s : saleService.getAll()) {
                try {
                    var items = saleService.getItems(s.getId());
                    s.setNetProfit(profitService.calculateNetProfit(s, items));
                } catch (Exception ignored) {}
                rows.add(TxnListEntry.fromSale(s));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            for (PayableTransaction p : AppServices.payables().getAll()) rows.add(TxnListEntry.fromPayable(p));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        rows.sort(Comparator
                .comparing(TxnListEntry::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TxnListEntry::getNumber, Comparator.nullsLast(Comparator.reverseOrder())));

        if (!q.isEmpty()) {
            rows = rows.stream().filter(e -> matches(e, q)).toList();
        }
        table.getItems().setAll(rows);
        loadBillNumber();
    }

    private static boolean matches(TxnListEntry e, String q) {
        return contains(e.getNumber(), q)
                || contains(e.getParty(), q)
                || contains(e.getDetails(), q)
                || contains(e.getBrand(), q)
                || contains(e.getTypeLabel(), q)
                || contains(e.getPayment(), q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
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
        try {
            Dialogs.showNewTransactionChooser(this::loadFromDb);
        } catch (Exception ex) {
            ex.printStackTrace();
            Dialogs.info("New Transaction",
                    "Could not open the dialog:\n"
                            + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    @FXML private void onAddTax() {
        Dialogs.showManageTaxes(() ->
                UiUtil.toast(App.getRoot(), "Taxes updated"));
    }

    @FXML private void onGenerateReport() {
        Dialogs.showGenerateTransactionReport(saleService);
    }

    @FXML private void onProfitLossReport() {
        Dialogs.showGenerateProfitLossReport(saleService);
    }
}
