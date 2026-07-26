package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.AlertConfig;
import com.sevatyres.model.Customer;
import com.sevatyres.service.AlertService;
import com.sevatyres.service.AppServices;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AlertsController implements Initializable, PageController {

    @FXML private TableView<AlertConfig>           alertTable;
    @FXML private TableColumn<AlertConfig, String>  nameCol, channelCol;
    @FXML private TableColumn<AlertConfig, Integer> intervalCol;
    @FXML private TableColumn<AlertConfig, Boolean> activeCol;
    @FXML private TableColumn<AlertConfig, AlertConfig> lastRunCol, actionCol;

    @FXML private TableView<AlertService.AlertLogEntry>           logTable;
    @FXML private TableColumn<AlertService.AlertLogEntry, String> logTimeCol, logCustomerCol,
            logChannelCol, logStatusCol, logMsgCol;

    @FXML private Label   emailStatusLabel;
    @FXML private FontIcon emailStatusIcon;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private AlertService alertService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        alertService = AppServices.alerts();

        setupAlertTable();
        setupLogTable();
        updateEmailStatus();
        loadData();
    }

    private void setupAlertTable() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        channelCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getChannel().name()));
        intervalCol.setCellValueFactory(new PropertyValueFactory<>("intervalDays"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label chip = new Label(v ? "Active" : "Paused");
                chip.getStyleClass().addAll("chip", v ? "chip-success" : "chip-warning");
                setGraphic(chip); setText(null);
            }
        });

        lastRunCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        lastRunCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(AlertConfig cfg, boolean empty) {
                super.updateItem(cfg, empty);
                if (empty || cfg == null) { setText(""); return; }
                setText(cfg.getLastRun() != null ? cfg.getLastRun().format(DT_FMT) : "Never");
            }
        });

        actionCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button sendBtn  = new Button("Send Now", new FontIcon("fas-paper-plane"));
            private final Button editBtn  = new Button("Edit",     new FontIcon("fas-pencil-alt"));
            private final Button delBtn   = new Button("Delete",   new FontIcon("fas-trash"));
            {
                sendBtn.getStyleClass().addAll("btn");
                sendBtn.setGraphicTextGap(5);
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setGraphicTextGap(5);
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                delBtn.setGraphicTextGap(5);

                sendBtn.setOnAction(e -> {
                    AlertConfig cfg = getTableView().getItems().get(getIndex());
                    List<Customer> customers = AppServices.customers().getAll()
                            .stream()
                            .filter(c -> cfg.getChannel() == AlertConfig.Channel.SMS
                                    ? (c.getPhone() != null && !c.getPhone().isBlank())
                                    : (c.getEmail() != null && !c.getEmail().isBlank()))
                            .toList();
                    if (customers.isEmpty()) {
                        Dialogs.info("No Recipients",
                                "No customers have a " + cfg.getChannel().name().toLowerCase()
                                        + " address on file.");
                        return;
                    }
                    boolean ok = Dialogs.confirm("Send Alert",
                            "Send \"" + cfg.getName() + "\" to " + customers.size() + " customer(s)?",
                            "Channel: " + cfg.getChannel().name());
                    if (ok) {
                        int sent = alertService.sendNow(cfg, customers);
                        UiUtil.toast(App.getRoot(), sent + " message(s) sent.");
                        loadData();
                    }
                });

                editBtn.setOnAction(e -> {
                    AlertConfig cfg = getTableView().getItems().get(getIndex());
                    showAlertDialog(cfg, () -> loadData());
                });

                delBtn.setOnAction(e -> {
                    AlertConfig cfg = getTableView().getItems().get(getIndex());
                    if (Dialogs.confirm("Delete", "Delete \"" + cfg.getName() + "\"?", "")) {
                        alertService.delete(cfg.getId());
                        loadData();
                    }
                });
            }
            @Override protected void updateItem(AlertConfig cfg, boolean empty) {
                super.updateItem(cfg, empty);
                if (empty || cfg == null) { setGraphic(null); return; }
                HBox box = new HBox(8, sendBtn, editBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    private void setupLogTable() {
        logTimeCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(
                d.getValue().sentAt != null ? d.getValue().sentAt.format(DT_FMT) : ""));
        logCustomerCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().customerName));
        logChannelCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().channel));
        logStatusCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().status));
        logMsgCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().message));

        logStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label chip = new Label(s);
                chip.getStyleClass().addAll("chip",
                        "SENT".equals(s) ? "chip-success" : "chip-error");
                setGraphic(chip); setText(null);
            }
        });
    }

    private void updateEmailStatus() {
        boolean ok = alertService.isEmailConfigured();
        emailStatusLabel.setText(ok
                ? "Email configured and ready to send."
                : "Email not configured — update application.properties to enable sending.");
        emailStatusIcon.setStyle("-fx-icon-color: " + (ok ? "#38a169" : "#e53e3e") + ";");
    }

    private void loadData() {
        alertTable.getItems().setAll(alertService.getAll());
        logTable.getItems().setAll(alertService.getLog(50));
    }

    @FXML private void onNewAlert() {
        showAlertDialog(null, () -> loadData());
    }

    @FXML private void onRefreshLog() { loadData(); }

    // ─── Alert config editor dialog ───────────────────────────────────────────

    private static void showAlertDialog(AlertConfig existing, Runnable onSaved) {
        boolean isNew = existing == null;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(isNew ? "New Alert Campaign" : "Edit Alert — " + existing.getName());
        stage.setMinWidth(480);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 16, 28));

        Label title = new Label(isNew ? "New Alert Campaign" : "Edit Campaign");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");

        TextField nameField = new TextField(isNew ? "" : existing.getName());
        nameField.setPromptText("Campaign name (e.g. Monthly Reminder)");
        nameField.getStyleClass().add("field");
        nameField.setMaxWidth(Double.MAX_VALUE);

        TextArea msgArea = new TextArea(isNew
                ? "Dear {name},\n\nThank you for choosing Seva Tyres. We wanted to remind you about our services.\n\nBest regards,\nSeva Tyres"
                : existing.getMessageTemplate());
        msgArea.setWrapText(true);
        msgArea.setPrefRowCount(6);
        msgArea.setPromptText("Message template — use {name}, {phone}, {email} as placeholders");

        Label placeholderHint = new Label("Available placeholders: {name}  {phone}  {email}");
        placeholderHint.setStyle("-fx-font-size:11px; -fx-text-fill: -neutral-400;");

        ComboBox<AlertConfig.Channel> channelCombo = new ComboBox<>();
        channelCombo.getItems().addAll(AlertConfig.Channel.values());
        channelCombo.setValue(isNew ? AlertConfig.Channel.EMAIL
                : (existing.getChannel() != null ? existing.getChannel() : AlertConfig.Channel.EMAIL));
        channelCombo.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> intervalSpinner = new Spinner<>(1, 365, isNew ? 7 : existing.getIntervalDays());
        intervalSpinner.setEditable(true);
        intervalSpinner.setMaxWidth(Double.MAX_VALUE);

        CheckBox activeCheck = new CheckBox("Active (will be included in scheduled sends)");
        activeCheck.setSelected(isNew || existing.isActive());

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        VBox labeledName    = labeled("Campaign Name *", nameField);
        VBox labeledMsg     = new VBox(5, new Label("Message Template *")
                {{ setStyle("-fx-font-size:12px; -fx-text-fill: -text-muted;"); }},
                msgArea, placeholderHint);
        VBox labeledChannel = labeled("Channel", channelCombo);
        VBox labeledInterval = labeled("Send every N days", intervalSpinner);

        content.getChildren().addAll(title, new Separator(), labeledName, labeledMsg,
                labeledChannel, labeledInterval, activeCheck, err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button(isNew ? "Create Campaign" : "Save Changes");
        saveBtn.getStyleClass().add("btn");

        HBox btnRow = new HBox(12, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 28, 16, 28));

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            String msg = msgArea.getText().trim();
            if (msg.isEmpty()) { err.setText("Message template is required."); return; }

            AlertConfig cfg = isNew ? new AlertConfig() : existing;
            cfg.setName(name);
            cfg.setMessageTemplate(msg);
            cfg.setChannel(channelCombo.getValue());
            cfg.setIntervalDays(intervalSpinner.getValue());
            cfg.setActive(activeCheck.isSelected());
            AppServices.alerts().save(cfg);
            stage.close();
            if (onSaved != null) onSaved.run();
        });

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(520);

        VBox outer = new VBox(scroll, btnRow);
        outer.getStyleClass().add("dialog-root");
        Scene scene = new Scene(outer);
        com.sevatyres.viewmodel.ThemeManager.register(scene);
        com.sevatyres.viewmodel.ThemeManager.apply(scene);
        stage.setOnHidden(ev -> com.sevatyres.viewmodel.ThemeManager.unregister(scene));
        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private static VBox labeled(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill: -text-muted;");
        return new VBox(5, lbl, field);
    }
}
