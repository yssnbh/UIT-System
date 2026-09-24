package com.uit.feature.apogee.job;

import com.uit.feature.apogee.infrastructure.OracleConnections;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcOracleReader implements OracleReader {

    @Override
    public List<String[]> query(ApogeeSettings settings, String sql) {
        try (Connection connection = OracleConnections.open(settings);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            int columns = result.getMetaData().getColumnCount();
            List<String[]> rows = new ArrayList<>();
            while (result.next()) {
                String[] row = new String[columns];
                for (int index = 0; index < columns; index++) {
                    row[index] = result.getString(index + 1);
                }
                rows.add(row);
            }
            return rows;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            String message = exception.getMessage();
            throw new AppException(message == null || message.isBlank() ? "Unable to query Oracle" : message, exception);
        }
    }
}
