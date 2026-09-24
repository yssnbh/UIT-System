package com.uit.feature.apogee.infrastructure;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class OracleConnections {

    private OracleConnections() {
    }

    public static Connection open(ApogeeSettings settings) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", "SYS");
        properties.setProperty("password", settings.sysPassword());
        properties.setProperty("internal_logon", "SYSDBA");
        properties.setProperty("oracle.net.CONNECT_TIMEOUT", "15000");
        properties.setProperty("oracle.jdbc.ReadTimeout", "30000");
        return DriverManager.getConnection(url(settings), properties);
    }

    private static String url(ApogeeSettings settings) {
        if (!settings.databaseIp().matches("[A-Za-z0-9][A-Za-z0-9.-]*")
                || !settings.databasePort().matches("\\d{1,5}")
                || !settings.databaseSid().matches("[A-Za-z0-9_$#]+")) {
            throw new AppException("Database IP, port, or SID is not valid");
        }
        int port = Integer.parseInt(settings.databasePort());
        if (port < 1 || port > 65535) {
            throw new AppException("Database port is not valid");
        }
        return "jdbc:oracle:thin:@" + settings.databaseIp() + ":" + port + ":" + settings.databaseSid();
    }
}
