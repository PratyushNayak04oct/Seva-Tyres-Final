package com.finixis.controller;

import com.finixis.App;
import com.finixis.viewmodel.ThemeManager;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ShellController implements Initializable {

    @FXML private VBox sideMenu;
    @FXML private StackPane contentHost;
    @FXML private Button navHome, navAccounts, navCredit, navInventory,
            navTransactions, navReports;
    @FXML private Button themeToggleBtn;

    private Button currentNav;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        App.setShell(this);
        currentNav = navHome;
        loadPage("home");
        updateThemeBtn();
    }

    private void updateThemeBtn() {
        if (themeToggleBtn == null) return;
        boolean dark = ThemeManager.isDark();
        themeToggleBtn.setText(dark ? "\u2600 Light" : "\uD83C\uDF19 Dark");
    }

    @FXML private void onToggleTheme() {
        ThemeManager.toggle();
        updateThemeBtn();
        UiUtil.toast(App.getRoot(), ThemeManager.isDark() ? "Dark mode on" : "Light mode on");
    }

    private void loadPage(String name) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + name + ".fxml"));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load page: " + name, e);
        }
    }

    @FXML private void toggleMenu() { sideMenu.setVisible(!sideMenu.isVisible()); }

    @FXML private void goAccount()       { setActiveNav(null);             loadPage("account"); }
    @FXML private void goHome()          { setActiveNav(navHome);          loadPage("home"); }
    @FXML private void goAccounts()      { setActiveNav(navAccounts);      loadPage("accounts"); }
    @FXML private void goCredit()        { setActiveNav(navCredit);        loadPage("credit"); }
    @FXML private void goInventory()     { setActiveNav(navInventory);     loadPage("inventory"); }
    @FXML private void goTransactions()  { setActiveNav(navTransactions);  loadPage("transactions"); }
    @FXML private void goReports()       { setActiveNav(navReports);       loadPage("reports"); }

    @FXML private void onSignOut() { Dialogs.signOut(contentHost); }

    private void setActiveNav(Button nav) {
        if (currentNav != null) currentNav.getStyleClass().remove("menu-active");
        currentNav = nav;
        if (nav != null) nav.getStyleClass().add("menu-active");
    }

    public void navigate(String pageName) {
        Button target = switch (pageName) {
            case "home"         -> navHome;
            case "accounts"     -> navAccounts;
            case "credit"       -> navCredit;
            case "inventory"    -> navInventory;
            case "transactions" -> navTransactions;
            case "reports"      -> navReports;
            default             -> null;
        };
        setActiveNav(target);
        loadPage(pageName);
    }

    public void openCustomer(int customerId) {
        setActiveNav(navAccounts);
        loadPage("customer_detail");
        Object ctrl = null;
        try {
            // The controller is set as part of loadPage, re-retrieve it
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/customer_detail.fxml"));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
            ctrl = loader.getController();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open customer detail", e);
        }
        if (ctrl instanceof CustomerDetailController cdc) {
            cdc.loadCustomer(customerId);
        }
    }

    public StackPane getContentHost() { return contentHost; }
}
