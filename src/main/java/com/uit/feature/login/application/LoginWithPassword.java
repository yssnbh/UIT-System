package com.uit.feature.login.application;

import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;

import java.util.Optional;

public final class LoginWithPassword {

    private final UserRepository users;

    public LoginWithPassword(UserRepository users) {
        this.users = users;
    }

    public Optional<User> execute(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isEmpty()) {
            return Optional.empty();
        }
        return users.authenticate(username.trim(), password);
    }
}
