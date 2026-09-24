package com.uit.feature.settings.infrastructure;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteApogeeSettingsRepository implements ApogeeSettingsRepository {

    private final SqliteDatabase database;

    public SqliteApogeeSettingsRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public ApogeeSettings load() {
        synchronized (database.connection()) {
            return loadSettings();
        }
    }

    private ApogeeSettings loadSettings() {
        String sql = """
                SELECT server_ip, port, root_username, root_password, oracle_username, oracle_password,
                       apogee_user, admin_user, sys_user, system_user, sysman_user,
                       database_ip, database_port, database_sid
                FROM apogee_settings
                WHERE id = 1
                """;
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) {
                return ApogeeSettings.empty();
            }
            return new ApogeeSettings(
                    result.getString("server_ip"),
                    result.getString("port"),
                    result.getString("root_username"),
                    result.getString("root_password"),
                    result.getString("oracle_username"),
                    result.getString("oracle_password"),
                    result.getString("apogee_user"),
                    result.getString("admin_user"),
                    result.getString("sys_user"),
                    result.getString("system_user"),
                    result.getString("sysman_user"),
                    result.getString("database_ip"),
                    result.getString("database_port"),
                    result.getString("database_sid")
            );
        } catch (SQLException exception) {
            throw new AppException("Unable to load Apogee settings", exception);
        }
    }

    @Override
    public void save(ApogeeSettings settings) {
        String sql = """
                INSERT INTO apogee_settings (
                    id, server_ip, port, root_username, root_password, oracle_username, oracle_password,
                    apogee_user, admin_user, sys_user, system_user, sysman_user,
                    database_ip, database_port, database_sid
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    server_ip = excluded.server_ip,
                    port = excluded.port,
                    root_username = excluded.root_username,
                    root_password = excluded.root_password,
                    oracle_username = excluded.oracle_username,
                    oracle_password = excluded.oracle_password,
                    apogee_user = excluded.apogee_user,
                    admin_user = excluded.admin_user,
                    sys_user = excluded.sys_user,
                    system_user = excluded.system_user,
                    sysman_user = excluded.sysman_user,
                    database_ip = excluded.database_ip,
                    database_port = excluded.database_port,
                    database_sid = excluded.database_sid
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, settings.serverIp());
            statement.setString(2, settings.port());
            statement.setString(3, settings.rootUsername());
            statement.setString(4, settings.rootPassword());
            statement.setString(5, settings.oracleUsername());
            statement.setString(6, settings.oraclePassword());
            statement.setString(7, settings.apogeePassword());
            statement.setString(8, settings.adminPassword());
            statement.setString(9, settings.sysPassword());
            statement.setString(10, settings.systemPassword());
            statement.setString(11, settings.sysmanPassword());
            statement.setString(12, settings.databaseIp());
            statement.setString(13, settings.databasePort());
            statement.setString(14, settings.databaseSid());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new AppException("Unable to save Apogee settings", exception);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS apogee_settings (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    server_ip TEXT NOT NULL,
                    port TEXT NOT NULL,
                    root_username TEXT NOT NULL,
                    root_password TEXT NOT NULL,
                    oracle_username TEXT NOT NULL,
                    oracle_password TEXT NOT NULL,
                    apogee_user TEXT NOT NULL,
                    admin_user TEXT NOT NULL,
                    sys_user TEXT NOT NULL,
                    system_user TEXT NOT NULL,
                    sysman_user TEXT NOT NULL,
                    database_ip TEXT NOT NULL,
                    database_port TEXT NOT NULL,
                    database_sid TEXT NOT NULL
                )
                """;
        try (Statement statement = database.connection().createStatement()) {
            statement.execute(sql);
            if (!columnExists(statement, "database_ip")) {
                statement.execute("ALTER TABLE apogee_settings ADD COLUMN database_ip TEXT NOT NULL DEFAULT ''");
            }
        } catch (SQLException exception) {
            throw new AppException("Unable to prepare Apogee settings", exception);
        }
    }

    private static boolean columnExists(Statement statement, String column) throws SQLException {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(apogee_settings)")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
