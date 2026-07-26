package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.model.User;
import com.finixis.viewmodel.MockDataService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * User Management page — kept for completeness but not in the navigation for this MVP.
 * No role restrictions apply.
 */
public class UsersController implements Initializable, PageController {

    @FXML private Button addUserBtn;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> nameCol, emailCol, phoneCol;
    @FXML private TableColumn<User, Role> roleCol;
    @FXML private TableColumn<User, Boolean> statusCol;
    @FXML private TableColumn<User, User> actionCol;

    private MockDataService data;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(r.getDisplay());
                chip.getStyleClass().addAll("chip", switch (r) {
                    case ADMIN    -> "chip-error";
                    case MANAGER  -> "chip-primary";
                    case EMPLOYEE -> "chip-success";
                });
                setGraphic(chip);
                setText(null);
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(a ? "Active" : "Inactive");
                chip.getStyleClass().addAll("chip", a ? "chip-success" : "chip-warning");
                setGraphic(chip);
                setText(null);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            {
                editBtn.getStyleClass().add("icon-btn");
                editBtn.setGraphic(new FontIcon("fas-pencil-alt"));
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Dialogs.info("Edit User", "Edit dialog for " + u.getName() + " (UI-only prototype).");
                });
            }
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setGraphic(empty || u == null ? null : editBtn);
            }
        });

        table.getItems().setAll(data.getUsers());
    }

    @FXML private void onAddUser() {
        Dialogs.info("Add New User", "This would open the Add/Edit User dialog (UI-only prototype).");
    }
}
