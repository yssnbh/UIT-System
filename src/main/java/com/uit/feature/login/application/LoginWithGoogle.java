package com.uit.feature.login.application;

import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;

import java.util.Locale;
import java.util.Optional;

/**
 * Accepts an email that Google has already verified. Unknown emails are rejected.
 */
public final class LoginWithGoogle {

    private final UserRepository users;

    public LoginWithGoogle(UserRepository users) {
        this.users = users;
    }

    public Optional<User> execute(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }
}
