package com.uit.app.shell;

import com.uit.app.navigation.Navigator;
import com.uit.app.navigation.Screen;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

public final class ShellController {

    private final Navigator navigator;
    private final String signedInUsername;

    @FXML
    private ListView<Screen> navigation;
    @FXML
    private Label screenTitle;
    @FXML
    private Label signedIn;
    @FXML
    private StackPane content;

    public ShellController(Navigator navigator, String signedInUsername) {
        this.navigator = navigator;
        this.signedInUsername = signedInUsername;
    }

    @FXML
    private void initialize() {
        navigation.setFixedCellSize(42);
        navigation.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Screen item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
            }
        });
        navigator.attach(content);
        screenTitle.textProperty().bind(Bindings.createStringBinding(() -> {
            Screen selected = navigation.getSelectionModel().getSelectedItem();
            return selected == null ? "" : selected.title();
        }, navigation.getSelectionModel().selectedItemProperty()));

        navigation.getItems().setAll(navigator.screens());
        navigation.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (selected != null) {
                navigator.show(selected.id());
            }
        });
        if (!navigation.getItems().isEmpty()) {
            navigation.getSelectionModel().selectFirst();
        }
        signedIn.setText("Signed in as " + signedInUsername);
    }
}
