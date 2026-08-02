package com.sevatyres;

import com.sevatyres.controller.ShellController;
import com.sevatyres.service.AppServices;
import com.sevatyres.view.SplashScreen;
import com.sevatyres.viewmodel.MockDataService;
import com.sevatyres.viewmodel.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene primaryScene;
    private static MockDataService mockData;
    private static StackPane root;
    private static ShellController shell;

    @Override
    public void start(Stage stage) {
        SplashScreen splash = new SplashScreen();
        splash.show();

        Thread boot = new Thread(() -> {
            try {
                splash.setStatus("Connecting to database…", 0.12);
                AppServices.init();

                splash.setStatus("Loading shop data…", 0.48);
                MockDataService data = new MockDataService();

                splash.setStatus("Preparing workspace…", 0.72);
                // Tiny pause so the progress animation can read clearly
                Thread.sleep(180);

                Platform.runLater(() -> {
                    try {
                        splash.setStatus("Opening Seva Tyres…", 0.92);
                        openMainStage(stage, data);
                        // Keep splash visible for 2 seconds, then open the main app
                        splash.finish(2000, () -> {
                            stage.show();
                            stage.toFront();
                        });
                    } catch (Exception ex) {
                        splash.fail(ex);
                        ex.printStackTrace();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> splash.fail(ex));
                ex.printStackTrace();
            }
        }, "sevatyres-boot");
        boot.setDaemon(true);
        boot.start();
    }

    private void openMainStage(Stage stage, MockDataService data) throws Exception {
        mockData = data;
        Runtime.getRuntime().addShutdownHook(new Thread(AppServices::shutdown, "db-shutdown"));

        root = new StackPane();
        primaryScene = new Scene(root);

        stage.setTitle("Seva Tyres");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(primaryScene);
        stage.setMaximized(true);
        applyIcons(stage);

        ThemeManager.register(primaryScene);
        ThemeManager.apply(primaryScene);

        navigate("shell");
    }

    private static void applyIcons(Stage stage) {
        try {
            var icon512 = App.class.getResourceAsStream("/android-chrome-512x512.png");
            var icon192 = App.class.getResourceAsStream("/android-chrome-192x192.png");
            if (icon512 != null) stage.getIcons().add(new Image(icon512));
            if (icon192 != null) stage.getIcons().add(new Image(icon192));
        } catch (Exception ignored) {
            // icon is optional
        }
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
