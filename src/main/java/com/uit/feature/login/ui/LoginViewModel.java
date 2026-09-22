package com.uit.feature.login.ui;

import com.uit.feature.login.application.LoginWithGoogle;
import com.uit.feature.login.application.LoginWithPassword;
import com.uit.feature.login.domain.User;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Optional;

public final class LoginViewModel {

    private final LoginWithPassword loginWithPassword;
    private final LoginWithGoogle loginWithGoogle;
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    public LoginViewModel(LoginWithPassword loginWithPassword, LoginWithGoogle loginWithGoogle) {
        this.loginWithPassword = loginWithPassword;
        this.loginWithGoogle = loginWithGoogle;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    public void setBusy(boolean value) {
        busy.set(value);
    }

    public void showError(String message) {
        error.set(message);
    }

    public void clearPassword() {
        password.set("");
    }

    public Optional<User> signInWithPassword() {
        error.set("");
        String name = username.get() == null ? "" : username.get().trim();
        String secret = password.get() == null ? "" : password.get();
        if (name.isEmpty() || secret.isEmpty()) {
            error.set("Enter your username and password.");
            return Optional.empty();
        }
        Optional<User> user = loginWithPassword.execute(name, secret);
        if (user.isEmpty()) {
            error.set("Unknown username or password.");
        }
        return user;
    }

    public Optional<User> signInWithGoogle(String email) {
        Optional<User> user = loginWithGoogle.execute(email);
        if (user.isEmpty()) {
            error.set("This Google account is not registered.");
        } else {
            error.set("");
        }
        return user;
    }
}
