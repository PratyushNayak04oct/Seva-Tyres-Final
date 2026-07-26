package com.finixis.viewmodel;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton theme manager that tracks every Scene (main window + all dialogs).
 * JavaFX dialogs have their own Scene and do NOT inherit the main window's stylesheet,
 * so every dialog must register its Scene here and apply the current theme explicitly.
 */
public final class ThemeManager {
    private ThemeManager() {}

    private static boolean dark = false;
    private static final List<Scene> scenes = new ArrayList<>();

    /** Register a Scene so it receives future theme-toggle updates. */
    public static void register(Scene scene) {
        if (scene != null && !scenes.contains(scene)) {
            scenes.add(scene);
        }
    }

    /** Remove a Scene (call when the window/dialog is closing). */
    public static void unregister(Scene scene) {
        scenes.remove(scene);
    }

    public static boolean isDark() { return dark; }

    /** Not used for scene loading, kept for informational use. */
    public static String getThemePath() {
        return dark ? "/css/dark-theme.css" : "/css/light-theme.css";
    }

    /**
     * Apply the current theme to a single Scene immediately.
     * Light theme CSS contains all component rules. Dark theme CSS only overrides
     * CSS variables, so both must be loaded for dark mode to work correctly.
     */
    public static void apply(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        // Always load light-theme first (contains all component style rules)
        String lightUrl = ThemeManager.class.getResource("/css/light-theme.css").toExternalForm();
        scene.getStylesheets().add(lightUrl);
        if (dark) {
            // Override CSS variables with dark palette on top
            String darkUrl = ThemeManager.class.getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(darkUrl);
        }
    }

    /** Set theme explicitly (called at startup). */
    public static void applyTheme(boolean darkMode) {
        dark = darkMode;
        pushToAll();
    }

    /** Toggle between light and dark; updates every currently-visible Scene. */
    public static void toggle() {
        dark = !dark;
        pushToAll();
    }

    private static void pushToAll() {
        // Only evict scenes whose window has been fully closed (was shown, now gone).
        // Scenes with a null window are valid — they may not be attached to a Stage yet
        // (e.g. the primary scene before stage.show() is called the first time).
        scenes.removeIf(s -> s.getWindow() != null
                && !s.getWindow().isShowing()
                && s.getWindow().getScene() == null);
        new ArrayList<>(scenes).forEach(ThemeManager::apply);
    }

    /** @deprecated Use apply(scene) instead — theme requires two stylesheets in dark mode. */
    public static String getThemeUrl() {
        return ThemeManager.class.getResource("/css/light-theme.css").toExternalForm();
    }
}
