package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqliteTablespaceRuleRepository implements TablespaceRuleRepository {

    private final SqliteDatabase database;

    public SqliteTablespaceRuleRepository(SqliteDatabase database) {
        this.database = database;
        createTable();
    }

    @Override
    public List<TablespaceRule> load() {
        synchronized (database.connection()) {
            List<TablespaceRule> stored = read();
            if (!stored.isEmpty()) {
                return stored;
            }
            write(TablespaceRules.defaults());
            return TablespaceRules.defaults();
        }
    }

    @Override
    public void save(List<TablespaceRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new AppException("Add at least one tablespace condition");
        }
        synchronized (database.connection()) {
            write(rules);
        }
    }

    private List<TablespaceRule> read() {
        String sql = """
                SELECT tablespace_name, metric, megabytes
                FROM tablespace_rules
                ORDER BY position
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<TablespaceRule> rules = new ArrayList<>();
            while (result.next()) {
                rules.add(new TablespaceRule(
                        result.getString("tablespace_name"),
                        TablespaceMetric.valueOf(result.getString("metric")),
                        result.getInt("megabytes")
                ));
            }
            return rules;
        } catch (SQLException exception) {
            throw new AppException("Unable to load tablespace conditions", exception);
        }
    }

    private void write(List<TablespaceRule> rules) {
        try (Statement clear = database.connection().createStatement()) {
            clear.execute("DELETE FROM tablespace_rules");
        } catch (SQLException exception) {
            throw new AppException("Unable to save tablespace conditions", exception);
        }
        String insert = """
                INSERT INTO tablespace_rules (position, tablespace_name, metric, megabytes)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(insert)) {
            int position = 0;
            for (TablespaceRule rule : rules) {
                statement.setInt(1, position++);
                statement.setString(2, rule.tablespace());
                statement.setString(3, rule.metric().name());
                statement.setInt(4, rule.megabytes());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new AppException("Unable to save tablespace conditions", exception);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tablespace_rules (
                    position INTEGER PRIMARY KEY,
                    tablespace_name TEXT NOT NULL,
                    metric TEXT NOT NULL,
                    megabytes INTEGER NOT NULL
                )
                """;
        synchronized (database.connection()) {
            try (Statement statement = database.connection().createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                throw new AppException("Unable to prepare tablespace conditions", exception);
            }
        }
    }
}
