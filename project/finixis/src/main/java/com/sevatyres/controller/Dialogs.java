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

    // â”€â”€â”€ Generic dialogs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Add Customer dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    public static void showEditCustomer(Customer customer, Consumer<Customer> onSaved) {
        Stage stage = buildDialogStage("Edit Customer");

        VBox content = contentVBox();
        Label title = dialogTitle("Edit Customer");
        Label sub   = dialogSub("Update details for \"" + customer.getName() + "\".");

        TextField nameField  = styledField("Full name");
        nameField.setText(nvl(customer.getName()));
        TextField phoneField = styledField("Phone number");
        phoneField.setText(nvl(customer.getPhone()));
        TextField emailField = styledField("Email address");
        emailField.setText(nvl(customer.getEmail()));
        TextField addrField  = styledField("Address");
        addrField.setText(nvl(customer.getAddress()));

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Phone", phoneField),
                labeledField("Email", emailField),
                labeledField("Address", addrField));

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
            customer.setName(name);
            customer.setPhone(phoneField.getText().trim());
            customer.setEmail(emailField.getText().trim());
            customer.setAddress(addrField.getText().trim());
            Customer updated = AppServices.customers().updateCustomer(customer);
            stage.close();
            if (onSaved != null) onSaved.accept(updated);
        });

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn));
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    // â”€â”€â”€ Add Debit dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showAddDebit(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Add Debit â€” " + customer.getName());

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

    // â”€â”€â”€ Record Payment dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showRecordPayment(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Record Payment â€” " + customer.getName(), 640, 820);
        VBox content = contentVBox();
        Label title = dialogTitle("Record Payment");
        Label sub   = dialogSub("Customer: " + customer.getName() + "  Â·  Select items and enter payment by method.");

        VBox itemsBox = new VBox(10);
        List<InventoryItem> inventory = AppServices.inventory().getAll();

        Label totalAmountLabel = new Label("\u20b90.00");
        totalAmountLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: -primary-600;");
        Label remainingLabel = new Label("\u20b90.00");
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px;");
        Label err = errLabel();

        TextField phonePeField    = styledField("0.00");
        TextField acTransferField = styledField("0.00");
        TextField cardSwipeField  = styledField("0.00");
        TextField bajajField      = styledField("0.00");
        TextField cashField       = styledField("0.00");
        TextField chequeField     = styledField("0.00");

        List<ComboBox<InventoryItem>> itemCombos = new ArrayList<>();
        List<TextField> qtyFields = new ArrayList<>();

        Runnable[] updateTotalsRef = new Runnable[1];
        updateTotalsRef[0] = () -> {
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
            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            totalAmountLabel.setText(UiUtil.money(total));
            double rem = total - paid;
            remainingLabel.setText(UiUtil.money(Math.max(0, rem)));
            remainingLabel.setStyle(rem > 0.009
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };

        Runnable[] addRowRef = new Runnable[1];
        addRowRef[0] = () -> {
            ComboBox<InventoryItem> combo = new ComboBox<>();
            combo.getItems().addAll(inventory);
            combo.setPromptText("Select item\u2026");
            combo.setPrefWidth(240);
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

            combo.valueProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotalsRef[0].run(); });
            qtyField.textProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotalsRef[0].run(); });

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
                    updateTotalsRef[0].run();
                }
            });
        };
        addRowRef[0].run();

        Button addMoreBtn = new Button("+ Add another item");
        addMoreBtn.getStyleClass().addAll("btn", "btn-ghost");
        addMoreBtn.setOnAction(ev -> { addRowRef[0].run(); updateTotalsRef[0].run(); });

        for (TextField tf : new TextField[]{phonePeField, acTransferField, cardSwipeField,
                bajajField, cashField, chequeField}) {
            tf.textProperty().addListener((o, a, b) -> updateTotalsRef[0].run());
        }

        Label itemsLabel = new Label("ITEMS");
        itemsLabel.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");
        Label payHeader = new Label("PAYMENT METHODS");
        payHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");
        VBox payForm = new VBox(10, payHeader,
                labeledField("PhonePe (UPI) \u20b9", phonePeField),
                labeledField("Account Transfer \u20b9", acTransferField),
                labeledField("Card Swipe \u20b9", cardSwipeField),
                labeledField("Bajaj Finance \u20b9", bajajField),
                labeledField("Cash \u20b9", cashField),
                labeledField("Cheque \u20b9", chequeField));

        HBox totalRow = new HBox(12, new Label("Total Amount:"), totalAmountLabel);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");
        HBox remainingRow = new HBox(12, new Label("Remaining Amount:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                itemsLabel, itemsBox, addMoreBtn,
                new Separator(), payForm, new Separator(),
                totalRow, remainingRow, err);

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Confirm Payment");
        confirmBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            boolean hasItem = itemCombos.stream().anyMatch(c -> c.getValue() != null);
            if (!hasItem) { err.setText("Please select at least one item."); return; }

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

            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            if (paid < 0) { err.setText("Please enter valid payment amounts."); return; }

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

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn), 640);
    }

    // â”€â”€â”€ New Sale Transaction dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

public static void showNewSaleTransaction(Consumer<SaleTransaction> onSaved) {
        Stage stage = buildDialogStage("New Transaction", 820, 980);
        VBox content = contentVBox();
        content.setPrefWidth(860);
        Label title = dialogTitle("New Transaction");
        Label sub   = dialogSub("Scan a barcode into the product field, or pick from inventory. Quantity starts at 0. Brand comes from each item.");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("date-picker");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField billNoField = styledField("Bill number (optional — auto-generated if blank)");

        List<InventoryItem> inventoryItems = AppServices.inventory().getAll();

        TextField phonePeField    = styledField("0.00");
        TextField acTransferField = styledField("0.00");
        TextField cardSwipeField  = styledField("0.00");
        TextField bajajField      = styledField("0.00");
        TextField cashField       = styledField("0.00");
        TextField chequeField     = styledField("0.00");

        Label itemTotalLabel = new Label("\u20b90.00");
        itemTotalLabel.setStyle("-fx-font-weight:700; -fx-font-size:18px; -fx-text-fill: -primary-600;");
        Label paidTotalLabel = new Label("\u20b90.00");
        paidTotalLabel.setStyle("-fx-font-weight:600; -fx-font-size:15px;");
        Label remainingLabel = new Label("\u20b90.00");
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #38a169;");

        class ItemRow {
            final TextField productField = styledField("Product name or scan barcode\u2026");
            final ComboBox<InventoryItem> combo = new ComboBox<>();
            final TextField brandField = styledField("Brand (optional)");
            final TextField qtyField = styledField("0");
            final TextField unitPriceField = styledField("0.00");
            final Label lineTotalLbl = new Label("\u20b90.00");
            final Label stockLbl = new Label();
            final Button removeBtn = new Button("\u2715");
            InventoryItem selected = null;
            VBox rowNode;
        }
        List<ItemRow> rows = new ArrayList<>();
        VBox itemsBox = new VBox(10);

        Runnable[] updateTotalsRef = new Runnable[1];
        updateTotalsRef[0] = () -> {
            double total = 0;
            for (ItemRow r : rows) {
                double unit = parseDouble(r.unitPriceField);
                int qty = 0;
                try { qty = Math.max(0, Integer.parseInt(r.qtyField.getText().trim())); }
                catch (NumberFormatException ignored) {}
                double line = unit * qty;
                r.lineTotalLbl.setText(UiUtil.money(line));
                r.lineTotalLbl.setStyle("-fx-font-weight:700;");
                total += line;

                if (r.selected != null) {
                    InventoryItem fresh = AppServices.inventory().getById(r.selected.getId()).orElse(r.selected);
                    if (qty > fresh.getQuantity()) {
                        r.stockLbl.setText("Out of stock");
                        r.stockLbl.setStyle("-fx-font-size:11px; -fx-text-fill: #e53e3e; -fx-font-weight:700;");
                    } else {
                        r.stockLbl.setText("Available (" + fresh.getQuantity() + ")");
                        r.stockLbl.setStyle("-fx-font-size:11px; -fx-text-fill: #38a169;");
                    }
                } else {
                    r.stockLbl.setText("");
                }
            }
            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            double remaining = Math.max(0, total - paid);
            itemTotalLabel.setText(UiUtil.money(total));
            paidTotalLabel.setText(UiUtil.money(paid));
            remainingLabel.setText(UiUtil.money(remaining));
            remainingLabel.setStyle(remaining > 0.009
                    ? "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #e53e3e;"
                    : "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #38a169;");
        };

        Runnable[] addRowRef = new Runnable[1];
        addRowRef[0] = () -> {
            ItemRow r = new ItemRow();
            r.qtyField.setText("0");
            r.qtyField.setPrefWidth(70);
            r.unitPriceField.setText("0.00");
            r.unitPriceField.setPrefWidth(90);
            r.combo.getItems().addAll(inventoryItems);
            r.combo.setPromptText("Select from inventory\u2026");
            r.combo.getStyleClass().add("combo");
            r.combo.setMaxWidth(Double.MAX_VALUE);
            r.combo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(InventoryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    String stock = item.getQuantity() == 0 ? "Out of Stock" : "Stock: " + item.getQuantity();
                    String brand = (item.getBrand() != null && !item.getBrand().isBlank())
                            ? " · " + item.getBrand() : "";
                    setText(item.getName() + brand + "  [" + stock + "]");
                }
            });
            r.combo.setButtonCell(r.combo.getCellFactory().call(null));
            r.brandField.setPrefWidth(160);

            Runnable applyItem = () -> {
                if (r.selected == null) return;
                r.productField.setText(r.selected.getName());
                r.unitPriceField.setText(String.format("%.2f", r.selected.getUnitPrice()));
                r.brandField.setText(r.selected.getBrand() != null ? r.selected.getBrand() : "");
                updateTotalsRef[0].run();
            };

            r.combo.setOnAction(e -> {
                InventoryItem item = r.combo.getValue();
                if (item != null) { r.selected = item; applyItem.run(); }
            });

            // Barcode scan / Enter: look up by barcode, then by exact name
            r.productField.setOnAction(e -> {
                String code = r.productField.getText().trim();
                if (code.isEmpty()) return;
                AppServices.inventory().getByBarcode(code).ifPresentOrElse(item -> {
                    r.selected = item;
                    r.combo.setValue(item);
                    applyItem.run();
                }, () -> inventoryItems.stream()
                        .filter(it -> it.getName().equalsIgnoreCase(code))
                        .findFirst()
                        .ifPresent(item -> {
                            r.selected = item;
                            r.combo.setValue(item);
                            applyItem.run();
                        }));
            });

            r.qtyField.textProperty().addListener((o, a, b) -> updateTotalsRef[0].run());
            r.unitPriceField.textProperty().addListener((o, a, b) -> updateTotalsRef[0].run());

            r.removeBtn.getStyleClass().addAll("btn", "btn-secondary");
            r.removeBtn.setStyle("-fx-padding: 4 8;");

            Label qtyLbl = new Label("Qty:");
            Label upLbl  = new Label("Unit \u20b9:");
            Label ltLbl  = new Label("Line:");
            Label brandLbl = new Label("Brand:");
            upLbl.setStyle("-fx-font-size:11px; -fx-text-fill: -text-muted;");
            ltLbl.setStyle("-fx-font-size:11px; -fx-text-fill: -text-muted;");
            brandLbl.setStyle("-fx-font-size:11px; -fx-text-fill: -text-muted;");

            HBox productRow = new HBox(10, r.productField, r.combo);
            HBox.setHgrow(r.productField, Priority.ALWAYS);
            HBox.setHgrow(r.combo, Priority.ALWAYS);
            productRow.setAlignment(Pos.CENTER_LEFT);

            HBox detailRow = new HBox(12, brandLbl, r.brandField, qtyLbl, r.qtyField,
                    upLbl, r.unitPriceField, ltLbl, r.lineTotalLbl, r.stockLbl, r.removeBtn);
            detailRow.setAlignment(Pos.CENTER_LEFT);

            r.rowNode = new VBox(8, productRow, detailRow);
            r.rowNode.setStyle("-fx-background-color: -surface-2; -fx-padding: 12; -fx-background-radius: 8;");

            rows.add(r);
            itemsBox.getChildren().add(r.rowNode);

            r.removeBtn.setOnAction(ev -> {
                if (rows.size() <= 1) return;
                rows.remove(r);
                itemsBox.getChildren().remove(r.rowNode);
                updateTotalsRef[0].run();
            });
        };
        addRowRef[0].run();

        Button addMoreBtn = new Button("+ Add another item");
        addMoreBtn.getStyleClass().addAll("btn", "btn-ghost");
        addMoreBtn.setOnAction(ev -> addRowRef[0].run());

        for (TextField tf : new TextField[]{phonePeField, acTransferField, cardSwipeField,
                bajajField, cashField, chequeField}) {
            tf.textProperty().addListener((o, a, b) -> updateTotalsRef[0].run());
        }

        Label custHeader = new Label("CUSTOMER DETAILS (required when amount is on credit)");
        custHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        List<Customer> existingCustomers = AppServices.customers().getAll();
        ComboBox<Customer> existingCustCombo = new ComboBox<>();
        existingCustCombo.getItems().addAll(existingCustomers);
        existingCustCombo.setPromptText("Select existing customer\u2026");
        existingCustCombo.setMaxWidth(Double.MAX_VALUE);
        existingCustCombo.getStyleClass().add("combo");
        existingCustCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null
                        : c.getName() + (c.getPhone() != null ? " \u2014 " + c.getPhone() : ""));
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

        Label itemsHeader = new Label("ITEMS");
        itemsHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");
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
                labeledField("Date", datePicker),
                labeledField("Bill No", billNoField),
                new Separator(),
                itemsHeader, itemsBox, addMoreBtn,
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
            List<SaleTransactionItem> lineItems = new ArrayList<>();
            for (ItemRow r : rows) {
                String name = r.productField.getText().trim();
                if (name.isEmpty() && r.selected == null) continue;
                int qty;
                try { qty = Integer.parseInt(r.qtyField.getText().trim()); }
                catch (NumberFormatException ex) { err.setText("Quantity must be a valid number."); return; }
                if (qty <= 0) {
                    err.setText("Quantity must be greater than zero for \""
                            + (r.selected != null ? r.selected.getName() : name) + "\".");
                    return;
                }
                double unit = parseDouble(r.unitPriceField);
                Integer invId = null;
                if (r.selected != null) {
                    invId = r.selected.getId();
                    name = r.selected.getName();
                    if (unit <= 0) unit = r.selected.getUnitPrice();
                    InventoryItem fresh = AppServices.inventory().getById(invId).orElse(r.selected);
                    if (qty > fresh.getQuantity()) {
                        err.setText("Out of stock: \"" + name + "\" has only "
                                + fresh.getQuantity() + " available.");
                        return;
                    }
                } else if (unit <= 0) {
                    err.setText("Enter a unit price for \"" + name + "\".");
                    return;
                }
                lineItems.add(new SaleTransactionItem(invId, name, qty, unit));
            }
            if (lineItems.isEmpty()) { err.setText("Add at least one item."); return; }

            double paid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            double itemsTotal = lineItems.stream().mapToDouble(SaleTransactionItem::getLineTotal).sum();
            double remaining = Math.max(0, itemsTotal - paid);

            String custName = custNameField.getText().trim();
            if (remaining > 0.009 && custName.isEmpty()) {
                err.setText("Customer name is required when there is a remaining balance (credit).");
                return;
            }

            SaleTransaction tx = new SaleTransaction();
            tx.setBillNo(billNoField.getText().trim());
            tx.setSaleDate(datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now());
            tx.setPhonePe(parseDouble(phonePeField));
            tx.setAccountTransfer(parseDouble(acTransferField));
            tx.setCardSwipe(parseDouble(cardSwipeField));
            tx.setBajajFinance(parseDouble(bajajField));
            tx.setCash(parseDouble(cashField));
            tx.setCheque(parseDouble(chequeField));

            SaleTransactionItem first = lineItems.get(0);
            tx.setParticulars(first.getItemName() + (lineItems.size() > 1
                    ? " (+" + (lineItems.size() - 1) + " more)" : ""));
            tx.setQuantity(first.getQuantity());
            tx.setUnitPrice(first.getUnitPrice());
            tx.setInventoryItemId(first.getInventoryId());
            // Brand from first item row that produced a line item
            String saleBrand = "";
            for (ItemRow r : rows) {
                if (r.productField.getText().trim().isEmpty() && r.selected == null) continue;
                saleBrand = r.brandField.getText().trim();
                break;
            }
            tx.setBrand(saleBrand);

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

            SaleTransaction saved = AppServices.saleTransactions().save(tx, lineItems);
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn), 860);
    }

    // â”€â”€â”€ View Sale Transaction dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    // â”€â”€â”€ View Sale Transaction dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Edit Sale Transaction dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showEditSaleTransaction(SaleTransaction t, Runnable onSaved) {
        Stage stage = buildDialogStage("Edit Transaction â€” Bill #" + t.getBillNo());
        VBox content = contentVBox();
        Label title = dialogTitle("Edit Transaction");
        Label sub   = dialogSub("Record an additional payment against this bill. Existing details are read-only.");

        // Read-only transaction details
        double originalTotal = t.getTotal() > 0 ? t.getTotal() : t.getItemTotal();
        double alreadyPaid   = t.getPaidAmount();
        double creditLeft    = t.getCreditAmount() > 0 ? t.getCreditAmount()
                : Math.max(0, originalTotal - alreadyPaid);

        Label totalLbl     = new Label(UiUtil.money(originalTotal));
        Label paidLbl      = new Label(UiUtil.money(alreadyPaid));
        Label remainingLbl = new Label(UiUtil.money(creditLeft));
        totalLbl.setStyle("-fx-font-weight:700; -fx-font-size:15px; -fx-text-fill: -primary-600;");
        paidLbl.setStyle("-fx-font-weight:700; -fx-font-size:15px;");
        remainingLbl.setStyle("-fx-font-weight:700; -fx-font-size:15px; -fx-text-fill: #e53e3e;");

        // How previous payment was made (text only)
        String prevPayment = t.getPaymentSummary();
        StringBuilder prevDetail = new StringBuilder();
        if (t.getPhonePe() > 0)         prevDetail.append("PhonePe: ").append(UiUtil.money(t.getPhonePe())).append("\n");
        if (t.getAccountTransfer() > 0) prevDetail.append("A/C Transfer: ").append(UiUtil.money(t.getAccountTransfer())).append("\n");
        if (t.getCardSwipe() > 0)       prevDetail.append("Card Swipe: ").append(UiUtil.money(t.getCardSwipe())).append("\n");
        if (t.getBajajFinance() > 0)    prevDetail.append("Bajaj Finance: ").append(UiUtil.money(t.getBajajFinance())).append("\n");
        if (t.getCash() > 0)            prevDetail.append("Cash: ").append(UiUtil.money(t.getCash())).append("\n");
        if (t.getCheque() > 0)          prevDetail.append("Cheque: ").append(UiUtil.money(t.getCheque())).append("\n");
        if (t.getCreditAmount() > 0)    prevDetail.append("On Credit: ").append(UiUtil.money(t.getCreditAmount())).append("\n");
        if (prevDetail.isEmpty()) prevDetail.append(prevPayment);

        Label prevPayLabel = new Label(prevDetail.toString().trim());
        prevPayLabel.setWrapText(true);
        prevPayLabel.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        VBox infoBox = new VBox(10,
                labeledField("Bill No", readonlyField(t.getBillNo() != null ? t.getBillNo() : "\u2014")),
                labeledField("Date", readonlyField(UiUtil.date(t.getSaleDate()))),
                labeledField("Particulars", readonlyField(t.getParticulars() != null ? t.getParticulars() : "\u2014")),
                labeledField("Customer", readonlyField(t.getCustomerName() != null ? t.getCustomerName() : "\u2014")),
                new HBox(24,
                        new VBox(4, new Label("Total Amount") {{ setStyle("-fx-font-size:11px;"); }}, totalLbl),
                        new VBox(4, new Label("Already Paid") {{ setStyle("-fx-font-size:11px;"); }}, paidLbl),
                        new VBox(4, new Label("Remaining (Credit)") {{ setStyle("-fx-font-size:11px;"); }}, remainingLbl)),
                labeledField("Previous payment methods", prevPayLabel));

        // New payment fields â€” all start at zero (placeholders)
        TextField phonePeField    = styledField("0.00");
        TextField acTransferField = styledField("0.00");
        TextField cardSwipeField  = styledField("0.00");
        TextField bajajField      = styledField("0.00");
        TextField cashField       = styledField("0.00");
        TextField chequeField     = styledField("0.00");

        Label newRemainingLabel = new Label(UiUtil.money(creditLeft));
        newRemainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #e53e3e;");

        Label overErr = new Label();
        overErr.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        final double creditCap = creditLeft;
        Runnable updateNew = () -> {
            // Only sum NEWLY entered payment amounts â€” do NOT include previously paid
            double newPaid = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            if (newPaid > creditCap + 0.009) {
                overErr.setText("Amount entered is greater than credit ("
                        + UiUtil.money(creditCap) + ").");
            } else {
                overErr.setText("");
            }
            double newRem = Math.max(0, creditCap - newPaid);
            newRemainingLabel.setText(UiUtil.money(newRem));
            newRemainingLabel.setStyle(newRem > 0.009
                    ? "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #e53e3e;"
                    : "-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: #38a169;");
        };
        for (TextField tf : new TextField[]{phonePeField, acTransferField, cardSwipeField,
                bajajField, cashField, chequeField}) {
            tf.textProperty().addListener((o, a, b) -> updateNew.run());
        }

        Label payHeader = new Label("NEW PAYMENT (enter amounts to apply against remaining credit)");
        payHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        Label err = errLabel();

        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                infoBox, new Separator(),
                payHeader,
                labeledField("PhonePe \u20b9", phonePeField),
                labeledField("A/C Transfer \u20b9", acTransferField),
                labeledField("Card Swipe \u20b9", cardSwipeField),
                labeledField("Bajaj Finance \u20b9", bajajField),
                labeledField("Cash \u20b9", cashField),
                labeledField("Cheque \u20b9", chequeField),
                overErr,
                new HBox(12, new Label("Remaining after this payment:"), newRemainingLabel) {{
                    setAlignment(Pos.CENTER_LEFT);
                    setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");
                }},
                err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Apply Payment");
        saveBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            if (!overErr.getText().isEmpty()) { err.setText(overErr.getText()); return; }

            double newPhonePe = parseDouble(phonePeField);
            double newAc      = parseDouble(acTransferField);
            double newCard    = parseDouble(cardSwipeField);
            double newBajaj   = parseDouble(bajajField);
            double newCash    = parseDouble(cashField);
            double newCheque  = parseDouble(chequeField);
            double newPaid    = newPhonePe + newAc + newCard + newBajaj + newCash + newCheque;

            if (newPaid <= 0) { err.setText("Enter a payment amount greater than zero."); return; }
            if (newPaid > creditCap + 0.009) {
                err.setText("Amount entered is greater than credit ("
                        + UiUtil.money(creditCap) + ").");
                return;
            }

            // Accumulate onto existing payment methods; reduce credit by newPaid only
            t.setPhonePe(t.getPhonePe() + newPhonePe);
            t.setAccountTransfer(t.getAccountTransfer() + newAc);
            t.setCardSwipe(t.getCardSwipe() + newCard);
            t.setBajajFinance(t.getBajajFinance() + newBajaj);
            t.setCash(t.getCash() + newCash);
            t.setCheque(t.getCheque() + newCheque);
            t.setCreditAmount(Math.max(0, creditCap - newPaid));
            // Keep original total; recompute only credit from paid
            t.setTotal(originalTotal);

            AppServices.saleTransactions().update(t);

            // Also update the linked Transaction_Credit so Credits page stays in sync
            if (t.getCustomerId() != null && newPaid > 0) {
                try {
                    AppServices.transactions().getCreditsByCustomer(t.getCustomerId()).stream()
                            .filter(Transaction::isOngoing)
                            .filter(c -> c.getDescription() != null
                                    && c.getDescription().contains("Bill " + t.getBillNo()))
                            .findFirst()
                            .ifPresent(c -> AppServices.transactions()
                                    .recordPartialPayment(c.getId(), newPaid, LocalDate.now()));
                } catch (Exception ex) {
                    System.err.println("[EditSale] Credit sync failed: " + ex.getMessage());
                }
            }

            stage.close();
            if (onSaved != null) onSaved.run();
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn));
    }

    // â”€â”€â”€ Generate Transaction Report dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    // â”€â”€â”€ Generate Transaction Report dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Add Inventory Item dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showAddItem(Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Add Inventory Item");
        VBox content = contentVBox();
        Label title = dialogTitle("Add Item");
        Label sub   = dialogSub("Enter details. Attach a barcode so items can be scanned in transactions.");

        TextField nameField    = styledField("Item name");
        TextField brandField   = styledField("Brand (optional)");
        TextField qtyField     = styledField("0");
        TextField priceField   = styledField("0.00");
        TextField barcodeField = styledField("Barcode (scan or type, optional)");
        Label barcodeHint = new Label("Attach a USB barcode scanner to auto-fill this field.");
        barcodeHint.setStyle("-fx-font-size:11px; -fx-text-fill: -neutral-400;");

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Brand", brandField),
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
            item.setBrand(brandField.getText().trim());
            item.setQuantity(qty);
            item.setUnitPrice(price);
            String bc = barcodeField.getText().trim();
            if (!bc.isEmpty()) item.setBarcode(bc);
            try {
                InventoryItem saved = AppServices.inventory().addItem(item);
                stage.close();
                if (onSaved != null) onSaved.accept(saved);
            } catch (IllegalArgumentException ex) {
                err.setText(ex.getMessage());
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                String msg = cause.getMessage() != null ? cause.getMessage() : "Could not save item.";
                if (msg.contains("uq_inventory_name") || msg.contains("already exists")) {
                    err.setText("An item named \"" + name + "\" already exists. Use a different name.");
                } else {
                    err.setText("Could not save item: " + msg);
                }
            }
        });

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn));
    }

    // â”€â”€â”€ Edit Inventory Item dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showEditItem(InventoryItem item, Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Edit Item â€” " + item.getName());
        VBox content = contentVBox();
        Label title = dialogTitle("Edit Item");
        Label sub   = dialogSub("Update the details for \"" + item.getName() + "\".");

        TextField nameField    = styledField("Item name");  nameField.setText(item.getName());
        TextField brandField   = styledField("Brand (optional)");
        if (item.getBrand() != null) brandField.setText(item.getBrand());
        TextField qtyField     = styledField("0");          qtyField.setText(String.valueOf(item.getQuantity()));
        TextField priceField   = styledField("0.00");       priceField.setText(String.format("%.2f", item.getUnitPrice()));
        TextField barcodeField = styledField("Barcode (scan or type, optional)");
        if (item.getBarcode() != null) barcodeField.setText(item.getBarcode());

        VBox form = new VBox(14,
                labeledField("Name *", nameField),
                labeledField("Brand", brandField),
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
            item.setBrand(brandField.getText().trim());
            item.setQuantity(qty);
            item.setUnitPrice(price);
            String bc = barcodeField.getText().trim();
            item.setBarcode(bc.isEmpty() ? null : bc);
            try {
                InventoryItem updated = AppServices.inventory().updateItem(item);
                stage.close();
                if (onSaved != null) onSaved.accept(updated);
            } catch (IllegalArgumentException ex) {
                err.setText(ex.getMessage());
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                String msg = cause.getMessage() != null ? cause.getMessage() : "Could not save item.";
                if (msg.contains("uq_inventory_name") || msg.contains("already exists")) {
                    err.setText("An item named \"" + name + "\" already exists. Use a different name.");
                } else {
                    err.setText("Could not save item: " + msg);
                }
            }
        });

        presentDialog(stage, content, buttonRow(cancelBtn, confirmBtn));
    }

    // â”€â”€â”€ Stock Adjustment dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Add Credit Info dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Edit Transaction dialog (credit / remaining payment) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void showEditTransaction(Transaction t, Runnable onSave) {
        Stage stage = buildDialogStage("Edit Credit â€” Pay Remaining", 640, 800);
        VBox content = contentVBox();
        Label title = dialogTitle("Pay Remaining Credit");
        Label sub   = dialogSub("Same transaction is updated each time you pay. Remaining balance and last date change.");

        VBox infoForm = new VBox(12,
                labeledField("Description / Items", readonlyField(
                        t.getDescription() != null ? t.getDescription() : t.getType().name())),
                labeledField("Original Total (\u20b9)", readonlyField(UiUtil.money(t.getAmount()))),
                labeledField("Already Paid (\u20b9)", readonlyField(UiUtil.money(t.getPaidAmount()))),
                labeledField("Last Transaction Date", readonlyField(UiUtil.date(t.getDate()))),
                labeledField("Remaining Credit (\u20b9)", readonlyField(UiUtil.money(t.getBalance()))));

        TextField phonePeField    = styledField("0.00");
        TextField acTransferField = styledField("0.00");
        TextField cardSwipeField  = styledField("0.00");
        TextField bajajField      = styledField("0.00");
        TextField cashField       = styledField("0.00");
        TextField chequeField     = styledField("0.00");

        DatePicker paymentDatePicker = new DatePicker(LocalDate.now());
        paymentDatePicker.getStyleClass().add("date-picker");
        paymentDatePicker.setMaxWidth(Double.MAX_VALUE);

        Label remainingLabel = new Label(UiUtil.money(t.getBalance()));
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;");
        Label overErr = new Label();
        overErr.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");
        final double creditCap = t.getBalance();

        Runnable updateRemaining = () -> {
            double payAmt = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            if (payAmt > creditCap + 0.009) {
                overErr.setText("Amount entered is greater than remaining credit ("
                        + UiUtil.money(creditCap) + ").");
            } else {
                overErr.setText("");
            }
            double newBal = Math.max(0, creditCap - payAmt);
            remainingLabel.setText(UiUtil.money(newBal));
            remainingLabel.setStyle(newBal > 0.009
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };
        for (TextField tf : new TextField[]{phonePeField, acTransferField, cardSwipeField,
                bajajField, cashField, chequeField}) {
            tf.textProperty().addListener((o, a, b) -> updateRemaining.run());
        }

        Label payHeader = new Label("NEW PAYMENT METHODS (enter amounts to apply against remaining credit)");
        payHeader.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");
        VBox payForm = new VBox(10, payHeader,
                labeledField("PhonePe \u20b9", phonePeField),
                labeledField("A/C Transfer \u20b9", acTransferField),
                labeledField("Card Swipe \u20b9", cardSwipeField),
                labeledField("Bajaj Finance \u20b9", bajajField),
                labeledField("Cash \u20b9", cashField),
                labeledField("Cheque \u20b9", chequeField),
                labeledField("Payment Date", paymentDatePicker));

        HBox remainingRow = new HBox(12, new Label("Remaining after this payment:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        Label err = errLabel();
        content.getChildren().addAll(
                new VBox(4, title, sub), new Separator(), infoForm, new Separator(),
                payForm, overErr, remainingRow, err);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Apply Payment");
        saveBtn.getStyleClass().add("btn");

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            if (!overErr.getText().isEmpty()) { err.setText(overErr.getText()); return; }
            double payAmt = parseDouble(phonePeField) + parseDouble(acTransferField)
                    + parseDouble(cardSwipeField) + parseDouble(bajajField)
                    + parseDouble(cashField) + parseDouble(chequeField);
            if (payAmt <= 0) { err.setText("Enter a payment amount greater than zero."); return; }
            if (payAmt > creditCap + 0.009) {
                err.setText("Amount entered is greater than remaining credit.");
                return;
            }
            LocalDate payDate = paymentDatePicker.getValue() != null
                    ? paymentDatePicker.getValue() : LocalDate.now();
            // Same credit transaction: accumulate paid, update remaining + last date
            AppServices.transactions().recordPartialPayment(t.getId(), payAmt, payDate);
            t.setBalance(Math.max(0, t.getBalance() - payAmt));
            t.setPaidAmount(t.getPaidAmount() + payAmt);
            t.setDate(payDate);
            t.setOngoing(t.getBalance() > 0);
            stage.close();
            if (onSave != null) onSave.run();
        });

        presentDialog(stage, content, buttonRow(cancelBtn, saveBtn), 640);
    }

    // â”€â”€â”€ View Transaction dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€ Layout helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static void presentDialog(Stage stage, VBox contentVBox, HBox buttonRow) {
        presentDialog(stage, contentVBox, buttonRow, 540);
    }

    private static void presentDialog(Stage stage, VBox contentVBox, HBox buttonRow, double contentWidth) {
        contentVBox.setPrefWidth(contentWidth);
        ScrollPane scroll = new ScrollPane(contentVBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(640);
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
        return buildDialogStage(title, 540, 740);
    }

    private static Stage buildDialogStage(String title, double minWidth, double maxWidth) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(title);
        stage.setMinWidth(minWidth);
        stage.setMaxWidth(maxWidth);
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
