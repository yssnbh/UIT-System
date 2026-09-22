package com.uit.app.navigation;

import com.uit.shared.exception.AppException;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows a registered screen inside the shell content area and keeps its view alive.
 */
public final class Navigator {

    private final Map<String, Screen> screens;
    private final Map<String, Parent> views = new LinkedHashMap<>();
    private StackPane host;

    public Navigator(List<Screen> screens) {
        this.screens = new LinkedHashMap<>();
        for (Screen screen : screens) {
            if (this.screens.putIfAbsent(screen.id(), screen) != null) {
                throw new AppException("Duplicate screen id: " + screen.id());
            }
        }
    }

    public List<Screen> screens() {
        return List.copyOf(screens.values());
    }

    public void attach(StackPane host) {
        this.host = host;
    }

    public void show(String screenId) {
        if (host == null) {
            throw new AppException("Navigator is not attached to the shell");
        }
        Screen screen = screens.get(screenId);
        if (screen == null) {
            throw new AppException("Unknown screen: " + screenId);
        }
        Parent view = views.computeIfAbsent(screenId, id -> screen.createView());
        host.getChildren().setAll(view);
    }
}
