package com.uit.feature.home.ui;

import com.uit.app.navigation.Screen;
import com.uit.shared.exception.AppException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public final class HomeScreen implements Screen {

    private final HomeViewModel viewModel;

    public HomeScreen(HomeViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public String id() {
        return "home";
    }

    @Override
    public String title() {
        return "Home";
    }

    @Override
    public Parent createView() {
        URL layout = HomeScreen.class.getResource("/com/uit/feature/home/ui/home.fxml");
        if (layout == null) {
            throw new AppException("Missing Home screen layout");
        }
        FXMLLoader loader = new FXMLLoader(layout);
        loader.setController(new HomeController(viewModel));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new AppException("Unable to open Home", exception);
        }
    }
}
