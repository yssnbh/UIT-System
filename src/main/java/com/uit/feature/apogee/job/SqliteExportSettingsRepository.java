package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteExportSettingsRepository implements ExportSettingsRepository {

    private final SqliteDatabase database;

    public SqliteExportSettingsRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public ExportSettings load() {
        synchronized (database.connection()) {
            String sql = "SELECT path, expected_gb, local_path FROM export_settings WHERE id = 1";
            try (PreparedStatement statement = database.connection().prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    save(ExportSettings.defaults());
                    return ExportSettings.defaults();
                }
                String localPath = result.getString("local_path");
                if (localPath == null || localPath.isBlank()) {
                    localPath = ExportSettings.defaults().localPath();
                }
                return new ExportSettings(result.getString("path"), result.getInt("expected_gb"), localPath);
            } catch (SQLException exception) {
                throw new AppException("Unable to load the export settings", exception);
            }
        }
    }

    @Override
    public void save(ExportSettings settings) {
        synchronized (database.connection()) {
            String sql = """
                    INSERT INTO export_settings (id, path, expected_gb, local_path)
                    VALUES (1, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        path = excluded.path,
                        expected_gb = excluded.expected_gb,
                        local_path = excluded.local_path
                    """;
            try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
                statement.setString(1, settings.path());
                statement.setInt(2, settings.expectedGigabytes());
                statement.setString(3, settings.localPath());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new AppException("Unable to save the export settings", exception);
            }
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS export_settings (
                    id INTEGER PRIMARY KEY,
                    path TEXT NOT NULL,
                    expected_gb INTEGER NOT NULL,
                    local_path TEXT
                )
                """;
        synchronized (database.connection()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute(sql);
                statement.execute("ALTER TABLE export_settings ADD COLUMN local_path TEXT");
            } catch (SQLException exception) {
                if (exception.getMessage() == null || !exception.getMessage().contains("duplicate column")) {
                    throw new AppException("Unable to prepare export settings", exception);
                }
            }
        }
    }
}
