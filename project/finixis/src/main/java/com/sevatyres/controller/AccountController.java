package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.model.User;
import com.sevatyres.viewmodel.ThemeManager;
import com.sevatyres.viewmodel.UiUtil;
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
            roleLabel.setText("Seva Tyres Staff");
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
        }

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
        UiUtil.toast(App.getRoot(), "Profile saved");
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
