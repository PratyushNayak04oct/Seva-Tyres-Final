package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.GeneratedFile;
import com.sevatyres.model.Transaction;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.CompanyService;
import com.sevatyres.service.ReportService;
import com.sevatyres.service.TransactionService;
import com.sevatyres.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Reports page — summary stats, editable invoice template, generated files.
 */
public class ReportsController implements Initializable, PageController {

    @FXML private Label totalCredits, totalDebits, netLabel;
    @FXML private TextArea invoiceTemplateArea;
    @FXML private Label templateStatusLabel;
    @FXML private VBox filesBox;

    private ReportService      reportService;
    private TransactionService txnService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reportService = AppServices.reports();
        txnService    = AppServices.transactions();

        double credits  = txnService.getAllCredits().stream().mapToDouble(Transaction::getAmount).sum();
        double debits   = txnService.getAllDebits().stream().mapToDouble(Transaction::getAmount).sum();
        double payments = txnService.getAllCredits().stream().mapToDouble(Transaction::getPaidAmount).sum();

        totalCredits.setText(UiUtil.money(credits));
        totalDebits.setText(UiUtil.money(payments + debits));
        double net = credits - payments - debits;
        netLabel.setText(UiUtil.signedMoney(net));
        netLabel.getStyleClass().add(net >= 0 ? "stat-positive" : "stat-negative");

        if (invoiceTemplateArea != null) {
            invoiceTemplateArea.setText(AppServices.company().getInvoiceTemplate());
        }
        renderFiles();
    }

    @FXML private void onSaveTemplate() {
        String text = invoiceTemplateArea.getText();
        if (text == null || text.isBlank()) {
            Dialogs.info("Template Required", "Invoice template cannot be empty.");
            return;
        }
        AppServices.company().saveInvoiceTemplate(text);
        if (templateStatusLabel != null) {
            templateStatusLabel.setText("Template saved — new invoices will use this layout.");
        }
        UiUtil.toast(App.getRoot(), "Invoice template saved");
    }

    @FXML private void onResetTemplate() {
        if (!Dialogs.confirm("Reset Template", "Restore the default invoice template?",
                "Your current edits will be replaced.")) return;
        String def = CompanyService.defaultInvoiceTemplate();
        invoiceTemplateArea.setText(def);
        AppServices.company().saveInvoiceTemplate(def);
        if (templateStatusLabel != null) {
            templateStatusLabel.setText("Default template restored.");
        }
        UiUtil.toast(App.getRoot(), "Default invoice template restored");
    }

    private void renderFiles() {
        filesBox.getChildren().clear();
        List<GeneratedFile> files = reportService.getAll();

        if (files.isEmpty()) {
            Label empty = new Label("No files generated yet. Use Transactions \u2192 Generate Report / Invoice.");
            empty.getStyleClass().addAll("text-muted");
            empty.setStyle("-fx-padding: 12 0;");
            filesBox.getChildren().add(empty);
            return;
        }

        List<GeneratedFile> sorted = files.stream()
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .toList();

        for (GeneratedFile gf : sorted) {
            filesBox.getChildren().add(buildFileRow(gf));
        }
    }

    private HBox buildFileRow(GeneratedFile gf) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("file-row");

        boolean isPdf = gf.getFormat().equals("PDF");
        FontIcon icon = new FontIcon(isPdf ? "fas-file-pdf" : "fas-file-excel");
        icon.getStyleClass().add(isPdf ? "file-icon-pdf" : "file-icon-excel");

        VBox info = new VBox(2);
        Label name = new Label(gf.getName());
        name.getStyleClass().add("font-bold");
        Label meta = new Label(gf.getFileType() + "  \u00b7  " + gf.getFormat()
                + "  \u00b7  " + gf.getTimestampDisplay());
        meta.getStyleClass().addAll("text-muted", "text-sm");
        info.getChildren().addAll(name, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label statusChip;
        if (gf.isAvailable()) {
            statusChip = new Label("Ready");
            statusChip.getStyleClass().addAll("chip", "chip-success");
        } else {
            statusChip = new Label("File missing");
            statusChip.getStyleClass().addAll("chip", "chip-error");
        }

        Button dlBtn = new Button("Download");
        dlBtn.getStyleClass().addAll("btn", "btn-secondary");
        dlBtn.setGraphic(new FontIcon("fas-download"));
        dlBtn.setDisable(!gf.isAvailable());
        dlBtn.setOnAction(e -> openFile(gf.getFile()));

        row.getChildren().addAll(icon, info, statusChip, dlBtn);
        return row;
    }

    private void openFile(File file) {
        if (file == null || !file.exists()) {
            Dialogs.info("File Not Found",
                    "The file could not be located. It may have been moved or deleted.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                Dialogs.info("Open File", "File is at:\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            Dialogs.info("Could Not Open File", "Error: " + ex.getMessage()
                    + "\n\nFile is at:\n" + file.getAbsolutePath());
        }
    }
}
