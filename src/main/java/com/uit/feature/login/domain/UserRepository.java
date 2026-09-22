package com.uit.feature.login.domain;

import java.util.Optional;

public interface UserRepository {

    Optional<User> authenticate(String username, String password);

    Optional<User> findByEmail(String email);
}
