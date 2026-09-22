package com.uit.feature.login.infrastructure;

import com.uit.shared.exception.AppException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record GoogleAuthConfig(Path file, String clientId, String clientSecret) {

    public boolean configured() {
        return clientId != null && !clientId.isBlank();
    }

    public static GoogleAuthConfig load() {
        Path file = SqliteDatabase.directory().resolve("google-oauth.properties");
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                try (InputStream template = GoogleAuthConfig.class.getResourceAsStream("/com/uit/feature/login/google-oauth.properties")) {
                    if (template == null) {
                        throw new AppException("Missing Google sign-in configuration template");
                    }
                    Files.copy(template, file);
                }
            }
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
            return new GoogleAuthConfig(
                    file,
                    properties.getProperty("google.clientId", "").trim(),
                    properties.getProperty("google.clientSecret", "").trim()
            );
        } catch (IOException exception) {
            throw new AppException("Unable to read Google sign-in settings", exception);
        }
    }
}
