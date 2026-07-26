package com.finixis.viewmodel;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiUtil {
    private UiUtil() {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    public static String money(double v) {
        return String.format(Locale.US, "₹%,.2f", v);
    }

    public static String signedMoney(double v) {
        return (v < 0 ? "-" : "+") + money(Math.abs(v));
    }

    public static String date(LocalDate d) {
        return d == null ? "—" : DATE.format(d);
    }

    public static String dateRangeLabel(LocalDate d) {
        if (d == null) return "Earlier";
        LocalDate today = LocalDate.now();
        if (d.equals(today) || d.isAfter(today.minusDays(1))) return "Today";
        if (d.isAfter(today.minusDays(7))) return "This Week";
        if (d.isAfter(today.minusDays(30))) return "This Month";
        if (d.isAfter(today.minusDays(90))) return "Last 3 Months";
        return "Earlier";
    }

    /** Lightweight non-blocking toast anchored to a pane. */
    public static void toast(StackPane root, String message) {
        if (root == null || root.getScene() == null || root.getScene().getWindow() == null) return;
        Popup popup = new Popup();
        Label label = new Label(message);
        label.getStyleClass().add("toast");
        popup.getContent().add(label);
        popup.setAutoFix(true);
        double w = root.getScene().getWidth();
        popup.show(root.getScene().getWindow());
        popup.setX(root.getScene().getWindow().getX() + (w - label.getWidth()) / 2.0 - 60);
        popup.setY(root.getScene().getWindow().getY() + root.getScene().getHeight() - 90);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.2));
        pause.setOnFinished(e -> popup.hide());
        pause.play();
    }
}
