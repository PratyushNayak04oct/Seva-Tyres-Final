package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.GeneratedFile;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.ReportService;
import com.finixis.service.TransactionService;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReportsController implements Initializable, PageController {

    @FXML private ComboBox<String> rangeCombo;
    @FXML private ComboBox<String> invoiceFormatCombo;
    @FXML private TextField companyNameField;
    @FXML private Label totalCredits, totalDebits, netLabel;
    @FXML private BarChart<String, Number> chart;
    @FXML private VBox filesBox;

    private ReportService      reportService;
    private TransactionService txnService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reportService = AppServices.reports();
        txnService    = AppServices.transactions();

        rangeCombo.getItems().addAll("Last 30 days", "Last 90 days", "This Year");
        rangeCombo.getSelectionModel().select("Last 90 days");

        if (invoiceFormatCombo != null) {
            invoiceFormatCombo.getItems().addAll("PDF (A4)", "PDF (Letter)");
            invoiceFormatCombo.getSelectionModel().select(0);
        }

        // Summary stats from DB
        double credits  = txnService.getAllCredits().stream().mapToDouble(Transaction::getAmount).sum();
        double debits   = txnService.getAllDebits().stream().mapToDouble(Transaction::getAmount).sum();
        double payments = txnService.getAllCredits().stream().mapToDouble(Transaction::getPaidAmount).sum();

        totalCredits.setText(UiUtil.money(credits));
        totalDebits.setText(UiUtil.money(payments + debits));
        double net = credits - payments - debits;
        netLabel.setText(UiUtil.signedMoney(net));
        netLabel.getStyleClass().add(net >= 0 ? "stat-positive" : "stat-negative");

        // Mock monthly bar chart
        XYChart.Series<String, Number> creditSeries = new XYChart.Series<>();
        creditSeries.setName("Credits");
        creditSeries.getData().addAll(
                new XYChart.Data<>("Jan", 12400), new XYChart.Data<>("Feb", 9800),
                new XYChart.Data<>("Mar", 15600), new XYChart.Data<>("Apr", 11200),
                new XYChart.Data<>("May", 8900),  new XYChart.Data<>("Jun", 14200),
                new XYChart.Data<>("Jul", 18900));
        XYChart.Series<String, Number> paymentSeries = new XYChart.Series<>();
        paymentSeries.setName("Payments");
        paymentSeries.getData().addAll(
                new XYChart.Data<>("Jan", 8000), new XYChart.Data<>("Feb", 11200),
                new XYChart.Data<>("Mar", 6400), new XYChart.Data<>("Apr", 9800),
                new XYChart.Data<>("May", 7200), new XYChart.Data<>("Jun", 11500),
                new XYChart.Data<>("Jul", 9300));
        chart.getData().addAll(creditSeries, paymentSeries);
        chart.setTitle("Credits vs Payments – Mock Monthly");

        renderFiles();
    }

    private void renderFiles() {
        filesBox.getChildren().clear();
        List<GeneratedFile> files = reportService.getAll();

        if (files.isEmpty()) {
            Label empty = new Label("No files generated yet. Use Transaction History → Generate Report / Create Invoice / Export.");
            empty.getStyleClass().addAll("text-muted");
            empty.setStyle("-fx-padding: 12 0;");
            filesBox.getChildren().add(empty);
            return;
        }

        // Show newest first
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

        // File type icon
        boolean isPdf = gf.getFormat().equals("PDF");
        FontIcon icon = new FontIcon(isPdf ? "fas-file-pdf" : "fas-file-excel");
        icon.getStyleClass().add(isPdf ? "file-icon-pdf" : "file-icon-excel");

        // Name + metadata
        VBox info = new VBox(2);
        Label name = new Label(gf.getName());
        name.getStyleClass().add("font-bold");
        Label meta = new Label(gf.getFileType() + "  ·  " + gf.getFormat()
                + "  ·  " + gf.getTimestampDisplay());
        meta.getStyleClass().addAll("text-muted", "text-sm");
        info.getChildren().addAll(name, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Status chip
        Label statusChip;
        if (gf.isAvailable()) {
            statusChip = new Label("Ready");
            statusChip.getStyleClass().addAll("chip", "chip-success");
        } else {
            statusChip = new Label("File missing");
            statusChip.getStyleClass().addAll("chip", "chip-error");
        }

        // Download button
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
        Dialogs.showFileDownloadedDialog(file);
    }
}
