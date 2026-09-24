package com.uit.feature.apogee.infrastructure;

import com.uit.feature.apogee.application.AccountUnlockResult;
import com.uit.feature.apogee.application.ApogeeDatabase;
import com.uit.feature.apogee.application.RandomPassword;
import com.uit.feature.apogee.application.UnlockOutcome;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OracleApogeeDatabase implements ApogeeDatabase {

    @Override
    public List<AccountUnlockResult> unlock(ApogeeSettings settings, List<String> usernames, String password) {
        try (Connection connection = open(settings)) {
            List<AccountUnlockResult> results = new ArrayList<>();
            for (String username : usernames) {
                results.add(unlockOne(connection, username, password));
            }
            return results;
        } catch (SQLException exception) {
            throw new AppException(exception.getMessage(), exception);
        }
    }

    @Override
    public List<AccountUnlockResult> lock(ApogeeSettings settings, List<String> usernames) {
        try (Connection connection = open(settings)) {
            List<AccountUnlockResult> results = new ArrayList<>();
            for (String username : usernames) {
                results.add(lockOne(connection, username));
            }
            return results;
        } catch (SQLException exception) {
            throw new AppException(exception.getMessage(), exception);
        }
    }

    private static AccountUnlockResult lockOne(Connection connection, String username) {
        try {
            String account = username.toUpperCase(Locale.ROOT);
            if (!exists(connection, account)) {
                return new AccountUnlockResult(account, UnlockOutcome.NOT_FOUND, "User not exists");
            }
            String password = RandomPassword.generate();
            try (Statement statement = connection.createStatement()) {
                statement.execute(ApogeeQueries.CHANGE_PASSWORD.statement(account, password));
                statement.execute(ApogeeQueries.LOCK_ACCOUNT.statement(account, null));
                statement.execute(ApogeeQueries.EXPIRE_PASSWORD.statement(account, null));
                statement.execute(ApogeeQueries.COMMIT.sql());
            }
            return new AccountUnlockResult(account, UnlockOutcome.UNLOCKED, "Account locked");
        } catch (SQLException | AppException exception) {
            return new AccountUnlockResult(username.toUpperCase(Locale.ROOT), UnlockOutcome.ERROR, exception.getMessage());
        }
    }

    private static AccountUnlockResult unlockOne(Connection connection, String username, String password) {
        try {
            String account = username.toUpperCase(Locale.ROOT);
            if (!exists(connection, account)) {
                return new AccountUnlockResult(account, UnlockOutcome.NOT_FOUND, "User not exists");
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(ApogeeQueries.UNLOCK_ACCOUNT.statement(account, null));
                statement.execute(ApogeeQueries.CHANGE_PASSWORD.statement(account, password));
                statement.execute(ApogeeQueries.COMMIT.sql());
            }
            return new AccountUnlockResult(account, UnlockOutcome.UNLOCKED, "Unlocked and password changed");
        } catch (SQLException | AppException exception) {
            return new AccountUnlockResult(username.toUpperCase(Locale.ROOT), UnlockOutcome.ERROR, exception.getMessage());
        }
    }

    private static boolean exists(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ApogeeQueries.FIND_USER.sql())) {
            statement.setString(1, username.toUpperCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static Connection open(ApogeeSettings settings) throws SQLException {
        return OracleConnections.open(settings);
    }
}
