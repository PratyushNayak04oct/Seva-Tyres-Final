package com.sevatyres.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Modern branded loading window shown before the main Seva Tyres UI opens.
 */
public final class SplashScreen {

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final ProgressBar bar = new ProgressBar(0);
    private final Label statusLabel = new Label("Starting Seva Tyres…");
    private final Label percentLabel = new Label("0%");
    private final Label errorLabel = new Label();
    private final long shownAt = System.currentTimeMillis();
    private Timeline progressPulse;

    public SplashScreen() {
        StackPane root = new StackPane();
        root.getStyleClass().add("splash-root");
        root.setPrefSize(520, 340);

        // Soft decorative orbs (depth)
        Circle glow = new Circle(160, Color.web("#3b82f6", 0.18));
        glow.setTranslateY(-40);
        glow.setEffect(new GaussianBlur(48));
        Circle glow2 = new Circle(110, Color.web("#0ea5e9", 0.12));
        glow2.setTranslateX(150);
        glow2.setTranslateY(90);
        glow2.setEffect(new GaussianBlur(36));

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(36, 42, 28, 42));
        content.setMaxWidth(440);

        StackPane mark = buildBrandMark();
        Label brand = new Label("SEVA TYRES");
        brand.getStyleClass().add("splash-brand");
        Label tagline = new Label("TYRE  ·  SHOP  ·  MANAGEMENT");
        tagline.getStyleClass().add("splash-tagline");

        VBox titleBox = new VBox(6, brand, tagline);
        titleBox.setAlignment(Pos.CENTER);

        bar.getStyleClass().add("splash-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setProgress(0.02);

        statusLabel.getStyleClass().add("splash-status");
        percentLabel.getStyleClass().add("splash-percent");
        HBox statusRow = new HBox(10, statusLabel, new Region(), percentLabel);
        HBox.setHgrow(statusRow.getChildren().get(1), Priority.ALWAYS);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setMaxWidth(Double.MAX_VALUE);

        errorLabel.getStyleClass().add("splash-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);

        Label footer = new Label("Preparing your workspace");
        footer.getStyleClass().add("splash-footer");

        content.getChildren().addAll(mark, titleBox, bar, statusRow, errorLabel, footer);
        root.getChildren().addAll(glow, glow2, content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        var css = SplashScreen.class.getResource("/css/splash.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.centerOnScreen();
        stage.setResizable(false);

        // Gentle breathing on the progress track while work runs
        progressPulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(900), new KeyValue(bar.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1800), new KeyValue(bar.opacityProperty(), 0.85))
        );
        progressPulse.setCycleCount(Timeline.INDEFINITE);
    }

    private StackPane buildBrandMark() {
        StackPane mark = new StackPane();
        mark.getStyleClass().add("splash-mark");
        mark.setMinSize(96, 96);
        mark.setPrefSize(96, 96);

        Circle ring = new Circle(34);
        ring.getStyleClass().add("splash-orb");

        Arc sweep = new Arc(0, 0, 34, 34, 90, 110);
        sweep.setType(ArcType.OPEN);
        sweep.getStyleClass().add("splash-orb-accent");

        FontIcon tyre = new FontIcon("fas-circle-notch");
        tyre.setIconSize(30);
        tyre.setIconColor(Color.web("#e0f2fe"));

        StackPane iconWrap = new StackPane(ring, sweep, tyre);
        mark.getChildren().add(iconWrap);

        RotateTransition spin = new RotateTransition(Duration.seconds(2.4), sweep);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.play();

        RotateTransition iconSpin = new RotateTransition(Duration.seconds(3.2), tyre);
        iconSpin.setByAngle(-360);
        iconSpin.setCycleCount(RotateTransition.INDEFINITE);
        iconSpin.setInterpolator(Interpolator.LINEAR);
        iconSpin.play();

        return mark;
    }

    public void show() {
        stage.show();
        progressPulse.play();
        // Entrance fade
        stage.getScene().getRoot().setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(380), stage.getScene().getRoot());
        in.setFromValue(0);
        in.setToValue(1);
        in.play();
    }

    public void setStatus(String message, double progress) {
        Runnable r = () -> {
            statusLabel.setText(message);
            double p = Math.max(0, Math.min(1, progress));
            bar.setProgress(p);
            percentLabel.setText(Math.round(p * 100) + "%");
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    public void fail(Throwable error) {
        Runnable r = () -> {
            progressPulse.stop();
            bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            statusLabel.setText("Startup failed");
            errorLabel.setText(error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Could not start Seva Tyres.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /**
     * Ensures splash is visible at least {@code minMillis}, then fades out and runs {@code afterClose}.
     */
    public void finish(long minMillis, Runnable afterClose) {
        long wait = Math.max(0, minMillis - (System.currentTimeMillis() - shownAt));
        Thread t = new Thread(() -> {
            try { Thread.sleep(wait); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            Platform.runLater(() -> {
                progressPulse.stop();
                setStatus("Ready", 1.0);
                FadeTransition out = new FadeTransition(Duration.millis(420), stage.getScene().getRoot());
                out.setFromValue(1);
                out.setToValue(0);
                out.setOnFinished(e -> {
                    stage.close();
                    if (afterClose != null) afterClose.run();
                });
                out.play();
            });
        }, "splash-finish");
        t.setDaemon(true);
        t.start();
    }

    public Stage getStage() { return stage; }
}
