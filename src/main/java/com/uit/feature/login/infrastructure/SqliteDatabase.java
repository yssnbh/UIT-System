package com.uit.feature.login.infrastructure;

import com.uit.shared.exception.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteDatabase implements AutoCloseable {

    private final Connection connection;

    private SqliteDatabase(Connection connection) {
        this.connection = connection;
    }

    public static Path directory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "UIT System");
        }
        return Path.of(System.getProperty("user.home"), ".uit-system");
    }

    public static SqliteDatabase openDefault() {
        SqliteDatabase database = open(directory().resolve("uit-system.db"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                database.close();
            } catch (RuntimeException ignored) {
                // The process is already leaving.
            }
        }, "uit-database-close"));
        return database;
    }

    public static SqliteDatabase open(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            SqliteDatabase database = new SqliteDatabase(connection);
            database.initialize();
            return database;
        } catch (IOException | SQLException exception) {
            throw new AppException("Unable to open the database", exception);
        }
    }

    Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            throw new AppException("Unable to close the database", exception);
        }
    }

    private void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        password_hash TEXT NOT NULL
                    )
                    """);
        }
        InitialUsers.insertIfMissing(connection, new PasswordHasher());
    }
}
