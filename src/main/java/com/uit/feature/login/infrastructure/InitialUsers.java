package com.uit.feature.login.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

final class InitialUsers {

    private InitialUsers() {
    }

    static void insertIfMissing(Connection connection, PasswordHasher hasher) throws SQLException {
        insertIfMissing(connection, hasher, "yssn", "yassine.bouhroz@uit.ac.ma", "yssnBH++09");
    }

    private static void insertIfMissing(
            Connection connection,
            PasswordHasher hasher,
            String username,
            String email,
            String password
    ) throws SQLException {
        if (exists(connection, username)) {
            return;
        }
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email.toLowerCase(Locale.ROOT));
            statement.setString(3, hasher.hash(password));
            statement.executeUpdate();
        }
    }

    private static boolean exists(Connection connection, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
