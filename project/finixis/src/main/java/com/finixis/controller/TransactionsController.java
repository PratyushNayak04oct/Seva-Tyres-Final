package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.GeneratedFile;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.ReportService;
import com.finixis.service.TransactionService;
import com.finixis.viewmodel.FileGenerationService;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeCombo, dateRangeCombo;
    @FXML private DatePicker fromDate, toDate;
    @FXML private VBox groupedBox;
    @FXML private Button exportBtn, reportBtn, invoiceBtn;

    private TransactionService txnService;
    private ReportService      reportService;
    private static final String ALL = "All time", D90 = "Last 90 days", D30 = "Last 30 days";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txnService    = AppServices.transactions();
        reportService = AppServices.reports();
        typeCombo.getItems().addAll("All Types", "Credit", "Debit");
        typeCombo.getSelectionModel().select(0);
        dateRangeCombo.getItems().addAll(D30, D90, ALL);
        dateRangeCombo.getSelectionModel().select(ALL);
        render();
    }

    private void render() {
        groupedBox.getChildren().clear();
        String q = searchField.getText().toLowerCase().trim();
        String typeFilter = typeCombo.getValue();

        LocalDate cutoff;
        LocalDate upperBound = null;
        if (fromDate != null && toDate != null
                && fromDate.getValue() != null && toDate.getValue() != null) {
            cutoff = fromDate.getValue();
            upperBound = toDate.getValue();
        } else {
            cutoff = switch (dateRangeCombo.getValue()) {
                case D30 -> LocalDate.now().minusDays(30);
                case D90 -> LocalDate.now().minusDays(90);
                default  -> LocalDate.of(1900, 1, 1);
            };
        }

        final LocalDate finalUpper = upperBound;
        List<Transaction> txns = txnService.getAll().stream()
                .filter(t -> q.isEmpty()
                        || (t.getCustomerName() != null && t.getCustomerName().toLowerCase().contains(q))
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                .filter(t -> switch (typeFilter) {
                    case "Credit" -> t.getType() == Transaction.Type.CREDIT;
                    case "Debit"  -> t.getType() == Transaction.Type.DEBIT;
                    default -> true;
                })
                .filter(t -> !t.getDate().isBefore(cutoff))
                .filter(t -> finalUpper == null || !t.getDate().isAfter(finalUpper))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());

        if (txns.isEmpty()) {
            Label empty = new Label("No transactions found.");
            empty.getStyleClass().add("text-muted");
            empty.setStyle("-fx-padding: 16 0;");
            groupedBox.getChildren().add(empty);
            return;
        }

        LinkedHashMap<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction t : txns) {
            grouped.computeIfAbsent(UiUtil.dateRangeLabel(t.getDate()), k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            Label header = new Label(entry.getKey().toUpperCase() + "  (" + entry.getValue().size() + ")");
            header.getStyleClass().add("date-group-header");
            groupedBox.getChildren().add(header);
            for (Transaction t : entry.getValue()) {
                groupedBox.getChildren().add(buildRow(t));
            }
        }
    }

    private HBox buildRow(Transaction t) {
        HBox row = new HBox(14);
        row.getStyleClass().add("txn-row");
        if (t.isOngoing()) row.getStyleClass().add("txn-row-ongoing");

        VBox left = new VBox(3);
        Label desc = new Label(t.getDescription() != null && !t.getDescription().isBlank()
                ? t.getDescription() : t.getType().name());
        desc.getStyleClass().add("txn-desc");
        Label meta = new Label(t.getCustomerName() + "  ·  " + UiUtil.date(t.getDate()) + "  ·  " + t.getType());
        meta.getStyleClass().add("txn-meta");
        left.getChildren().addAll(desc, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        Label totalLbl = new Label("Total: " + UiUtil.money(t.getAmount()));
        totalLbl.getStyleClass().add("txn-meta");

        Label paidLbl = new Label("Paid: " + UiUtil.money(t.getPaidAmount()));
        paidLbl.getStyleClass().add("txn-meta");

        Label remainLbl = new Label("Remaining: " + UiUtil.money(t.getBalance()));
        remainLbl.getStyleClass().add("txn-amount");

        Label status = new Label();
        status.getStyleClass().add("chip");

        if (!t.isOngoing()) {
            status.setText("All Cleared");
            status.getStyleClass().add("chip-success");
            remainLbl.getStyleClass().add("text-success");
        } else if (t.getType() == Transaction.Type.DEBIT) {
            status.setText("To Pay");
            status.getStyleClass().add("chip-error");
            remainLbl.getStyleClass().addAll("text-error", "font-bold");
        } else {
            status.setText("To Receive");
            status.getStyleClass().add("chip-primary");
            remainLbl.getStyleClass().add("text-success");
        }

        right.getChildren().addAll(totalLbl, paidLbl, remainLbl, status);

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().addAll("btn", "btn-secondary");
        openBtn.setOnAction(e -> App.getShell().openCustomer(t.getCustomerId()));

        Button invoiceRowBtn = new Button("Invoice", new FontIcon("fas-file-invoice"));
        invoiceRowBtn.getStyleClass().addAll("btn", "btn-secondary");
        invoiceRowBtn.setGraphicTextGap(6);
        invoiceRowBtn.setOnAction(e -> generateAndOpenInvoice(t));

        row.getChildren().addAll(left, spacer, right, openBtn, invoiceRowBtn);
        return row;
    }

    private void generateAndOpenInvoice(Transaction t) {
        try {
            GeneratedFile gf = FileGenerationService.generateInvoiceForTransaction(t);
            reportService.saveFile(gf);
            Dialogs.showFileDownloadedDialog(gf.getFile());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(gf.getFile());
            }
        } catch (Exception ex) {
            Dialogs.info("Invoice Error", "Could not generate invoice:\n" + ex.getMessage());
        }
    }

    @FXML private void onSearch() { render(); }
    @FXML private void onFilter() { render(); }

    @FXML private void onExport() {
        runFileAction(() -> reportService.exportTransactions(txnService), "Export");
    }

    @FXML private void onReport() {
        runFileAction(() -> reportService.generateReport(txnService), "Report");
    }

    @FXML private void onInvoice() {
        UiUtil.toast(App.getRoot(), "Select a customer and transaction to create an invoice.");
        App.getShell().navigate("accounts");
    }

    private void runFileAction(java.util.function.Supplier<List<GeneratedFile>> action, String label) {
        try {
            List<GeneratedFile> files = action.get();
            UiUtil.toast(App.getRoot(),
                    label + " saved! See Reports page to download (" + files.size() + " files).");
        } catch (Exception ex) {
            Dialogs.info(label + " Failed", "Could not write file:\n" + ex.getMessage());
        }
    }
}
