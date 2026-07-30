package com.sevatyres.controller;

import com.sevatyres.App;
import com.sevatyres.viewmodel.ThemeManager;
import com.sevatyres.viewmodel.UiUtil;
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
    @FXML private Button navHome, navTransactions, navAccounts, navCredit, navInventory, navReports, navAlerts, navCompany;
    @FXML private Button themeToggleBtn;
    @FXML private Button refreshBtn;

    private Button currentNav;
    private String currentPage = "home";
    private Integer openCustomerId = null;

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

    /** Reloads the current page (or open customer detail) so DB changes appear immediately. */
    @FXML private void onRefresh() {
        if (openCustomerId != null) {
            openCustomer(openCustomerId);
        } else {
            loadPage(currentPage);
        }
        UiUtil.toast(App.getRoot(), "Refreshed");
    }

    private void loadPage(String name) {
        try {
            currentPage = name;
            openCustomerId = null;
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + name + ".fxml"));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load page: " + name, e);
        }
    }

    @FXML private void toggleMenu() { sideMenu.setVisible(!sideMenu.isVisible()); }

    @FXML private void goHome()          { setActiveNav(navHome);          loadPage("home"); }
    @FXML private void goTransactions()  { setActiveNav(navTransactions);  loadPage("transactions"); }
    @FXML private void goAccounts()      { setActiveNav(navAccounts);      loadPage("accounts"); }
    @FXML private void goCredit()        { setActiveNav(navCredit);        loadPage("credit"); }
    @FXML private void goInventory()     { setActiveNav(navInventory);     loadPage("inventory"); }
    @FXML private void goReports()       { setActiveNav(navReports);       loadPage("reports"); }
    @FXML private void goAlerts()        { setActiveNav(navAlerts);        loadPage("alerts"); }
    @FXML private void goCompany()       { setActiveNav(navCompany);       loadPage("company"); }

    private void setActiveNav(Button nav) {
        if (currentNav != null) currentNav.getStyleClass().remove("menu-active");
        currentNav = nav;
        if (nav != null) nav.getStyleClass().add("menu-active");
    }

    public void navigate(String pageName) {
        Button target = switch (pageName) {
            case "home"         -> navHome;
            case "transactions" -> navTransactions;
            case "accounts"     -> navAccounts;
            case "credit"       -> navCredit;
            case "inventory"    -> navInventory;
            case "reports"      -> navReports;
            case "alerts"       -> navAlerts;
            case "company"      -> navCompany;
            default             -> null;
        };
        setActiveNav(target);
        loadPage(pageName);
    }

    public void openCustomer(int customerId) {
        setActiveNav(navAccounts);
        try {
            currentPage = "customer_detail";
            openCustomerId = customerId;
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/customer_detail.fxml"));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
            Object ctrl = loader.getController();
            if (ctrl instanceof CustomerDetailController cdc) {
                cdc.loadCustomer(customerId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to open customer detail", e);
        }
    }

    public StackPane getContentHost() { return contentHost; }
}
