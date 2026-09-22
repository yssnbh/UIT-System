package com.uit.feature.login.ui;

import com.uit.feature.login.application.LoginWithGoogle;
import com.uit.feature.login.application.LoginWithPassword;
import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;
import com.uit.feature.login.infrastructure.GoogleOAuthClient;
import com.uit.shared.exception.AppException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LoginWindow {

    private final LoginViewModel viewModel;
    private final GoogleOAuthClient google;

    public LoginWindow(UserRepository users, GoogleOAuthClient google) {
        this.viewModel = new LoginViewModel(new LoginWithPassword(users), new LoginWithGoogle(users));
        this.google = google;
    }

    public void open(Stage stage, Consumer<User> onSuccess) {
        AtomicBoolean active = new AtomicBoolean(true);
        URL layout = LoginWindow.class.getResource("/com/uit/feature/login/ui/login.fxml");
        if (layout == null) {
            throw new AppException("Missing login screen layout");
        }
        FXMLLoader loader = new FXMLLoader(layout);
        loader.setController(new LoginController(viewModel, google, active::get, user -> {
            active.set(false);
            google.cancel();
            onSuccess.accept(user);
        }));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException exception) {
            throw new AppException("Unable to open the login screen", exception);
        }
        Scene scene = new Scene(root, 1040, 680);
        URL stylesheet = LoginWindow.class.getResource("/com/uit/feature/login/ui/login.css");
        if (stylesheet == null) {
            throw new AppException("Missing login screen stylesheet");
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setOnCloseRequest(event -> {
            active.set(false);
            google.cancel();
        });
        stage.setTitle("Sign in — UIT System");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
