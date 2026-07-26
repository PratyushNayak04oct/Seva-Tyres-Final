package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.AlertConfig;
import com.sevatyres.model.Customer;
import com.sevatyres.service.AlertService;
import com.sevatyres.service.AppServices;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AlertsController implements Initializable, PageController {

    @FXML private TableView<AlertConfig>                alertTable;
    @FXML private TableColumn<AlertConfig, String>      nameCol, channelCol;
    @FXML private TableColumn<AlertConfig, Integer>     intervalCol, durationCol;
    @FXML private TableColumn<AlertConfig, Boolean>     activeCol;
    @FXML private TableColumn<AlertConfig, AlertConfig> lastRunCol, actionCol;

    @FXML private TableView<AlertService.AlertLogEntry>           logTable;
    @FXML private TableColumn<AlertService.AlertLogEntry, String> logTimeCol, logCustomerCol,
            logChannelCol, logStatusCol, logMsgCol;

    @FXML private Label    emailStatusLabel;
    @FXML private FontIcon emailStatusIcon;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private AlertService alertService;

    /** A single send-target (from customer list or manually entered). */
    private record RecipientEntry(String name, String phone, String email) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        alertService = AppServices.alerts();
        setupAlertTable();
        setupLogTable();
        updateEmailStatus();
        loadData();
    }

    // ─── Table setup ──────────────────────────────────────────────────────────

    private void setupAlertTable() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        channelCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getChannel().name()));
        intervalCol.setCellValueFactory(new PropertyValueFactory<>("intervalDays"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("durationDays"));

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
            private final Button sendBtn = new Button("Send Now", new FontIcon("fas-paper-plane"));
            private final Button editBtn = new Button("Edit",     new FontIcon("fas-pencil-alt"));
            private final Button delBtn  = new Button("Delete",   new FontIcon("fas-trash"));
            {
                sendBtn.getStyleClass().add("btn");
                sendBtn.setGraphicTextGap(5);
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setGraphicTextGap(5);
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                delBtn.setGraphicTextGap(5);

                sendBtn.setOnAction(e -> {
                    AlertConfig cfg = getTableView().getItems().get(getIndex());
                    List<Customer> allCustomers = AppServices.customers().getAll();
                    showSendDialog(cfg, allCustomers);
                    loadData();
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
                HBox box = new HBox(6, sendBtn, editBtn, delBtn);
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
                chip.getStyleClass().addAll("chip", "SENT".equals(s) ? "chip-success" : "chip-error");
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

    @FXML private void onNewAlert()   { showAlertDialog(null, () -> loadData()); }
    @FXML private void onRefreshLog() { loadData(); }

    // ─── Send dialog ──────────────────────────────────────────────────────────

    /**
     * Opens a dialog letting the user pick existing customers (auto-populating
     * phone / email) or enter custom recipients, then send the campaign.
     */
    private static void showSendDialog(AlertConfig cfg, List<Customer> allCustomers) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle("Send — " + cfg.getName());
        stage.setMinWidth(600);

        ObservableList<RecipientEntry> recipients = FXCollections.observableArrayList();
        boolean isSms = cfg.getChannel() == AlertConfig.Channel.SMS;

        // ── Dialog header ──
        Label hdr = new Label("Send — " + cfg.getName());
        hdr.setStyle("-fx-font-size:17px; -fx-font-weight:700;");
        Label subInfo = new Label(
                "Channel: " + cfg.getChannel().name()
                + "   ·   Every " + cfg.getIntervalDays() + " day(s)"
                + "   ·   Duration: " + cfg.getDurationDays() + " day(s)");
        subInfo.getStyleClass().addAll("text-sm", "text-muted");
        VBox headerBox = new VBox(4, hdr, subInfo);
        headerBox.setPadding(new Insets(20, 24, 12, 24));

        // ── Section 1: pick from existing customers ──
        Label sec1Lbl = new Label("Add from Existing Customers");
        sec1Lbl.getStyleClass().add("section-title");

        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.setMaxWidth(Double.MAX_VALUE);
        customerCombo.setPromptText("Select a customer…");
        customerCombo.getStyleClass().add("combo");
        customerCombo.getItems().addAll(allCustomers);
        customerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) {
                if (c == null) return "";
                String contact = isSms
                        ? nvl(c.getPhone())
                        : nvl(c.getEmail());
                return c.getName() + (contact.isBlank() ? "" : " — " + contact);
            }
            @Override public Customer fromString(String s) { return null; }
        });

        TextField cName  = roField("Name");
        TextField cPhone = roField("Phone");
        TextField cEmail = roField("Email");

        customerCombo.setOnAction(ev -> {
            Customer c = customerCombo.getValue();
            if (c != null) {
                cName.setText(nvl(c.getName()));
                cPhone.setText(nvl(c.getPhone()));
                cEmail.setText(nvl(c.getEmail()));
            } else { cName.clear(); cPhone.clear(); cEmail.clear(); }
        });

        Button addFromCustBtn = new Button("Add to Recipients");
        addFromCustBtn.getStyleClass().add("btn");
        addFromCustBtn.setOnAction(ev -> {
            Customer c = customerCombo.getValue();
            if (c == null) return;
            boolean dup = recipients.stream().anyMatch(r -> r.name().equals(c.getName()));
            if (!dup) recipients.add(new RecipientEntry(nvl(c.getName()), nvl(c.getPhone()), nvl(c.getEmail())));
            customerCombo.setValue(null);
            cName.clear(); cPhone.clear(); cEmail.clear();
        });

        GridPane cGrid = makeGrid();
        addGridRow(cGrid, 0, "Customer", customerCombo);
        addGridRow(cGrid, 1, "Name",     cName);
        addGridRow(cGrid, 2, "Phone",    cPhone);
        addGridRow(cGrid, 3, "Email",    cEmail);
        HBox addFromRow = new HBox(addFromCustBtn);
        addFromRow.setAlignment(Pos.CENTER_RIGHT);

        VBox sec1 = new VBox(10, sec1Lbl, cGrid, addFromRow);
        sec1.getStyleClass().add("section-card");

        // ── Section 2: custom recipient ──
        Label sec2Lbl = new Label("Add Custom Recipient");
        sec2Lbl.getStyleClass().add("section-title");

        TextField custName  = inputField("Full name *");
        TextField custPhone = inputField("Phone number");
        TextField custEmail = inputField("Email address");

        Label custErr = new Label();
        custErr.getStyleClass().addAll("text-sm", "text-error");

        Button addCustomBtn = new Button("Add Custom Recipient");
        addCustomBtn.getStyleClass().addAll("btn", "btn-secondary");
        addCustomBtn.setOnAction(ev -> {
            String n = custName.getText().trim();
            if (n.isEmpty()) { custErr.setText("Name is required."); return; }
            custErr.setText("");
            recipients.add(new RecipientEntry(n, custPhone.getText().trim(), custEmail.getText().trim()));
            custName.clear(); custPhone.clear(); custEmail.clear();
        });

        GridPane custGrid = makeGrid();
        addGridRow(custGrid, 0, "Name",  custName);
        addGridRow(custGrid, 1, "Phone", custPhone);
        addGridRow(custGrid, 2, "Email", custEmail);

        HBox customRow = new HBox(10, custErr,
                spacer(), addCustomBtn);
        customRow.setAlignment(Pos.CENTER_LEFT);

        VBox sec2 = new VBox(10, sec2Lbl, custGrid, customRow);
        sec2.getStyleClass().add("section-card");

        // ── Section 3: recipients list ──
        Label recipTitle = new Label("Recipients (0)");
        recipTitle.getStyleClass().add("section-title");

        ListView<RecipientEntry> recipList = new ListView<>(recipients);
        recipList.setPrefHeight(160);
        recipList.setCellFactory(lv -> new ListCell<>() {
            private final Label  infoLbl   = new Label();
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.getStyleClass().addAll("btn", "btn-danger");
                removeBtn.setStyle("-fx-font-size:11px; -fx-padding:4 10;");
                removeBtn.setOnAction(ev -> {
                    RecipientEntry item = getItem();
                    if (item != null) recipients.remove(item);
                });
            }
            private final HBox row = new HBox(10, infoLbl, spacer(), removeBtn);
            { row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(4, 0, 4, 0)); }

            @Override protected void updateItem(RecipientEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String contact = isSms
                        ? (item.phone().isBlank() ? "(no phone)" : item.phone())
                        : (item.email().isBlank() ? "(no email)" : item.email());
                infoLbl.setText(item.name() + "  —  " + contact);
                setGraphic(row);
            }
        });

        recipients.addListener((ListChangeListener<RecipientEntry>) ch ->
                recipTitle.setText("Recipients (" + recipients.size() + ")"));

        VBox sec3 = new VBox(10, recipTitle, recipList);
        sec3.getStyleClass().add("section-card");

        // ── Bottom button row ──
        Label sendErr = new Label();
        sendErr.getStyleClass().addAll("text-sm", "text-error");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button sendBtn = new Button("Send to 0 Recipients");
        sendBtn.getStyleClass().add("btn");

        recipients.addListener((ListChangeListener<RecipientEntry>) ch ->
                sendBtn.setText("Send to " + recipients.size() + " Recipient(s)"));

        cancelBtn.setOnAction(ev -> stage.close());
        sendBtn.setOnAction(ev -> {
            if (recipients.isEmpty()) { sendErr.setText("Add at least one recipient."); return; }
            List<Customer> toSend = recipients.stream().map(r -> {
                Customer c = new Customer();
                c.setName(r.name()); c.setPhone(r.phone()); c.setEmail(r.email());
                return c;
            }).toList();
            int sent = AppServices.alerts().sendNow(cfg, toSend);
            UiUtil.toast(App.getRoot(), sent + " message(s) sent.");
            stage.close();
        });

        HBox btnRow = new HBox(12, sendErr, spacer(), cancelBtn, sendBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(8, 24, 16, 24));

        VBox content = new VBox(16, sec1, sec2, sec3);
        content.setPadding(new Insets(16, 24, 8, 24));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(510);

        VBox outer = new VBox(headerBox, new Separator(), scroll, btnRow);
        outer.getStyleClass().add("dialog-root");

        Scene scene = new Scene(outer);
        com.sevatyres.viewmodel.ThemeManager.register(scene);
        com.sevatyres.viewmodel.ThemeManager.apply(scene);
        stage.setOnHidden(ev -> com.sevatyres.viewmodel.ThemeManager.unregister(scene));
        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    // ─── Campaign editor dialog ────────────────────────────────────────────────

    private static void showAlertDialog(AlertConfig existing, Runnable onSaved) {
        boolean isNew = existing == null;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(isNew ? "New Alert Campaign" : "Edit Alert — " + existing.getName());
        stage.setMinWidth(500);

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
        msgArea.setPrefRowCount(12);
        msgArea.setPrefHeight(220);
        msgArea.setMinHeight(180);
        msgArea.getStyleClass().add("text-area");
        msgArea.setPromptText("Message template — use {name}, {phone}, {email} as placeholders");

        Label placeholderHint = new Label("Available placeholders: {name}  {phone}  {email}");
        placeholderHint.getStyleClass().addAll("text-sm", "text-muted");

        ComboBox<AlertConfig.Channel> channelCombo = new ComboBox<>();
        channelCombo.getItems().addAll(AlertConfig.Channel.values());
        channelCombo.setValue(isNew ? AlertConfig.Channel.EMAIL
                : (existing.getChannel() != null ? existing.getChannel() : AlertConfig.Channel.EMAIL));
        channelCombo.getStyleClass().add("combo");
        channelCombo.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> intervalSpinner = new Spinner<>(1, 365,
                isNew ? 7 : existing.getIntervalDays());
        intervalSpinner.setEditable(true);
        intervalSpinner.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> durationSpinner = new Spinner<>(1, 3650,
                isNew ? 30 : existing.getDurationDays());
        durationSpinner.setEditable(true);
        durationSpinner.setMaxWidth(Double.MAX_VALUE);

        CheckBox activeCheck = new CheckBox("Active (will be included in scheduled sends)");
        activeCheck.setSelected(isNew || existing.isActive());

        Label err = new Label();
        err.getStyleClass().addAll("text-sm", "text-error");

        content.getChildren().addAll(
                title, new Separator(),
                labeled("Campaign Name *", nameField),
                new VBox(5,
                        lbl("Message Template *"),
                        msgArea, placeholderHint),
                labeled("Channel", channelCombo),
                labeled("Send every N days (interval)", intervalSpinner),
                labeled("Campaign duration (total days)", durationSpinner),
                activeCheck, err);

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
            cfg.setDurationDays(durationSpinner.getValue());
            cfg.setActive(activeCheck.isSelected());
            AppServices.alerts().save(cfg);
            stage.close();
            if (onSaved != null) onSaved.run();
        });

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(620);
        scroll.getStyleClass().add("scroll-pane");

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

    // ─── Shared UI helpers ────────────────────────────────────────────────────

    /** Null-safe empty string helper. */
    private static String nvl(String s) { return s != null ? s : ""; }

    /** A Region that grows horizontally (flex spacer). */
    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Read-only text field for displaying auto-filled data. */
    private static TextField roField(String prompt) {
        TextField tf = new TextField();
        tf.setEditable(false);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("field");
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("-fx-opacity:0.85;");
        return tf;
    }

    /** Editable text field. */
    private static TextField inputField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    /** Two-column GridPane: 80px label column + growing field column. */
    private static GridPane makeGrid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        ColumnConstraints lc = new ColumnConstraints(80);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(lc, fc);
        return g;
    }

    private static void addGridRow(GridPane g, int row, String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().addAll("text-sm", "text-muted");
        lbl.setAlignment(Pos.CENTER_RIGHT);
        lbl.setMaxWidth(Double.MAX_VALUE);
        g.add(lbl, 0, row);
        g.add(field, 1, row);
    }

    private static VBox labeled(String labelText, javafx.scene.Node field) {
        return new VBox(5, lbl(labelText), field);
    }

    private static Label lbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("text-sm", "text-muted");
        return l;
    }
}
