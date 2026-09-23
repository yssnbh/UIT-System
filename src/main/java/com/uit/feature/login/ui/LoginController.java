package com.uit.feature.login.ui;

import com.uit.feature.login.domain.User;
import com.uit.feature.login.infrastructure.GoogleOAuthClient;
import com.uit.shared.exception.AppException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class LoginController {

    private final LoginViewModel viewModel;
    private final GoogleOAuthClient google;
    private final BooleanSupplier active;
    private final Consumer<User> onSuccess;

    @FXML
    private Label error;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private TextField passwordVisible;
    @FXML
    private Button togglePassword;
    @FXML
    private Button signIn;
    @FXML
    private Button googleButton;

    public LoginController(
            LoginViewModel viewModel,
            GoogleOAuthClient google,
            BooleanSupplier active,
            Consumer<User> onSuccess
    ) {
        this.viewModel = viewModel;
        this.google = google;
        this.active = active;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void initialize() {
        username.textProperty().bindBidirectional(viewModel.usernameProperty());
        password.textProperty().bindBidirectional(viewModel.passwordProperty());
        passwordVisible.textProperty().bindBidirectional(viewModel.passwordProperty());
        error.textProperty().bind(viewModel.errorProperty());
        error.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());

        username.disableProperty().bind(viewModel.busyProperty());
        password.disableProperty().bind(viewModel.busyProperty());
        passwordVisible.disableProperty().bind(viewModel.busyProperty());
        togglePassword.disableProperty().bind(viewModel.busyProperty());
        signIn.disableProperty().bind(viewModel.busyProperty());
        googleButton.disableProperty().bind(viewModel.busyProperty());

        Label mark = new Label("G");
        mark.setStyle("-fx-text-fill: #4285F4; -fx-font-size: 16px; -fx-font-weight: bold;");
        googleButton.setGraphic(mark);
        googleButton.setContentDisplay(ContentDisplay.LEFT);
        googleButton.setGraphicTextGap(10);

        Platform.runLater(username::requestFocus);
    }

    @FXML
    private void onTogglePassword() {
        boolean show = !passwordVisible.isVisible();
        passwordVisible.setVisible(show);
        passwordVisible.setManaged(show);
        password.setVisible(!show);
        password.setManaged(!show);
        togglePassword.setText(show ? "Hide" : "Show");
    }

    @FXML
    private void onSignIn() {
        if (viewModel.busyProperty().get()) {
            return;
        }
        viewModel.signInWithPassword().ifPresent(this::finish);
    }

    @FXML
    private void onGoogle() {
        if (viewModel.busyProperty().get()) {
            return;
        }
        viewModel.showError("");
        viewModel.setBusy(true);
        Thread worker = new Thread(() -> {
            try {
                String email = google.requestEmail();
                Platform.runLater(() -> {
                    if (!active.getAsBoolean()) {
                        viewModel.setBusy(false);
                        return;
                    }
                    viewModel.signInWithGoogle(email).ifPresent(this::finish);
                    viewModel.setBusy(false);
                });
            } catch (AppException exception) {
                Platform.runLater(() -> {
                    if (active.getAsBoolean()) {
                        viewModel.showError(exception.getMessage());
                    }
                    viewModel.setBusy(false);
                });
            } catch (RuntimeException exception) {
                exception.printStackTrace();
                Platform.runLater(() -> {
                    if (active.getAsBoolean()) {
                        viewModel.showError("Google sign-in failed. Try again.");
                    }
                    viewModel.setBusy(false);
                });
            }
        }, "google-sign-in");
        worker.setDaemon(true);
        worker.start();
    }

    private void finish(User user) {
        viewModel.clearPassword();
        onSuccess.accept(user);
    }
}
