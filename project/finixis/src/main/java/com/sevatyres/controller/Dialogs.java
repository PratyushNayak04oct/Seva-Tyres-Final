package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.*;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.SaleTransactionService;
import com.sevatyres.viewmodel.FileGenerationService;
import com.sevatyres.viewmodel.ThemeManager;
import com.sevatyres.viewmodel.UiUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central dialog factory for Seva Tyres.
 */
public final class Dialogs {
    private Dialogs() {}

    // ─── Generic dialogs ──────────────────────────────────────────────────────

    public static boolean confirm(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        applyThemeOnShow(a);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        applyThemeOnShow(a);
        a.showAndWait();
    }

    public static void signOut(StackPane root) {
        boolean ok = confirm("Sign Out", "Sign out of Seva Tyres?",
                "You will be returned to the login screen.");
        if (ok) UiUtil.toast(root, "Signed out");
    }

    public static void showFileDownloadedDialog(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Saved");
        alert.setHeaderText("File saved successfully");
        alert.setContentText("Your file has been saved to:\n\n"
                + (file != null ? file.getAbsolutePath() : "Unknown location")
                + "\n\nClick Close to dismiss.");
        alert.getButtonTypes().setAll(ButtonType.CLOSE);
        applyThemeOnShow(alert);
        alert.showAndWait();
    }

    public static void markSettled(String what) {
        boolean ok = confirm("Mark as Settled", "Mark this " + what + " as settled?",
                "Settled items will no longer appear as pending.");
        if (ok) UiUtil.toast(App.getRoot(), what + " marked as settled");
    }

    // ─── Add Customer dialog ──────────────────────────────────────────────────

    public static void showAddCustomer(Consumer<Customer> onSaved) {
        Stage stage = buildDialogStage("Add Customer");

        VBox content = contentVBox();
        Label title = dialogTitle("Add Customer");
        Label sub   = dialogSub("Enter the new customer's details below.");

        TextField nameField  = styledField("Full name");
        TextField phoneField = styledField("Phone number");
        TextField emailField = styledField("Email address");
        TextField addrField  = styledField("Address");

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Phone", phoneField),
                labeledField("Email", emailField),
                labeledField("Address", addrField));

        Label err = errLabel();
        content.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Add Customer");
        confirmBtn.getStyleClass().add("btn");
        HBox btnRow = buttonRow(cancelBtn, confirmBtn);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            Customer saved = AppServices.customers().addCustomer(
                    name, phoneField.getText().trim(),
                    emailField.getText().trim(), addrField.getText().trim());
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        presentDialog(stage, content, btnRow);
    }

    // ─── Add Debit dialog ─────────────────────────────────────────────────────

    public static void showAddDebit(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Add Debit — " + customer.getName());

        VBox content = contentVBox();
        Label title = dialogTitle("Add Debit");
        Label sub   = dialogSub("Record an amount owed to " + customer.getName());

        TextField amountField = styledField("0.00");
        Label err = errLabel();
        content.getChildren().addAll(new VBox(4, title, sub), new Separator(),
                labeledField("Customer Name", readonlyField(customer.getName())),
                labeledField("Amount (\u20b9)", amountField), err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Confirm Debit");
        confirmBtn.getStyleClass().add("btn");
        HBox btnRow = buttonRow(cancelBtn, confirmBtn);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid positive amount.");
                return;
            }
            AppServices.transactions().addDebit(
                    customer.getId(), customer.getName(), amount, "Debit added manually");
            stage.close();
            if (onConfirm != null) onConfirm.run();
        });

        presentDialog(stage, content, btnRow);
    }

    // ─── Record Payment dialog ────────────────────────────────────────────────

    public static void showRecordPayment(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Record Payment — " + customer.getName());

        VBox content = contentVBox();
        Label title = dialogTitle("Record Payment");
        Label sub   = dialogSub("Customer: " + customer.getName() + "  ·  Select items and enter amount paid.");

        VBox itemsBox = new VBox(10);
        List<InventoryItem> inventory = AppServices.inventory().getAll();

        Label totalAmountLabel = new Label("\u20b90.00");
        totalAmountLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: -primary-600;");

        TextField paidField = styledField("0.00");

        Label remainingLabel = new Label("\u20b90.00");
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px;");

        Label err = errLabel();

        List<ComboBox<InventoryItem>> itemCombos = new ArrayList<>();
        List<TextField> qtyFields = new ArrayList<>();

        Runnable updateTotals = () -> {
            double total = 0;
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item != null) {
                    try {
                        int qty = Integer.parseInt(qtyFields.get(i).getText().trim());
                        if (qty > 0) total += item.getUnitPrice() * qty;
                    } catch (NumberFormatException ignored) {}
                }
            }
            double t = total;
            totalAmountLabel.setText(UiUtil.money(t));
            double paid = 0;
            try { paid = Double.parseDouble(paidField.getText().trim()); } catch (Exception ignored) {}
            double remaining = t - paid;
            remainingLabel.setText(UiUtil.money(Math.max(0, remaining)));
            remainingLabel.setStyle(remaining > 0
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };

        Runnable[] addRowRef = new Runnable[1];
        addRowRef[0] = () -> {
            ComboBox<InventoryItem> combo = new ComboBox<>();
            combo.getItems().addAll(inventory);
            combo.setPromptText("Select item\u2026");
            combo.setPrefWidth(220);
            combo.getStyleClass().add("combo");
            combo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(InventoryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null
                            : item.getName() + "  (" + UiUtil.money(item.getUnitPrice()) + "/unit)");
                }
            });
            combo.setButtonCell(combo.getCellFactory().call(null));

            TextField qtyField = styledField("0");
            qtyField.setText("0");
            qtyField.setPrefWidth(70);

            Label availLabel = new Label();
            availLabel.setStyle("-fx-font-size:11px;");

            Runnable updateAvail = () -> {
                InventoryItem sel = combo.getValue();
                if (sel == null) { availLabel.setText(""); return; }
                InventoryItem fresh = AppServices.inventory().getById(sel.getId()).orElse(sel);
                int avail = fresh.getQuantity();
                int reqQty = 0;
                try { reqQty = Integer.parseInt(qtyField.getText().trim()); } catch (NumberFormatException ignored) {}
                if (reqQty > avail) {
                    availLabel.setText("Only " + avail + " available");
                    availLabel.setStyle("-fx-font-size:11px; -fx-text-fill: -error-600;");
                } else {
                    availLabel.setText("Available: " + avail + " units");
                    availLabel.setStyle("-fx-font-size:11px; -fx-text-fill: -success-600;");
                }
            };

            combo.valueProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotals.run(); });
            qtyField.textProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotals.run(); });

            Button removeBtn = new Button("\u2715");
            removeBtn.getStyleClass().addAll("btn", "btn-secondary");
            removeBtn.setStyle("-fx-padding: 4 8;");

            HBox row = new HBox(8, combo, new Label("Qty:"), qtyField, availLabel, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);

            itemCombos.add(combo);
            qtyFields.add(qtyField);
            itemsBox.getChildren().add(row);

            removeBtn.setOnAction(ev -> {
                int idx = itemsBox.getChildren().indexOf(row);
                if (idx >= 0 && itemsBox.getChildren().size() > 1) {
                    itemsBox.getChildren().remove(row);
                    itemCombos.remove(idx);
                    qtyFields.remove(idx);
                    updateTotals.run();
                }
            });
        };
        addRowRef[0].run();

        Button addMoreBtn = new Button("+ Add another item");
        addMoreBtn.getStyleClass().addAll("btn", "btn-ghost");
        addMoreBtn.setOnAction(ev -> { addRowRef[0].run(); updateTotals.run(); });

        paidField.textProperty().addListener((obs, o, n) -> updateTotals.run());

        Label itemsLabel = new Label("ITEMS");
        itemsLabel.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        HBox totalRow = new HBox(12, new Label("Total Amount:"), totalAmountLabel);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        HBox remainingRow = new HBox(12, new Label("Remaining Amount:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                itemsLabel, itemsBox, addMoreBtn,
                new Separator(),
                totalRow, labeledField("Paid Amount (\u20b9)", paidField), remainingRow, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Confirm Payment");
        confirmBtn.getStyleClass().add("btn");
        HBox btnRow = buttonRow(cancelBtn, confirmBtn);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            boolean hasItem = itemCombos.stream().anyMatch(c -> c.getValue() != null);
            if (!hasItem) { err.setText("Please select at least one item."); return; }

            // Task 6: Validate qty against available stock
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item == null) continue;
                int qty = 1;
                try { qty = Math.max(1, Integer.parseInt(qtyFields.get(i).getText().trim())); }
                catch (NumberFormatException ignored) {}
                InventoryItem fresh = AppServices.inventory().getById(item.getId()).orElse(item);
                if (qty > fresh.getQuantity()) {
                    err.setText("Quantity for \"" + item.getName() + "\" exceeds available stock ("
                            + fresh.getQuantity() + " available).");
                    return;
                }
            }

            double paid;
            try {
                paid = Double.parseDouble(paidField.getText().trim());
                if (paid < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid paid amount.");
                return;
            }

            List<TransactionLineItem> lineItems = new ArrayList<>();
            StringBuilder desc = new StringBuilder();
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item != null) {
                    int qty = 1;
                    try { qty = Math.max(1, Integer.parseInt(qtyFields.get(i).getText().trim())); }
                    catch (NumberFormatException ignored) {}
                    lineItems.add(new TransactionLineItem(
                            item.getId(), item.getName(), qty, item.getUnitPrice()));
                    if (!desc.isEmpty()) desc.append(", ");
                    desc.append(qty).append("x ").append(item.getName());
                }
            }

            AppServices.transactions().recordPayment(
                    customer.getId(), customer.getName(), lineItems, paid, "Payment: " + desc);
            stage.close();
            if (onConfirm != null) onConfirm.run();
        });

        presentDialog(stage, content, btnRow);
    }

    // ─── New Sale Transaction dialog ──────────────────────────────────────────

    public static void showNewSaleTransaction(Consumer<SaleTransaction> onSaved) {
        Stage stage = buildDialogStage("New Transaction");
        VBox content = contentVBox();
        Label title = dialogTitle("New Transaction");
        Label sub   = dialogSub("Select a product from inventory or enter manually. Scan barcode for quick lookup.");

        // ── Basic fields ──────────────────────────────────────────────────────
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("date-picker");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField billNoField = styledField("Bill number (optional — auto-generated if blank)");

        // ── Barcode scanner field ─────────────────────────────────────────────
        TextField barcodeField = styledField("Scan barcode or type it and press Enter...");
        barcodeField.getStyleClass().add("field");
        Label barcodeStatus = new Label();
        barcodeStatus.setStyle("-fx-font-size:12px;");
        VBox barcodeSection = new VBox(6,
                labeledField("Barcode (USB scanner / manual)", barcodeField),
                barcodeStatus);

        // ── Particulars: text field + inventory dropdown ───────────────────────
        TextField particularsField = styledField("Product or service name");
        List<InventoryItem> inventoryItems = AppServices.inventory().getAll();
        ComboBox<InventoryItem> inventoryCombo = new ComboBox<>();
        inventoryCombo.getItems().addAll(inventoryItems);
        inventoryCombo.setPromptText("Select from inventory…");
        inventoryCombo.getStyleClass().add("combo");
        inventoryCombo.setPrefWidth(220);
        inventoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String stock = item.getQuantity() == 0
                        ? "Out of Stock" : "Stock: " + item.getQuantity();
                setText(item.getName() + "  [" + stock + "]");
            }
        });
        inventoryCombo.setButtonCell(inventoryCombo.getCellFactory().call(null));

        HBox particularsRow = new HBox(8, particularsField, inventoryCombo);
        HBox.setHgrow(particularsField, Priority.ALWAYS);

        // stock info label
        Label stockInfoLabel = new Label();
        stockInfoLabel.setStyle("-fx-font-size:12px; -fx-text-fill: -neutral-500;");

        TextField brandField = styledField("Brand (optional)");

        TextField qtyField = styledField("1");
        qtyField.setText("1");
        Label stockErrLabel = new Label();
        stockErrLabel.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        TextField unitPriceField = styledField("0.00");
        unitPriceField.setText("0.00");

        // ── Totals (read-only display) ─────────────────────────────────────────
        Label itemTotalLabel = new Label("\u20b90.00");
        itemTotalLabel.setStyle("-fx-font-weight:700; -fx-font-size:18px; -fx-text-fill: -primary-600;");

        Label paidTotalLabel = new Label("\u20b90.00");
        paidTotalLabel.setStyle("-fx-font-weight:600; -fx-font-size:15px; -fx-text-fill: -neutral-500;");

        Label remainingLabel = new Label("\u20b90.00");
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #38a169;");

        // ── Payment fields ─────────────────────────────────────────────────────
        TextField phonePeField    = styledField("0.00");
        TextField acTransferField = styledField("0.00");
        TextField cardSwipeField  = styledField("0.00");
        TextField bajajField      = styledField("0.00");
        TextField cashField       = styledField("0.00");
        TextField chequeField     = styledField("0.00");

        // ── Shared state ───────────────────────────────────────────────────────
        final InventoryItem[] selectedItem = {null};

        Runnable updateCalculations = () -> {
            double unitPrice = parseDouble(unitPriceField);
            int qty;
            try { qty = Math.max(1, Integer.parseInt(qtyField.getText().trim())); }
            catch (NumberFormatException ignored) { qty = 1; }
            double itemTotal = unitPrice * qty;
            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            double remaining = Math.max(0, itemTotal - paid);

            itemTotalLabel.setText(UiUtil.money(itemTotal));
            paidTotalLabel.setText(UiUtil.money(paid));
            remainingLabel.setText(UiUtil.money(remaining));
            remainingLabel.setStyle(remaining > 0.009
                    ? "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #e53e3e;"
                    : "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #38a169;");
        };

        for (TextField tf : new TextField[]{unitPriceField, qtyField, phonePeField,
                acTransferField, cardSwipeField, bajajField, cashField, chequeField}) {
            tf.textProperty().addListener((obs, o, n) -> updateCalculations.run());
        }

        // Fill fields when inventory item selected
        Runnable applySelectedItem = () -> {
            InventoryItem item = selectedItem[0];
            if (item == null) return;
            particularsField.setText(item.getName());
            unitPriceField.setText(String.format("%.2f", item.getUnitPrice()));
            String info = "Available stock: " + item.getQuantity() + " unit(s)";
            stockInfoLabel.setText(info);
            stockInfoLabel.setStyle(item.getQuantity() == 0
                    ? "-fx-font-size:12px; -fx-text-fill: #e53e3e;"
                    : "-fx-font-size:12px; -fx-text-fill: -neutral-500;");
            updateCalculations.run();
        };

        inventoryCombo.setOnAction(e -> {
            InventoryItem item = inventoryCombo.getValue();
            if (item != null) { selectedItem[0] = item; applySelectedItem.run(); }
        });

        // Barcode lookup: press Enter or Tab in barcodeField
        barcodeField.setOnAction(e -> {
            String code = barcodeField.getText().trim();
            if (code.isEmpty()) return;
            AppServices.inventory().getByBarcode(code).ifPresentOrElse(
                    item -> {
                        selectedItem[0] = item;
                        inventoryCombo.setValue(item);
                        applySelectedItem.run();
                        barcodeStatus.setText("✓ Found: " + item.getName());
                        barcodeStatus.setStyle("-fx-font-size:12px; -fx-text-fill: #38a169;");
                    },
                    () -> {
                        barcodeStatus.setText("✗ No product found for barcode: " + code);
                        barcodeStatus.setStyle("-fx-font-size:12px; -fx-text-fill: #e53e3e;");
                    });
        });

        // Qty stock validation label (shown while typing)
        qtyField.textProperty().addListener((obs, o, n) -> {
            if (selectedItem[0] == null) { stockErrLabel.setText(""); return; }
            try {
                int qty = Integer.parseInt(n.trim());
                InventoryItem fresh = AppServices.inventory()
                        .getById(selectedItem[0].getId()).orElse(selectedItem[0]);
                if (qty > fresh.getQuantity()) {
                    stockErrLabel.setText("⚠ Only " + fresh.getQuantity() + " in stock");
                } else {
                    stockErrLabel.setText("");
                }
            } catch (NumberFormatException ignored) { stockErrLabel.setText(""); }
        });

        // ── Customer section (shown when there's a remaining balance) ──────────
        Label custHeader = new Label("CUSTOMER DETAILS (required when amount is on credit)");
        custHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        List<Customer> existingCustomers = AppServices.customers().getAll();
        ComboBox<Customer> existingCustCombo = new ComboBox<>();
        existingCustCombo.getItems().addAll(existingCustomers);
        existingCustCombo.setPromptText("Select existing customer…");
        existingCustCombo.setMaxWidth(Double.MAX_VALUE);
        existingCustCombo.getStyleClass().add("combo");
        existingCustCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null
                        : c.getName() + (c.getPhone() != null ? " — " + c.getPhone() : ""));
            }
        });
        existingCustCombo.setButtonCell(existingCustCombo.getCellFactory().call(null));

        TextField custNameField  = styledField("Customer name");
        TextField custPhoneField = styledField("Phone");
        TextField custEmailField = styledField("Email");
        TextField custAddrField  = styledField("Address (optional)");

        existingCustCombo.setOnAction(e -> {
            Customer c = existingCustCombo.getValue();
            if (c != null) {
                custNameField.setText(c.getName());
                custPhoneField.setText(c.getPhone() != null ? c.getPhone() : "");
                custEmailField.setText(c.getEmail() != null ? c.getEmail() : "");
                custAddrField.setText(c.getAddress() != null ? c.getAddress() : "");
            }
        });

        VBox customerSection = new VBox(10,
                custHeader,
                labeledField("Existing Customer (optional)", existingCustCombo),
                labeledField("Name *", custNameField),
                labeledField("Phone", custPhoneField),
                labeledField("Email", custEmailField),
                labeledField("Address", custAddrField));

        Label err = errLabel();

        // ── Layout ─────────────────────────────────────────────────────────────
        Label payHeader = new Label("PAYMENT METHODS");
        payHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        VBox payForm = new VBox(10, payHeader,
                labeledField("PhonePe (UPI) \u20b9", phonePeField),
                labeledField("Account Transfer \u20b9", acTransferField),
                labeledField("Card Swipe \u20b9", cardSwipeField),
                labeledField("Bajaj Finance \u20b9", bajajField),
                labeledField("Cash \u20b9", cashField),
                labeledField("Cheque \u20b9", chequeField));

        HBox summaryRow = new HBox(24,
                new VBox(4, new Label("Item Total") {{ setStyle("-fx-font-size:11px;"); }}, itemTotalLabel),
                new VBox(4, new Label("Paid") {{ setStyle("-fx-font-size:11px;"); }}, paidTotalLabel),
                new VBox(4, new Label("Remaining (Credit)") {{ setStyle("-fx-font-size:11px;"); }}, remainingLabel));
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        summaryRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 14 16; -fx-background-radius: 8;");

        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                barcodeSection,
                labeledField("Date", datePicker),
                labeledField("Bill No", billNoField),
                new VBox(6, new Label("Particulars *") {{ setStyle("-fx-font-size:12px; -fx-text-fill: -text-muted;"); }},
                        particularsRow, stockInfoLabel),
                new VBox(6, stockErrLabel, labeledField("Quantity", qtyField)),
                labeledField("Unit Price \u20b9", unitPriceField),
                labeledField("Brand", brandField),
                new Separator(),
                payForm,
                new Separator(),
                summaryRow,
                new Separator(),
                customerSection,
                err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Save Transaction");
        saveBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            String particulars = particularsField.getText().trim();
            if (particulars.isEmpty()) { err.setText("Particulars is required."); return; }

            int qty = 1;
            try { qty = Math.max(1, Integer.parseInt(qtyField.getText().trim())); }
            catch (NumberFormatException ex) { err.setText("Quantity must be a valid number."); return; }

            // Stock check
            if (selectedItem[0] != null) {
                InventoryItem fresh = AppServices.inventory()
                        .getById(selectedItem[0].getId()).orElse(selectedItem[0]);
                if (qty > fresh.getQuantity()) {
                    err.setText("Not enough stock for \"" + fresh.getName()
                            + "\" (" + fresh.getQuantity() + " available).");
                    return;
                }
            }

            double unitPrice = parseDouble(unitPriceField);
            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            double itemTotal = unitPrice * qty;
            double remaining = Math.max(0, itemTotal - paid);

            // Require customer when there is credit
            String custName = custNameField.getText().trim();
            if (remaining > 0.009 && custName.isEmpty()) {
                err.setText("Customer name is required when there is a remaining balance (credit).");
                return;
            }

            SaleTransaction tx = new SaleTransaction();
            tx.setBillNo(billNoField.getText().trim());
            tx.setSaleDate(datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now());
            tx.setParticulars(particulars);
            tx.setBrand(brandField.getText().trim());
            tx.setQuantity(qty);
            tx.setUnitPrice(unitPrice);
            if (selectedItem[0] != null) tx.setInventoryItemId(selectedItem[0].getId());
            tx.setPhonePe(parseDouble(phonePeField));
            tx.setAccountTransfer(parseDouble(acTransferField));
            tx.setCardSwipe(parseDouble(cardSwipeField));
            tx.setBajajFinance(parseDouble(bajajField));
            tx.setCash(parseDouble(cashField));
            tx.setCheque(parseDouble(chequeField));

            // Customer info
            Customer selCust = existingCustCombo.getValue();
            if (selCust != null) {
                tx.setCustomerId(selCust.getId());
                tx.setCustomerName(selCust.getName());
                tx.setCustomerEmail(selCust.getEmail());
                tx.setCustomerPhone(selCust.getPhone());
                tx.setCustomerAddress(selCust.getAddress());
            } else if (!custName.isEmpty()) {
                tx.setCustomerName(custName);
                tx.setCustomerPhone(custPhoneField.getText().trim());
                tx.setCustomerEmail(custEmailField.getText().trim());
                tx.setCustomerAddress(custAddrField.getText().trim());
            }

            // computeTotal() inside save() will auto-set creditAmount = remaining
            SaleTransaction saved = AppServices.saleTransactions().save(tx);
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn));
    }

    // ─── View Sale Transaction dialog ─────────────────────────────────────────

    public static void showViewSaleTransaction(SaleTransaction t) {
        Stage stage = buildDialogStage("Transaction Details");
        VBox content = contentVBox();
        Label title = dialogTitle("Transaction Details");

        VBox form = new VBox(10,
                labeledField("Date",          readonlyField(UiUtil.date(t.getSaleDate()))),
                labeledField("Bill No",        readonlyField(t.getBillNo() != null ? t.getBillNo() : "\u2014")),
                labeledField("Particulars",    readonlyField(t.getParticulars() != null ? t.getParticulars() : "\u2014")),
                labeledField("Brand",          readonlyField(t.getBrand() != null ? t.getBrand() : "\u2014")),
                labeledField("Quantity",       readonlyField(String.valueOf(t.getQuantity()))),
                labeledField("PhonePe",        readonlyField(UiUtil.money(t.getPhonePe()))),
                labeledField("A/C Transfer",   readonlyField(UiUtil.money(t.getAccountTransfer()))),
                labeledField("Card Swipe",     readonlyField(UiUtil.money(t.getCardSwipe()))),
                labeledField("Bajaj Finance",  readonlyField(UiUtil.money(t.getBajajFinance()))),
                labeledField("Cash",           readonlyField(UiUtil.money(t.getCash()))),
                labeledField("Cheque",         readonlyField(UiUtil.money(t.getCheque()))),
                labeledField("Credit",         readonlyField(UiUtil.money(t.getCreditAmount()))),
                labeledField("Total",          readonlyField(UiUtil.money(t.getTotal()))));

        if (t.getCustomerName() != null) {
            form.getChildren().add(new Separator());
            form.getChildren().add(labeledField("Customer", readonlyField(t.getCustomerName())));
            if (t.getCustomerPhone() != null && !t.getCustomerPhone().isBlank())
                form.getChildren().add(labeledField("Phone", readonlyField(t.getCustomerPhone())));
            if (t.getCustomerEmail() != null && !t.getCustomerEmail().isBlank())
                form.getChildren().add(labeledField("Email", readonlyField(t.getCustomerEmail())));
        }

        content.getChildren().addAll(title, new Separator(), form);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> stage.close());

        presentDialog(stage, content, buttonRow(closeBtn));
    }

    // ─── Edit Sale Transaction dialog ─────────────────────────────────────────

    public static void showEditSaleTransaction(SaleTransaction t, Runnable onSaved) {
        Stage stage = buildDialogStage("Edit Transaction — Bill #" + t.getBillNo());
        VBox content = contentVBox();
        Label title = dialogTitle("Edit Transaction");

        DatePicker datePicker = new DatePicker(t.getSaleDate());
        datePicker.getStyleClass().add("date-picker");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField billNoField      = styledField("Bill No"); billNoField.setText(t.getBillNo() != null ? t.getBillNo() : "");
        TextField particularsField = styledField("Particulars"); particularsField.setText(t.getParticulars() != null ? t.getParticulars() : "");
        TextField brandField       = styledField("Brand"); brandField.setText(t.getBrand() != null ? t.getBrand() : "");
        TextField qtyField         = styledField("Qty"); qtyField.setText(String.valueOf(t.getQuantity()));

        TextField phonePeField     = amtField(t.getPhonePe());
        TextField acTransferField  = amtField(t.getAccountTransfer());
        TextField cardSwipeField   = amtField(t.getCardSwipe());
        TextField bajajField       = amtField(t.getBajajFinance());
        TextField cashField        = amtField(t.getCash());
        TextField chequeField      = amtField(t.getCheque());
        TextField creditField      = amtField(t.getCreditAmount());

        Label totalLabel = new Label(UiUtil.money(t.getTotal()));
        totalLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: -primary-600;");

        Runnable updateTotal = () -> {
            double total = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField)
                    + parseDouble(creditField);
            totalLabel.setText(UiUtil.money(total));
        };
        for (TextField tf : new TextField[]{phonePeField, acTransferField, cardSwipeField,
                bajajField, cashField, chequeField, creditField}) {
            tf.textProperty().addListener((obs, o, n) -> updateTotal.run());
        }

        // Task 5: Credit validation — check if credit amount exceeds outstanding
        Label creditWarning = new Label();
        creditWarning.setStyle("-fx-text-fill: -error-600; -fx-font-size:11px;");
        creditField.textProperty().addListener((obs, o, n) -> {
            // If customer has a credit account, validate
            if (t.getCustomerId() != null) {
                double remaining = AppServices.customers().getBalance(t.getCustomerId());
                double newCredit = parseDouble(creditField);
                if (remaining > 0 && newCredit > remaining) {
                    creditWarning.setText("Credit exceeds outstanding balance (\u20b9"
                            + String.format("%.2f", remaining) + ")");
                } else {
                    creditWarning.setText("");
                }
            }
        });

        Label err = errLabel();

        content.getChildren().addAll(title, new Separator(),
                labeledField("Date", datePicker),
                labeledField("Bill No *", billNoField),
                labeledField("Particulars *", particularsField),
                labeledField("Brand", brandField),
                labeledField("Quantity", qtyField),
                new Separator(),
                new Label("PAYMENT METHODS") {{
                    setStyle("-fx-font-size:11px; -fx-font-weight:700;");
                }},
                labeledField("PhonePe \u20b9", phonePeField),
                labeledField("A/C Transfer \u20b9", acTransferField),
                labeledField("Card Swipe \u20b9", cardSwipeField),
                labeledField("Bajaj Finance \u20b9", bajajField),
                labeledField("Cash \u20b9", cashField),
                labeledField("Cheque \u20b9", chequeField),
                labeledField("Credit \u20b9", creditField),
                creditWarning,
                labeledField("Total", totalLabel),
                err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            if (!creditWarning.getText().isEmpty()) { err.setText(creditWarning.getText()); return; }
            String billNo = billNoField.getText().trim();
            if (billNo.isEmpty()) { err.setText("Bill number is required."); return; }
            String particulars = particularsField.getText().trim();
            if (particulars.isEmpty()) { err.setText("Particulars is required."); return; }

            t.setSaleDate(datePicker.getValue());
            t.setBillNo(billNo);
            t.setParticulars(particulars);
            t.setBrand(brandField.getText().trim());
            try { t.setQuantity(Math.max(1, Integer.parseInt(qtyField.getText().trim()))); }
            catch (NumberFormatException ex) {}
            t.setPhonePe(parseDouble(phonePeField));
            t.setAccountTransfer(parseDouble(acTransferField));
            t.setCardSwipe(parseDouble(cardSwipeField));
            t.setBajajFinance(parseDouble(bajajField));
            t.setCash(parseDouble(cashField));
            t.setCheque(parseDouble(chequeField));
            t.setCreditAmount(parseDouble(creditField));
            t.computeTotal();

            AppServices.saleTransactions().update(t);
            stage.close();
            if (onSaved != null) onSaved.run();
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn));
    }

    // ─── Generate Transaction Report dialog ───────────────────────────────────

    public static void showGenerateTransactionReport(SaleTransactionService saleService) {
        Stage stage = buildDialogStage("Generate Transaction Report");
        VBox content = contentVBox();
        Label title = dialogTitle("Generate Report");
        Label sub   = dialogSub("Choose a date range and output format.");

        DatePicker fromDate = new DatePicker(LocalDate.now().minusMonths(1));
        fromDate.getStyleClass().add("date-picker");
        fromDate.setMaxWidth(Double.MAX_VALUE);

        DatePicker toDate = new DatePicker(LocalDate.now());
        toDate.getStyleClass().add("date-picker");
        toDate.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton pdfRadio   = new RadioButton("PDF");
        RadioButton excelRadio = new RadioButton("Excel");
        pdfRadio.setToggleGroup(formatGroup);
        excelRadio.setToggleGroup(formatGroup);
        pdfRadio.setSelected(true);

        CheckBox allTimeCheck = new CheckBox("Show all transactions (ignore date range)");

        HBox formatRow = new HBox(20, pdfRadio, excelRadio);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        Label err = errLabel();

        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                labeledField("From Date", fromDate),
                labeledField("To Date", toDate),
                allTimeCheck,
                labeledField("Format", formatRow),
                err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button generateBtn = new Button("Generate");
        generateBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        generateBtn.setOnAction(e -> {
            var txns = allTimeCheck.isSelected()
                    ? saleService.getAll()
                    : saleService.getByDateRange(fromDate.getValue(), toDate.getValue());

            boolean isPdf = pdfRadio.isSelected();
            try {
                var gf = FileGenerationService.generateSaleReport(txns, isPdf ? "PDF" : "Excel");
                AppServices.reports().saveFile(gf);
                stage.close();
                showFileDownloadedDialog(gf.getFile());
                if (java.awt.Desktop.isDesktopSupported()
                        && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(gf.getFile());
                }
            } catch (Exception ex) {
                err.setText("Error generating report: " + ex.getMessage());
            }
        });

        presentDialog(stage, content, buttonRow(cancelBtn, generateBtn));
    }

    // ─── Add Inventory Item dialog ────────────────────────────────────────────

    public static void showAddItem(Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Add Inventory Item");
        VBox content = contentVBox();
        Label title = dialogTitle("Add Item");
        Label sub   = dialogSub("Enter details. Attach a barcode so items can be scanned in transactions.");

        TextField nameField    = styledField("Item name");
        TextField qtyField     = styledField("0");
        TextField priceField   = styledField("0.00");
        TextField barcodeField = styledField("Barcode (scan or type, optional)");
        Label barcodeHint = new Label("Attach a USB barcode scanner to auto-fill this field.");
        barcodeHint.setStyle("-fx-font-size:11px; -fx-text-fill: -neutral-400;");

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Quantity", qtyField),
                labeledField("Unit Price (\u20b9)", priceField),
                new VBox(4, labeledField("Barcode", barcodeField), barcodeHint));

        Label err = errLabel();
        content.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Add Item");
        confirmBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            int qty;
            try { qty = Integer.parseInt(qtyField.getText().trim()); if (qty < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Quantity must be a non-negative integer."); return; }
            double price;
            try { price = Double.parseDouble(priceField.getText().trim()); if (price < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Unit price must be a non-negative number."); return; }

            InventoryItem item = new InventoryItem();
            item.setName(name);
            item.setQuantity(qty);
            item.setUnitPrice(price);
            String bc = barcodeField.getText().trim();
            if (!bc.isEmpty()) item.setBarcode(bc);
            InventoryItem saved = AppServices.inventory().addItem(item);
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn));
    }

    // ─── Edit Inventory Item dialog ───────────────────────────────────────────

    public static void showEditItem(InventoryItem item, Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Edit Item — " + item.getName());
        VBox content = contentVBox();
        Label title = dialogTitle("Edit Item");
        Label sub   = dialogSub("Update the details for \"" + item.getName() + "\".");

        TextField nameField    = styledField("Item name");  nameField.setText(item.getName());
        TextField qtyField     = styledField("0");          qtyField.setText(String.valueOf(item.getQuantity()));
        TextField priceField   = styledField("0.00");       priceField.setText(String.format("%.2f", item.getUnitPrice()));
        TextField barcodeField = styledField("Barcode (scan or type, optional)");
        if (item.getBarcode() != null) barcodeField.setText(item.getBarcode());

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Quantity", qtyField),
                labeledField("Unit Price (\u20b9)", priceField),
                labeledField("Barcode", barcodeField));

        Label err = errLabel();
        content.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Save Changes");
        confirmBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            int qty;
            try { qty = Integer.parseInt(qtyField.getText().trim()); if (qty < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Quantity must be a non-negative integer."); return; }
            double price;
            try { price = Double.parseDouble(priceField.getText().trim()); if (price < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Unit price must be a non-negative number."); return; }
            item.setName(name);
            item.setQuantity(qty);
            item.setUnitPrice(price);
            String bc = barcodeField.getText().trim();
            item.setBarcode(bc.isEmpty() ? null : bc);
            InventoryItem updated = AppServices.inventory().updateItem(item);
            stage.close();
            if (onSaved != null) onSaved.accept(updated);
        });

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn));
    }

    // ─── Stock Adjustment dialog ──────────────────────────────────────────────

    public static void showStockAdjust(List<InventoryItem> items, BiConsumer<Integer, Integer> onAdjust) {
        Stage stage = buildDialogStage("Adjust Stock");

        VBox content = contentVBox();
        Label title = dialogTitle("Stock Adjustment");
        Label sub   = dialogSub("Select an item and enter the stock change amount.");

        ComboBox<InventoryItem> itemCombo = new ComboBox<>();
        itemCombo.getItems().addAll(items);
        itemCombo.setPromptText("Select item\u2026");
        itemCombo.setMaxWidth(Double.MAX_VALUE);
        itemCombo.getStyleClass().add("combo");
        itemCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InventoryItem it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.getName() + " (Qty: " + it.getQuantity() + ")");
            }
        });
        itemCombo.setButtonCell(itemCombo.getCellFactory().call(null));

        TextField deltaField = styledField("e.g. +10 or -5");

        VBox form = new VBox(14,
                labeledField("Item", itemCombo),
                labeledField("Quantity Change (+/-)", deltaField));

        Label err = errLabel();
        content.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Adjust Stock");
        confirmBtn.getStyleClass().add("btn");
        HBox btnRow = buttonRow(cancelBtn, confirmBtn);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            InventoryItem selected = itemCombo.getValue();
            if (selected == null) { err.setText("Please select an item."); return; }
            int delta;
            try {
                String text = deltaField.getText().trim().replace("+", "");
                delta = Integer.parseInt(text);
                if (delta == 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Enter a non-zero integer (e.g. +10 or -5).");
                return;
            }
            stage.close();
            if (onAdjust != null) onAdjust.accept(selected.getId(), delta);
        });

        presentDialog(stage, content, btnRow);
    }

    // ─── Add Credit Info dialog ───────────────────────────────────────────────

    public static void showAddCreditInfo() {
        Stage stage = buildDialogStage("Add New Credit");
        VBox content = contentVBox();
        Label title = dialogTitle("Add New Credit");
        Label msg = new Label(
                "Credits are created automatically when you record a payment on a customer's page.\n\n"
                + "Navigate to Accounts \u2192 open a customer \u2192 Record Payment to add a credit entry.\n\n"
                + "Or use Transactions \u2192 New Transaction and enter a credit amount.");
        msg.setWrapText(true);
        msg.getStyleClass().add("txn-meta");

        content.getChildren().addAll(new VBox(8, title, msg), new Separator());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button goBtn = new Button("Go to Customers");
        goBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        goBtn.setOnAction(e -> { stage.close(); App.getShell().navigate("accounts"); });

        presentDialog(stage, content, buttonRow(cancelBtn, goBtn));
    }

    // ─── Edit Transaction dialog (credit amount validation — Task 5) ──────────

    public static void showEditTransaction(Transaction t, Runnable onSave) {
        Stage stage = buildDialogStage("Edit Transaction");

        VBox content = contentVBox();
        Label title = dialogTitle("Edit Transaction");

        VBox infoForm = new VBox(14,
                labeledField("Description / Items", readonlyField(
                        t.getDescription() != null ? t.getDescription() : t.getType().name())),
                labeledField("Original Total Amount (\u20b9)", readonlyField(UiUtil.money(t.getAmount()))),
                labeledField("Transaction Date",      readonlyField(UiUtil.date(t.getDate()))),
                labeledField("Current Balance (\u20b9)", readonlyField(UiUtil.money(t.getBalance()))));

        TextField payLaterField = styledField("0");
        payLaterField.setText("0");

        DatePicker paymentDatePicker = new DatePicker(LocalDate.now());
        paymentDatePicker.getStyleClass().add("date-picker");
        paymentDatePicker.setMaxWidth(Double.MAX_VALUE);

        Label remainingLabel = new Label(UiUtil.money(t.getBalance()));
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");

        // Task 5: Validate against remaining balance
        Label overErr = new Label();
        overErr.setStyle("-fx-text-fill: -error-600; -fx-font-size:11px;");

        Runnable updateRemaining = () -> {
            double payAmt = 0;
            try { payAmt = Double.parseDouble(payLaterField.getText().trim()); }
            catch (NumberFormatException ignored) {}

            // Validate: cannot pay more than remaining balance
            if (t.getType() == Transaction.Type.CREDIT && payAmt > t.getBalance()) {
                overErr.setText("Amount exceeds remaining balance (\u20b9"
                        + String.format("%.2f", t.getBalance()) + ").");
            } else if (t.getType() == Transaction.Type.DEBIT && payAmt > t.getBalance()) {
                overErr.setText("Amount exceeds outstanding debit (\u20b9"
                        + String.format("%.2f", t.getBalance()) + ").");
            } else {
                overErr.setText("");
            }

            double newBal = t.getBalance() - payAmt;
            remainingLabel.setText(UiUtil.money(Math.max(0, newBal)));
            remainingLabel.setStyle(newBal > 0
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };

        payLaterField.textProperty().addListener((obs, o, n) -> updateRemaining.run());

        VBox editForm = new VBox(14,
                labeledField("Pay Later Amount (\u20b9)", payLaterField),
                labeledField("Payment Date", paymentDatePicker));

        HBox remainingRow = new HBox(12, new Label("Remaining Balance:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        Label err = errLabel();
        content.getChildren().addAll(title, new Separator(), infoForm, new Separator(),
                editForm, overErr, remainingRow, err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            if (!overErr.getText().isEmpty()) { err.setText(overErr.getText()); return; }
            double payAmt;
            try {
                payAmt = Double.parseDouble(payLaterField.getText().trim());
                if (payAmt < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid non-negative amount.");
                return;
            }
            if (payAmt == 0) { stage.close(); return; }
            LocalDate payDate = paymentDatePicker.getValue() != null
                    ? paymentDatePicker.getValue() : LocalDate.now();
            AppServices.transactions().recordPartialPayment(t.getId(), payAmt, payDate);
            t.setBalance(Math.max(0, t.getBalance() - payAmt));
            t.setPaidAmount(t.getPaidAmount() + payAmt);
            t.setOngoing(t.getBalance() > 0);
            stage.close();
            if (onSave != null) onSave.run();
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn));
    }

    // ─── View Transaction dialog ──────────────────────────────────────────────

    public static void showViewTransaction(Transaction t) {
        Stage stage = buildDialogStage("Transaction Details");
        VBox content = contentVBox();
        Label title = dialogTitle("Transaction Details");

        VBox form = new VBox(14,
                labeledField("Customer",           readonlyField(t.getCustomerName() != null ? t.getCustomerName() : "\u2014")),
                labeledField("Type",               readonlyField(t.getType().name())),
                labeledField("Total Amount (\u20b9)", readonlyField(UiUtil.money(t.getAmount()))),
                labeledField("Paid Amount (\u20b9)",  readonlyField(UiUtil.money(t.getPaidAmount()))),
                labeledField("Remaining Balance (\u20b9)", readonlyField(UiUtil.money(t.getBalance()))),
                labeledField("Date",               readonlyField(UiUtil.date(t.getDate()))),
                labeledField("Status",             readonlyField(t.isOngoing() ? "Pending" : "All Cleared")),
                labeledField("Description",        readonlyField(t.getDescription() != null ? t.getDescription() : "\u2014")));

        content.getChildren().addAll(title, new Separator(), form);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> stage.close());

        presentDialog(stage, content, buttonRow(closeBtn));
    }

    // ─── Layout helpers ───────────────────────────────────────────────────────

    private static void presentDialog(Stage stage, VBox contentVBox, HBox buttonRow) {
        contentVBox.setPrefWidth(540);
        ScrollPane scroll = new ScrollPane(contentVBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(580);
        scroll.getStyleClass().add("scroll-pane");
        VBox outer = new VBox(scroll, buttonRow);
        outer.getStyleClass().add("dialog-root");
        Scene scene = new Scene(outer);
        ThemeManager.register(scene);
        ThemeManager.apply(scene);
        stage.setOnHidden(e -> ThemeManager.unregister(scene));
        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    private static Stage buildDialogStage(String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(title);
        stage.setMinWidth(540);
        stage.setMaxWidth(740);
        stage.setResizable(true);
        return stage;
    }

    private static VBox contentVBox() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24, 28, 16, 28));
        box.getStyleClass().add("dialog-root");
        return box;
    }

    private static HBox buttonRow(Button... buttons) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 28, 16, 28));
        row.getChildren().addAll(buttons);
        return row;
    }

    private static Label dialogTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        return lbl;
    }

    private static Label dialogSub(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("txn-meta");
        lbl.setWrapText(true);
        return lbl;
    }

    private static Label errLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");
        return lbl;
    }

    private static void applyThemeOnShow(Alert alert) {
        alert.setOnShowing(e -> {
            Scene scene = alert.getDialogPane().getScene();
            if (scene != null) { ThemeManager.register(scene); ThemeManager.apply(scene); }
        });
        alert.setOnHidden(e -> {
            Scene scene = alert.getDialogPane().getScene();
            if (scene != null) ThemeManager.unregister(scene);
        });
    }

    private static VBox labeledField(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill: -text-muted;");
        return new VBox(5, lbl, field);
    }

    private static TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("field");
        f.setMaxWidth(Double.MAX_VALUE);
        f.setPrefWidth(300);
        return f;
    }

    private static TextField amtField(double value) {
        TextField f = styledField("0.00");
        f.setText(value > 0 ? String.format("%.2f", value) : "0.00");
        return f;
    }

    private static TextField readonlyField(String value) {
        TextField f = styledField("");
        f.setText(value);
        f.setEditable(false);
        f.setStyle("-fx-background-color: -surface-2; -fx-opacity: 0.85;");
        return f;
    }

    private static double parseDouble(TextField field) {
        try {
            return Double.parseDouble(field.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
