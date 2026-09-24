package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class SqliteJobResultRepository implements JobResultRepository {

    static final DateTimeFormatter STORED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SqliteDatabase database;

    public SqliteJobResultRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public void save(JobResult result) {
        String sql = """
                INSERT INTO job_results (script_id, executed_at, success, result)
                VALUES (?, ?, ?, ?)
                """;
        synchronized (database.connection()) {
            try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
                statement.setString(1, result.scriptId());
                statement.setString(2, STORED_TIME.format(result.executedAt()));
                statement.setInt(3, result.success() ? 1 : 0);
                statement.setString(4, result.storedResult());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new AppException("Unable to save the job result", exception);
            }
        }
    }

    @Override
    public Optional<JobResult> latest(String scriptId) {
        String sql = """
                SELECT script_id, executed_at, success, result
                FROM job_results
                WHERE script_id = ?
                ORDER BY executed_at DESC, id DESC
                LIMIT 1
                """;
        synchronized (database.connection()) {
            try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
                statement.setString(1, scriptId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(read(result));
                }
            } catch (SQLException exception) {
                throw new AppException("Unable to load the job result", exception);
            }
        }
    }

    @Override
    public void deleteOlderThan(LocalDateTime cutoff) {
        synchronized (database.connection()) {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "DELETE FROM job_results WHERE executed_at < ?"
            )) {
                statement.setString(1, STORED_TIME.format(cutoff));
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new AppException("Unable to remove old job results", exception);
            }
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS job_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    script_id TEXT NOT NULL,
                    executed_at TEXT NOT NULL,
                    success INTEGER NOT NULL,
                    result TEXT NOT NULL
                )
                """;
        synchronized (database.connection()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                throw new AppException("Unable to prepare job results", exception);
            }
        }
    }

    private static JobResult read(ResultSet result) throws SQLException {
        return new JobResult(
                result.getString("script_id"),
                LocalDateTime.parse(result.getString("executed_at"), STORED_TIME),
                result.getInt("success") == 1,
                result.getString("result")
        );
    }
}
