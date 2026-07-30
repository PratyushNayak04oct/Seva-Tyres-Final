package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.CompanyInfo;
import com.sevatyres.model.CompanyMember;
import com.sevatyres.service.AppServices;
import com.sevatyres.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CompanyController implements Initializable, PageController {

    @FXML private TextField companyNameField, ownerNameField, emailField, phoneField, dbtPhoneField;
    @FXML private TextField gstinField, addressField, cityField, stateField, pincodeField;
    @FXML private TextField bankNameField, bankAccountField, bankIfscField, upiIdField;
    @FXML private TextField supportEmailField, supportPhoneField;
    @FXML private TextArea aboutArea;
    @FXML private TableView<CompanyMember> memberTable;
    @FXML private TableColumn<CompanyMember, String> memberNameCol, memberRoleCol, memberEmailCol, memberPhoneCol;
    @FXML private TableColumn<CompanyMember, CompanyMember> memberActionCol;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        memberNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        memberRoleCol.setCellValueFactory(new PropertyValueFactory<>("roleTitle"));
        memberEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        memberPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        memberActionCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        memberActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            {
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                delBtn.getStyleClass().addAll("btn", "btn-danger");
                editBtn.setOnAction(e -> {
                    CompanyMember m = getItem();
                    if (m != null) editMember(m);
                });
                delBtn.setOnAction(e -> {
                    CompanyMember m = getItem();
                    if (m == null) return;
                    if (!Dialogs.confirm("Delete Member", "Remove " + m.getName() + "?",
                            "This cannot be undone.")) return;
                    AppServices.company().deleteMember(m.getId());
                    reloadMembers();
                });
            }
            @Override protected void updateItem(CompanyMember item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : new HBox(8, editBtn, delBtn));
            }
        });

        loadCompany();
        reloadMembers();
    }

    private void loadCompany() {
        CompanyInfo c = AppServices.company().getCompany();
        companyNameField.setText(n(c.getCompanyName()));
        ownerNameField.setText(n(c.getOwnerName()));
        emailField.setText(n(c.getEmail()));
        phoneField.setText(n(c.getPhone()));
        dbtPhoneField.setText(n(c.getDbtPhone()));
        gstinField.setText(n(c.getGstin()));
        addressField.setText(n(c.getAddress()));
        cityField.setText(n(c.getCity()));
        stateField.setText(n(c.getState()));
        pincodeField.setText(n(c.getPincode()));
        bankNameField.setText(n(c.getBankName()));
        bankAccountField.setText(n(c.getBankAccount()));
        bankIfscField.setText(n(c.getBankIfsc()));
        upiIdField.setText(n(c.getUpiId()));
        supportEmailField.setText(n(c.getSupportEmail()));
        supportPhoneField.setText(n(c.getSupportPhone()));
        aboutArea.setText(n(c.getAboutText()));
    }

    private void reloadMembers() {
        memberTable.getItems().setAll(AppServices.company().getMembers());
    }

    @FXML private void onSave() {
        try {
            CompanyInfo c = AppServices.company().getCompany();
            c.setCompanyName(companyNameField.getText().trim());
            c.setOwnerName(ownerNameField.getText().trim());
            c.setEmail(emailField.getText().trim());
            c.setPhone(phoneField.getText().trim());
            c.setDbtPhone(dbtPhoneField.getText().trim());
            c.setGstin(gstinField.getText().trim());
            c.setAddress(addressField.getText().trim());
            c.setCity(cityField.getText().trim());
            c.setState(stateField.getText().trim());
            c.setPincode(pincodeField.getText().trim());
            c.setBankName(bankNameField.getText().trim());
            c.setBankAccount(bankAccountField.getText().trim());
            c.setBankIfsc(bankIfscField.getText().trim());
            c.setUpiId(upiIdField.getText().trim());
            c.setSupportEmail(supportEmailField.getText().trim());
            c.setSupportPhone(supportPhoneField.getText().trim());
            c.setAboutText(aboutArea.getText());
            AppServices.company().saveCompany(c);
            statusLabel.setText("Saved at " + java.time.LocalTime.now().withNano(0));
            UiUtil.toast(App.getRoot(), "Company information saved");
        } catch (Exception ex) {
            Dialogs.info("Save Failed", ex.getMessage());
        }
    }

    @FXML private void onAddMember() {
        editMember(null);
    }

    private void editMember(CompanyMember existing) {
        boolean isNew = existing == null;
        TextInputDialog nameDlg = new TextInputDialog(isNew ? "" : n(existing.getName()));
        nameDlg.setTitle(isNew ? "Add Member" : "Edit Member");
        nameDlg.setHeaderText(isNew ? "New company member" : "Edit member");
        nameDlg.setContentText("Full name:");
        nameDlg.showAndWait().ifPresent(name -> {
            if (name == null || name.isBlank()) return;
            CompanyMember m = isNew ? new CompanyMember() : existing;
            m.setName(name.trim());

            TextInputDialog roleDlg = new TextInputDialog(isNew ? "" : n(existing.getRoleTitle()));
            roleDlg.setHeaderText("Role / title");
            roleDlg.setContentText("Role:");
            roleDlg.showAndWait().ifPresent(m::setRoleTitle);

            TextInputDialog emailDlg = new TextInputDialog(isNew ? "" : n(existing.getEmail()));
            emailDlg.setHeaderText("Email");
            emailDlg.setContentText("Email:");
            emailDlg.showAndWait().ifPresent(m::setEmail);

            TextInputDialog phoneDlg = new TextInputDialog(isNew ? "" : n(existing.getPhone()));
            phoneDlg.setHeaderText("Phone");
            phoneDlg.setContentText("Phone:");
            phoneDlg.showAndWait().ifPresent(m::setPhone);

            if (isNew) AppServices.company().addMember(m);
            else AppServices.company().updateMember(m);
            reloadMembers();
            UiUtil.toast(App.getRoot(), isNew ? "Member added" : "Member updated");
        });
    }

    private static String n(String s) { return s == null ? "" : s; }
}
