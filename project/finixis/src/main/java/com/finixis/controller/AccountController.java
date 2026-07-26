package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.User;
import com.finixis.viewmodel.ThemeManager;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class AccountController implements Initializable, PageController {

    @FXML private Label avatar, nameLabel, roleLabel;
    @FXML private TextField emailField, phoneField;
    @FXML private Button themeBtn;

    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getMockData().getUsers() == null || App.getMockData().getUsers().isEmpty()
                ? null
                : App.getMockData().getUsers().get(0);

        if (currentUser != null) {
            avatar.setText(initials(currentUser.getName()));
            nameLabel.setText(currentUser.getName());
            roleLabel.setText("Finixis Staff");  // generic label — no roles in MVP
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
        }

        // Sync button label with actual current theme
        themeBtn.setText(ThemeManager.isDark() ? "Switch to Light Mode" : "Switch to Dark Mode");
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        return (p.length > 1
                ? "" + p[0].charAt(0) + p[p.length - 1].charAt(0)
                : "" + p[0].charAt(0)).toUpperCase();
    }

    @FXML private void onSave() {
        if (currentUser != null) {
            currentUser.setEmail(emailField.getText());
            currentUser.setPhone(phoneField.getText());
        }
        UiUtil.toast(App.getRoot(), "Profile saved (in-memory only)");
    }

    @FXML private void onCancel() {
        if (currentUser != null) {
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
        }
    }

    @FXML private void onToggleTheme() {
        ThemeManager.toggle();
        themeBtn.setText(ThemeManager.isDark() ? "Switch to Light Mode" : "Switch to Dark Mode");
        UiUtil.toast(App.getRoot(), ThemeManager.isDark() ? "Dark mode on" : "Light mode on");
    }
}
