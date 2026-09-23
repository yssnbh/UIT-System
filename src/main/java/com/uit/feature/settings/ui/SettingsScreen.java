package com.uit.feature.settings.ui;

import com.uit.app.navigation.Screen;
import com.uit.shared.exception.AppException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public final class SettingsScreen implements Screen {

    private final ApogeeSettingsViewModel viewModel;

    public SettingsScreen(ApogeeSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public String id() {
        return "settings";
    }

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public Parent createView() {
        URL layout = SettingsScreen.class.getResource("/com/uit/feature/settings/ui/settings.fxml");
        if (layout == null) {
            throw new AppException("Missing Settings screen layout");
        }
        FXMLLoader loader = new FXMLLoader(layout);
        loader.setController(new SettingsController(viewModel));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new AppException("Unable to open Settings", exception);
        }
    }
}
