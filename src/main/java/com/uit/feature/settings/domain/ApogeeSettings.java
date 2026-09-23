package com.uit.feature.settings.domain;

public record ApogeeSettings(
        String serverIp,
        String port,
        String rootUsername,
        String rootPassword,
        String oracleUsername,
        String oraclePassword,
        String apogeePassword,
        String adminPassword,
        String sysPassword,
        String systemPassword,
        String sysmanPassword,
        String databaseIp,
        String databasePort,
        String databaseSid
) {

    public ApogeeSettings {
        serverIp = clean(serverIp);
        port = clean(port);
        rootUsername = clean(rootUsername);
        rootPassword = clean(rootPassword);
        oracleUsername = clean(oracleUsername);
        oraclePassword = clean(oraclePassword);
        apogeePassword = clean(apogeePassword);
        adminPassword = clean(adminPassword);
        sysPassword = clean(sysPassword);
        systemPassword = clean(systemPassword);
        sysmanPassword = clean(sysmanPassword);
        databaseIp = clean(databaseIp);
        databasePort = clean(databasePort);
        databaseSid = clean(databaseSid);
    }

    public static ApogeeSettings empty() {
        return new ApogeeSettings("", "", "", "", "", "", "", "", "", "", "", "", "", "");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
