package com.uit.feature.login.infrastructure;

import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public final class SqliteUserRepository implements UserRepository {

    private final SqliteDatabase database;
    private final PasswordHasher hasher = new PasswordHasher();

    public SqliteUserRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT id, username, email, password_hash FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                if (!hasher.matches(password, result.getString("password_hash"))) {
                    return Optional.empty();
                }
                return Optional.of(readUser(result));
            }
        } catch (SQLException exception) {
            throw new AppException("Unable to sign in", exception);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, email, password_hash FROM users WHERE email = ? COLLATE NOCASE";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(readUser(result));
            }
        } catch (SQLException exception) {
            throw new AppException("Unable to look up the Google account", exception);
        }
    }

    private static User readUser(ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("username"), result.getString("email"));
    }
}
