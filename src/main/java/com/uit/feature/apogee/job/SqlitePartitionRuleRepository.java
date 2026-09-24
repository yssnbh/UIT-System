package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqlitePartitionRuleRepository implements PartitionRuleRepository {

    private final SqliteDatabase database;

    public SqlitePartitionRuleRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public List<PartitionRule> load() {
        synchronized (database.connection()) {
            List<PartitionRule> stored = read();
            if (!stored.isEmpty()) {
                return stored;
            }
            write(PartitionRules.defaults());
            return PartitionRules.defaults();
        }
    }

    @Override
    public void save(List<PartitionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new AppException("Add at least one partition condition");
        }
        synchronized (database.connection()) {
            write(rules);
        }
    }

    private List<PartitionRule> read() {
        String sql = """
                SELECT mount, max_percent
                FROM partition_rules
                ORDER BY position
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<PartitionRule> rules = new ArrayList<>();
            while (result.next()) {
                rules.add(new PartitionRule(result.getString("mount"), result.getInt("max_percent")));
            }
            return rules;
        } catch (SQLException exception) {
            throw new AppException("Unable to load partition conditions", exception);
        }
    }

    private void write(List<PartitionRule> rules) {
        try (Statement clear = database.connection().createStatement()) {
            clear.execute("DELETE FROM partition_rules");
        } catch (SQLException exception) {
            throw new AppException("Unable to save partition conditions", exception);
        }
        String insert = """
                INSERT INTO partition_rules (position, mount, max_percent)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(insert)) {
            int position = 0;
            for (PartitionRule rule : rules) {
                statement.setInt(1, position++);
                statement.setString(2, rule.mount());
                statement.setInt(3, rule.maxPercent());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new AppException("Unable to save partition conditions", exception);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS partition_rules (
                    position INTEGER PRIMARY KEY,
                    mount TEXT NOT NULL,
                    max_percent INTEGER NOT NULL
                )
                """;
        synchronized (database.connection()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                throw new AppException("Unable to prepare partition conditions", exception);
            }
        }
    }
}
