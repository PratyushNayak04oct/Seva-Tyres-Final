package com.sevatyres.viewmodel;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton theme manager — tracks every Scene (main + dialogs).
 */
public final class ThemeManager {
    private ThemeManager() {}

    private static boolean dark = false;
    private static final List<Scene> scenes = new ArrayList<>();

    public static void register(Scene scene) {
        if (scene != null && !scenes.contains(scene)) scenes.add(scene);
    }

    public static void unregister(Scene scene) {
        scenes.remove(scene);
    }

    public static boolean isDark() { return dark; }

    public static String getThemePath() {
        return dark ? "/css/dark-theme.css" : "/css/light-theme.css";
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String lightUrl = ThemeManager.class.getResource("/css/light-theme.css").toExternalForm();
        scene.getStylesheets().add(lightUrl);
        if (dark) {
            String darkUrl = ThemeManager.class.getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(darkUrl);
        }
    }

    public static void applyTheme(boolean darkMode) {
        dark = darkMode;
        pushToAll();
    }

    public static void toggle() {
        dark = !dark;
        pushToAll();
    }

    private static void pushToAll() {
        scenes.removeIf(s -> s.getWindow() != null
                && !s.getWindow().isShowing()
                && s.getWindow().getScene() == null);
        new ArrayList<>(scenes).forEach(ThemeManager::apply);
    }

    public static String getThemeUrl() {
        return ThemeManager.class.getResource("/css/light-theme.css").toExternalForm();
    }
}
