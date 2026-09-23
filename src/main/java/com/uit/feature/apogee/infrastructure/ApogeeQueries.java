package com.uit.feature.apogee.infrastructure;

import com.uit.shared.exception.AppException;

import java.util.Locale;

/**
 * Apogee SQL statements shared by the application.
 * Call a statement by its key so the same query can be used in more than one place.
 */
public enum ApogeeQueries {

    /**
     * Checks whether an Oracle account exists.
     * Bind the username in uppercase; Oracle stores account names in uppercase.
     */
    FIND_USER(
            "find-user",
            """
            SELECT *
            FROM dba_users
            WHERE username = ?
            """
    ),

    /**
     * Unlocks an Oracle account so the user can sign in again.
     * {username} is the account name in uppercase.
     */
    UNLOCK_ACCOUNT(
            "unlock-account",
            "ALTER USER {username} ACCOUNT UNLOCK"
    ),

    /**
     * Replaces the password of an Oracle account.
     * {username} is the account name in uppercase, and {password} is the new password.
     */
    CHANGE_PASSWORD(
            "change-password",
            "ALTER USER {username} IDENTIFIED BY {password}"
    ),

    /**
     * Locks an Oracle account so the user cannot sign in.
     * {username} is the account name in uppercase.
     */
    LOCK_ACCOUNT(
            "lock-account",
            "ALTER USER {username} ACCOUNT LOCK"
    ),

    /**
     * Marks the password as expired so it must be changed at the next sign-in.
     * {username} is the account name in uppercase.
     */
    EXPIRE_PASSWORD(
            "expire-password",
            "ALTER USER {username} PASSWORD EXPIRE"
    ),

    /**
     * Saves the current transaction.
     */
    COMMIT(
            "commit",
            "COMMIT"
    );

    private final String key;
    private final String sql;

    ApogeeQueries(String key, String sql) {
        this.key = key;
        this.sql = sql;
    }

    public String key() {
        return key;
    }

    public String sql() {
        return sql;
    }

    public static ApogeeQueries byKey(String key) {
        for (ApogeeQueries query : values()) {
            if (query.key.equals(key)) {
                return query;
            }
        }
        throw new AppException("Unknown Apogee query: " + key);
    }

    public String statement(String username, String password) {
        String statement = sql;
        if (statement.contains("{username}")) {
            statement = insert(statement, "{username}", quoteUsername(username));
        }
        if (statement.contains("{password}")) {
            statement = insert(statement, "{password}", quotePassword(password));
        }
        return statement;
    }

    private static String insert(String template, String token, String value) {
        int start = template.indexOf(token);
        return template.substring(0, start) + value + template.substring(start + token.length());
    }

    static String quoteUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9_$#]+")) {
            throw new AppException("Invalid account name: " + username);
        }
        return "\"" + username.toUpperCase(Locale.ROOT) + "\"";
    }

    static String quotePassword(String password) {
        if (password == null || password.isBlank() || password.chars().anyMatch(Character::isISOControl)) {
            throw new AppException("The password is empty or contains unsupported characters");
        }
        return "\"" + password.replace("\"", "\"\"") + "\"";
    }
}
