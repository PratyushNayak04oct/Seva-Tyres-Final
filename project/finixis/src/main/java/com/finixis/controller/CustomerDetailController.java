package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Customer;
import com.finixis.model.GeneratedFile;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.CustomerService;
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

public class CustomerDetailController implements Initializable, PageController {

    @FXML private Label avatar, nameLabel, emailLabel, phoneLabel, sinceLabel;
    @FXML private Label balanceLabel, balanceNote;
    @FXML private Label chipTxns, chipLast, chipOngoing;
    @FXML private Button addDebitBtn, recordPaymentBtn, deleteBtn;
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private DatePicker fromDate, toDate;
    @FXML private VBox historyBox;

    private CustomerService    customerService;
    private TransactionService txnService;
    private Customer customer;

    private static final String ALL = "All time";
    private static final String D90 = "Last 90 days";
    private static final String D30 = "Last 30 days";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        customerService = AppServices.customers();
        txnService      = AppServices.transactions();
        dateRangeCombo.getItems().addAll(D30, D90, ALL);
        dateRangeCombo.getSelectionModel().select(ALL);
        if (fromDate != null) fromDate.setValue(null);
        if (toDate != null) toDate.setValue(null);
    }

    public void loadCustomer(int customerId) {
        customer = customerService.getById(customerId).orElse(null);
        if (customer == null) return;

        avatar.setText(customer.getInitials());
        nameLabel.setText(customer.getName());
        emailLabel.setText(customer.getEmail() != null ? customer.getEmail() : "—");
        phoneLabel.setText(customer.getPhone() != null ? customer.getPhone() : "—");
        sinceLabel.setText("Customer since " + UiUtil.date(customer.getCustomerSince()));

        customer.setBalance(customerService.getBalance(customerId));
        refreshBalance();

        List<Transaction> txns = txnService.getByCustomer(customerId);
        chipTxns.setText(txns.size() + " transactions");
        long ongoing = txns.stream().filter(Transaction::isOngoing).count();
        chipOngoing.setText(ongoing + " pending");
        Optional<LocalDate> last = txns.stream().map(Transaction::getDate)
                .max(Comparator.naturalOrder());
        chipLast.setText("Last: " + last.map(UiUtil::date).orElse("—"));

        renderHistory();
    }

    private void refreshBalance() {
        if (customer == null) return;
        double bal = customerService.getBalance(customer.getId());
        customer.setBalance(bal);
        balanceLabel.setText(UiUtil.signedMoney(bal));
        balanceLabel.getStyleClass().removeAll("text-error", "text-success", "text-muted");
        if (bal > 0) {
            balanceLabel.getStyleClass().add("text-success");
            balanceNote.setText("To Receive");
        } else if (bal < 0) {
            balanceLabel.getStyleClass().add("text-error");
            balanceNote.setText("We Owe Customer");
        } else {
            balanceLabel.getStyleClass().add("text-success");
            balanceNote.setText("All Settled");
        }
    }

    private void renderHistory() {
        historyBox.getChildren().clear();
        if (customer == null) return;

        LocalDate cutoff;
        LocalDate upperBound = null;
        if (fromDate != null && toDate != null
                && fromDate.getValue() != null && toDate.getValue() != null) {
            cutoff = fromDate.getValue();
            upperBound = toDate.getValue();
        } else {
            String range = dateRangeCombo.getValue();
            cutoff = switch (range) {
                case D30 -> LocalDate.now().minusDays(30);
                case D90 -> LocalDate.now().minusDays(90);
                default  -> LocalDate.of(1900, 1, 1);
            };
        }

        final LocalDate finalUpper = upperBound;
        List<Transaction> txns = txnService.getByCustomer(customer.getId()).stream()
                .filter(t -> !t.getDate().isBefore(cutoff))
                .filter(t -> finalUpper == null || !t.getDate().isAfter(finalUpper))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .toList();

        if (txns.isEmpty()) {
            Label empty = new Label("No transactions in this range.");
            empty.getStyleClass().add("text-muted");
            empty.setStyle("-fx-padding: 16 0;");
            historyBox.getChildren().add(empty);
            return;
        }

        LinkedHashMap<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction t : txns) {
            grouped.computeIfAbsent(UiUtil.dateRangeLabel(t.getDate()), k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            Label header = new Label(entry.getKey().toUpperCase());
            header.getStyleClass().add("date-group-header");
            historyBox.getChildren().add(header);
            for (Transaction t : entry.getValue()) {
                historyBox.getChildren().add(buildTxnRow(t));
            }
        }
    }

    private HBox buildTxnRow(Transaction t) {
        HBox row = new HBox(14);
        row.getStyleClass().add("txn-row");
        if (t.isOngoing()) row.getStyleClass().add("txn-row-ongoing");

        VBox left = new VBox(3);
        Label desc = new Label(t.getDescription() != null && !t.getDescription().isBlank()
                ? t.getDescription() : t.getType().name());
        desc.getStyleClass().add("txn-desc");
        Label meta = new Label(UiUtil.date(t.getDate()) + "  ·  " + t.getType());
        meta.getStyleClass().add("txn-meta");
        left.getChildren().addAll(desc, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        Label balLabel = new Label(UiUtil.money(t.getBalance()));
        balLabel.getStyleClass().add("txn-amount");
        Label totalLabel = new Label("Total: " + UiUtil.money(t.getAmount()));
        totalLabel.getStyleClass().add("txn-meta");

        Label status = new Label();
        status.getStyleClass().add("chip");

        if (!t.isOngoing()) {
            status.setText("All Cleared");
            status.getStyleClass().add("chip-success");
            balLabel.getStyleClass().add("text-success");
        } else if (t.getType() == Transaction.Type.DEBIT) {
            status.setText("To Pay");
            status.getStyleClass().add("chip-error");
            balLabel.getStyleClass().add("text-error");
        } else {
            status.setText("To Receive");
            status.getStyleClass().add("chip-primary");
            balLabel.getStyleClass().add("text-normal");
        }

        right.getChildren().addAll(balLabel, totalLabel, status);

        Button editBtn = new Button("Edit", new FontIcon("fas-pencil-alt"));
        editBtn.getStyleClass().addAll("btn", "btn-secondary");
        editBtn.setGraphicTextGap(6);
        editBtn.setOnAction(e -> Dialogs.showEditTransaction(t, () -> {
            refreshBalance();
            renderHistory();
        }));

        Button openBtn = new Button("Open", new FontIcon("fas-external-link-alt"));
        openBtn.getStyleClass().addAll("btn", "btn-secondary");
        openBtn.setGraphicTextGap(6);
        openBtn.setOnAction(e -> Dialogs.showViewTransaction(t));

        Button invoiceBtn = new Button("Invoice", new FontIcon("fas-file-invoice"));
        invoiceBtn.getStyleClass().addAll("btn", "btn-secondary");
        invoiceBtn.setGraphicTextGap(6);
        invoiceBtn.setOnAction(e -> showInvoiceDialog(t));

        HBox actions = new HBox(6, editBtn, openBtn, invoiceBtn);
        actions.setAlignment(Pos.CENTER);

        row.getChildren().addAll(left, spacer, right, actions);
        return row;
    }

    private void showInvoiceDialog(Transaction t) {
        try {
            GeneratedFile gf = FileGenerationService.generateInvoiceForTransaction(t, customer);
            AppServices.reports().saveFile(gf);
            Dialogs.showFileDownloadedDialog(gf.getFile());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(gf.getFile());
            }
        } catch (Exception ex) {
            Dialogs.info("Invoice Error", "Could not generate invoice:\n" + ex.getMessage());
        }
    }

    @FXML private void onDateRange() { renderHistory(); }

    @FXML private void onBack() { App.getShell().navigate("accounts"); }

    @FXML private void onAddDebit() {
        if (customer == null) return;
        Dialogs.showAddDebit(customer, () -> {
            refreshBalance();
            renderHistory();
            UiUtil.toast(App.getRoot(), "Debit added for " + customer.getName());
        });
    }

    @FXML private void onRecordPayment() {
        if (customer == null) return;
        Dialogs.showRecordPayment(customer, () -> {
            refreshBalance();
            renderHistory();
            UiUtil.toast(App.getRoot(), "Payment recorded for " + customer.getName());
        });
    }

    @FXML private void onDelete() {
        if (customer == null) return;
        boolean ok = Dialogs.confirm(
                "Delete Customer",
                "Delete " + customer.getName() + "?",
                "This will permanently remove the customer and all their history.\nThis action cannot be undone.");
        if (ok) {
            try {
                customerService.deleteCustomer(customer.getId());
                UiUtil.toast(App.getRoot(), customer.getName() + " deleted");
            } catch (Exception ex) {
                Dialogs.info("Cannot Delete",
                        "Could not delete customer — they may have existing transactions.\n" + ex.getMessage());
                return;
            }
            App.getShell().navigate("accounts");
        }
    }
}
