package com.finixis.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;

import com.finixis.App;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.CustomerService;
import com.finixis.service.InventoryService;
import com.finixis.service.TransactionService;
import com.finixis.viewmodel.UiUtil;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class HomeController implements Initializable, PageController {

    @FXML private Label todayLabel, pendingCredits, pendingCreditsCount,
            pendingDebits, pendingDebitsCount, lowStock, totalCustomers;
    @FXML private VBox recentBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CustomerService    customers    = AppServices.customers();
        InventoryService   inventory    = AppServices.inventory();
        TransactionService transactions = AppServices.transactions();

        todayLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        double creditTotal = transactions.totalCreditOutstanding();
        long   creditCount = transactions.getAllCredits().stream().filter(Transaction::isOngoing).count();
        pendingCredits.setText(UiUtil.money(creditTotal));
        pendingCredits.getStyleClass().remove("stat-negative");
        pendingCredits.getStyleClass().add("text-success");
        pendingCreditsCount.setText(creditCount + " open");

        double debitTotal = transactions.totalDebits();
        long   debitCount = transactions.getAllDebits().size();
        pendingDebits.setText(UiUtil.money(debitTotal));
        pendingDebitsCount.setText(debitCount + " entries");

        lowStock.setText(String.valueOf(inventory.getLowStockCount()));
        totalCustomers.setText(String.valueOf(customers.getAll().size()));

        List<Transaction> recent = transactions.getAll().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(6).toList();

        if (recent.isEmpty()) {
            Label empty = new Label("No recent activity.");
            empty.getStyleClass().add("text-muted");
            recentBox.getChildren().add(empty);
        } else {
            for (Transaction t : recent) {
                recentBox.getChildren().add(buildActivityRow(t));
            }
        }
    }

    private HBox buildActivityRow(Transaction t) {
        HBox row = new HBox(12);
        row.getStyleClass().add("txn-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Colored circle avatar
        boolean isCredit = t.getType() == Transaction.Type.CREDIT
                || t.getType() == Transaction.Type.PAYMENT;
        Label avatar = new Label(isCredit ? "↑" : "↓");
        avatar.getStyleClass().addAll("chip", isCredit ? "chip-success" : "chip-error");
        avatar.setMinWidth(36);
        avatar.setAlignment(Pos.CENTER);

        // Left VBox: name + type
        VBox left = new VBox(2);
        Label nameLabel = new Label(t.getCustomerName() != null ? t.getCustomerName() : "—");
        nameLabel.getStyleClass().add("txn-desc");
        Label typeMeta = new Label(t.getType().name());
        typeMeta.getStyleClass().add("txn-meta");
        Label amountLabel = new Label(UiUtil.money(t.getAmount()));
        amountLabel.getStyleClass().addAll("txn-meta", isCredit ? "text-success" : "text-error");
        HBox typeRow = new HBox(6, typeMeta, amountLabel);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        left.getChildren().addAll(nameLabel, typeRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Date on the right
        Label dateLabel = new Label(UiUtil.date(t.getDate()));
        dateLabel.getStyleClass().add("txn-meta");

        // Open button
        Button openBtn = new Button("Open", new FontIcon("fas-external-link-alt"));
        openBtn.getStyleClass().addAll("btn", "btn-secondary");
        openBtn.setGraphicTextGap(6);
        openBtn.setOnAction(e -> App.getShell().openCustomer(t.getCustomerId()));

        row.getChildren().addAll(avatar, left, spacer, dateLabel, openBtn);
        return row;
    }

    @FXML private void goAccounts()     { App.getShell().navigate("accounts"); }
    @FXML private void goCredit()       { App.getShell().navigate("credit"); }
    @FXML private void goInventory()    { App.getShell().navigate("inventory"); }
    @FXML private void goTransactions() { App.getShell().navigate("transactions"); }
    @FXML private void goReports()      { App.getShell().navigate("reports"); }
    @FXML private void onCreateInvoice() {
        UiUtil.toast(App.getRoot(), "Go to Transaction History to generate an invoice.");
        App.getShell().navigate("transactions");
    }
}
