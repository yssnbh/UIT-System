package com.uit.feature.apogee.job;

public record TablespaceRule(String tablespace, TablespaceMetric metric, int megabytes) {

    public TablespaceRule {
        if (tablespace == null || tablespace.isBlank()) {
            throw new IllegalArgumentException("A tablespace name is required");
        }
        if (megabytes < 0) {
            throw new IllegalArgumentException("The limit cannot be negative");
        }
        tablespace = tablespace.toUpperCase(java.util.Locale.ROOT);
    }

    public boolean passes(double freeMegabytes, double sizeMegabytes) {
        return switch (metric) {
            case FREE -> freeMegabytes >= megabytes;
            case SIZE -> sizeMegabytes <= megabytes;
        };
    }

    public String label() {
        String limit = megabytes >= 1024 && megabytes % 1024 == 0
                ? (megabytes / 1024) + " GB"
                : megabytes + " MB";
        return switch (metric) {
            case FREE -> tablespace + " free space >= " + limit;
            case SIZE -> tablespace + " size <= " + limit;
        };
    }
}
