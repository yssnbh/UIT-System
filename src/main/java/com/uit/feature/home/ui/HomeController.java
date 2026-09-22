package com.uit.feature.home.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class HomeController {

    private final HomeViewModel viewModel;

    @FXML
    private Label message;

    public HomeController(HomeViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    private void initialize() {
        message.textProperty().bind(viewModel.messageProperty());
    }
}
