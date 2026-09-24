package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class SqliteJobScheduleRepository implements JobScheduleRepository {

    private final SqliteDatabase database;

    public SqliteJobScheduleRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public List<ScheduleSlot> load(String scriptId, List<ScheduleSlot> defaults) {
        synchronized (database.connection()) {
            List<ScheduleSlot> stored = read(scriptId);
            if (!stored.isEmpty()) {
                return stored;
            }
            if (defaults == null || defaults.isEmpty()) {
                throw new AppException("A script needs at least one time");
            }
            write(scriptId, defaults);
            return List.copyOf(defaults);
        }
    }

    @Override
    public void save(String scriptId, List<ScheduleSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new AppException("Add at least one time");
        }
        synchronized (database.connection()) {
            write(scriptId, slots);
        }
    }

    private List<ScheduleSlot> read(String scriptId) {
        String sql = """
                SELECT frequency, day, time_of_day
                FROM job_schedules
                WHERE script_id = ?
                ORDER BY position
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, scriptId);
            try (ResultSet result = statement.executeQuery()) {
                List<ScheduleSlot> slots = new ArrayList<>();
                while (result.next()) {
                    slots.add(new ScheduleSlot(
                            ScheduleFrequency.valueOf(result.getString("frequency")),
                            result.getInt("day"),
                            LocalTime.parse(result.getString("time_of_day"))
                    ));
                }
                return slots;
            }
        } catch (SQLException exception) {
            throw new AppException("Unable to load the script schedule", exception);
        }
    }

    private void write(String scriptId, List<ScheduleSlot> slots) {
        try (PreparedStatement delete = database.connection().prepareStatement(
                "DELETE FROM job_schedules WHERE script_id = ?"
        )) {
            delete.setString(1, scriptId);
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw new AppException("Unable to save the script schedule", exception);
        }
        String insert = """
                INSERT INTO job_schedules (script_id, position, frequency, day, time_of_day)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(insert)) {
            int position = 0;
            for (ScheduleSlot slot : slots) {
                statement.setString(1, scriptId);
                statement.setInt(2, position++);
                statement.setString(3, slot.frequency().name());
                statement.setInt(4, slot.day());
                statement.setString(5, ScriptSchedule.CLOCK.format(slot.time()));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new AppException("Unable to save the script schedule", exception);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS job_schedules (
                    script_id TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    frequency TEXT NOT NULL,
                    day INTEGER NOT NULL,
                    time_of_day TEXT NOT NULL,
                    PRIMARY KEY (script_id, position)
                )
                """;
        synchronized (database.connection()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                throw new AppException("Unable to prepare script schedules", exception);
            }
        }
    }
}
