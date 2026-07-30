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
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Reports page — summary stats, HTML invoice template upload, generated files.
 */
public class ReportsController implements Initializable, PageController {

    @FXML private Label totalCredits, totalDebits, netLabel;
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

        refreshTemplateStatus();
        renderFiles();
    }

    private void refreshTemplateStatus() {
        if (templateStatusLabel == null) return;
        if (AppServices.company().hasCustomInvoiceHtmlTemplate()) {
            templateStatusLabel.setText("Custom HTML template is active (stored in database). Upload a new file to replace it.");
        } else {
            templateStatusLabel.setText("Using built-in professional HTML template. Download it, edit, then upload.");
        }
    }

    @FXML private void onExportTemplate() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Default Invoice Template");
            chooser.setInitialFileName("invoice-template.html");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("HTML Template", "*.html", "*.htm"));
            File dest = chooser.showSaveDialog(App.getScene().getWindow());
            if (dest == null) return;
            String html = CompanyService.loadBuiltinInvoiceHtmlTemplate();
            Files.writeString(dest.toPath(), html, StandardCharsets.UTF_8);
            templateStatusLabel.setText("Saved editable template to: " + dest.getAbsolutePath());
            UiUtil.toast(App.getRoot(), "Template downloaded — edit then upload");
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dest.getParentFile());
        } catch (Exception ex) {
            Dialogs.info("Export Failed", ex.getMessage());
        }
    }

    @FXML private void onUploadTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Invoice Template (HTML)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("HTML Template", "*.html", "*.htm"));
        File chosen = chooser.showOpenDialog(App.getScene().getWindow());
        if (chosen == null) return;
        try {
            String html = Files.readString(chosen.toPath(), StandardCharsets.UTF_8);
            if (html.isBlank() || !html.toLowerCase().contains("<html")) {
                Dialogs.info("Invalid Template",
                        "Please upload a valid HTML invoice template file.");
                return;
            }
            if (!html.contains("{bill_no}") && !html.contains("{items_html}")) {
                if (!Dialogs.confirm("Missing Placeholders",
                        "This file may be missing required tags like {bill_no} or {items_html}.",
                        "Upload anyway?")) return;
            }
            AppServices.company().saveInvoiceHtmlTemplate(html);
            refreshTemplateStatus();
            UiUtil.toast(App.getRoot(), "Invoice template uploaded");
        } catch (Exception ex) {
            Dialogs.info("Upload Failed", ex.getMessage());
        }
    }

    @FXML private void onResetTemplate() {
        if (!Dialogs.confirm("Reset Template", "Switch back to the built-in professional template?",
                "Your uploaded custom template will be removed from the app.")) return;
        AppServices.company().clearInvoiceHtmlTemplate();
        refreshTemplateStatus();
        UiUtil.toast(App.getRoot(), "Built-in invoice template restored");
    }

    private void renderFiles() {
        filesBox.getChildren().clear();
        List<GeneratedFile> files = reportService.getAll();

        if (files.isEmpty()) {
            Label empty = new Label("No files generated yet. Use Transactions → Invoice / Generate Report.");
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

        boolean isPdf = "PDF".equalsIgnoreCase(gf.getFormat());
        FontIcon icon = new FontIcon(isPdf ? "fas-file-pdf" : "fas-file-code");
        icon.getStyleClass().add(isPdf ? "file-icon-pdf" : "file-icon-excel");

        VBox info = new VBox(2);
        Label name = new Label(gf.getName());
        name.getStyleClass().add("font-bold");
        Label meta = new Label(gf.getFileType() + "  ·  " + gf.getFormat()
                + "  ·  " + gf.getTimestampDisplay());
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

        Button dlBtn = new Button("Open");
        dlBtn.getStyleClass().addAll("btn", "btn-secondary");
        dlBtn.setGraphic(new FontIcon("fas-external-link-alt"));
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
