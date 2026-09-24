package com.uit.feature.apogee.job;

import java.util.List;

public final class TablespaceRules {

    private TablespaceRules() {
    }

    public static List<TablespaceRule> defaults() {
        return List.of(
                free("DATA_APO", 1024),
                free("DATA_APO2", 1024),
                free("DATA_BO", 200),
                size("DATA_PHO", 1024),
                free("DATA_RE", 1024),
                free("INDX_APO", 1024),
                free("INDX_APO2", 1024),
                free("INDX_RE", 500),
                size("SYSAUX", 30 * 1024),
                size("SYSTEM", 15 * 1024),
                free("TEMP", 500),
                size("TEMPORARY", 10 * 1024),
                free("TOOLS", 500),
                size("UNDOTBS1", 5 * 1024),
                size("USERS", 5)
        );
    }

    private static TablespaceRule free(String name, int megabytes) {
        return new TablespaceRule(name, TablespaceMetric.FREE, megabytes);
    }

    private static TablespaceRule size(String name, int megabytes) {
        return new TablespaceRule(name, TablespaceMetric.SIZE, megabytes);
    }
}
