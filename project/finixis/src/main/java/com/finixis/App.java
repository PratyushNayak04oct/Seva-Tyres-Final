package com.finixis;

import com.finixis.controller.ShellController;
import com.finixis.service.AppServices;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene primaryScene;
    private static MockDataService mockData;
    private static StackPane root;
    private static ShellController shell;

    @Override
    public void start(Stage stage) throws Exception {
        // Initialise DB pool + schema + seed data first
        AppServices.init();
        mockData = new MockDataService();
        // Shutdown hook — closes DB pool cleanly on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(AppServices::shutdown, "db-shutdown"));

        root = new StackPane();
        primaryScene = new Scene(root);

        stage.setTitle("Finixis");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(primaryScene);   // attach scene to stage first
        stage.setMaximized(true);

        // Register and apply AFTER scene is attached, so window reference is valid
        ThemeManager.register(primaryScene);
        ThemeManager.apply(primaryScene); // apply directly — no filtering needed

        navigate("shell");
        stage.show();
    }

    public static void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
            Parent view = loader.load();
            root.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load /fxml/" + fxml + ".fxml", e);
        }
    }

    public static Scene getScene() { return primaryScene; }
    public static StackPane getRoot() { return root; }
    public static MockDataService getMockData() { return mockData; }
    public static ShellController getShell() { return shell; }
    public static void setShell(ShellController s) { shell = s; }

    public static void main(String[] args) {
        launch(args);
    }
}
