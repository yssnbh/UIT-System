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
     * Reads each data file and temp file, not the tablespace total.
     * Columns are tablespace, file name, used megabytes, current size megabytes, and free megabytes.
     * Size is the file size now, not the autoextend maximum.
     */
    TABLESPACE_USAGE(
            "tablespace-usage",
            """
            SELECT tablespace_name, file_name,
                   ROUND(used_bytes / 1024 / 1024, 0),
                   ROUND(size_bytes / 1024 / 1024, 0),
                   ROUND(free_bytes / 1024 / 1024, 0)
            FROM (
                SELECT d.tablespace_name,
                       d.file_name,
                       d.bytes - NVL(f.free_bytes, 0) AS used_bytes,
                       d.bytes AS size_bytes,
                       NVL(f.free_bytes, 0) AS free_bytes
                FROM dba_data_files d
                LEFT JOIN (
                    SELECT file_id, SUM(bytes) free_bytes
                    FROM dba_free_space
                    GROUP BY file_id
                ) f ON f.file_id = d.file_id
                UNION ALL
                SELECT t.tablespace_name,
                       t.file_name,
                       NVL(h.bytes_used, 0),
                       t.bytes,
                       NVL(h.bytes_free, 0)
                FROM dba_temp_files t
                LEFT JOIN v$temp_space_header h ON h.file_id = t.file_id
            )
            ORDER BY tablespace_name, file_name
            """
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
