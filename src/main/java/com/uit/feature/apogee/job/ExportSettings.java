package com.uit.feature.apogee.job;

public record ExportSettings(String path, int expectedGigabytes, String localPath) {

    public ExportSettings {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("A folder path is required");
        }
        if (localPath == null || localPath.isBlank()) {
            throw new IllegalArgumentException("A folder on this PC is required");
        }
        if (expectedGigabytes < 1) {
            throw new IllegalArgumentException("Expected size must be at least 1 GB");
        }
        path = path.trim();
        localPath = localPath.trim();
    }

    public static ExportSettings defaults() {
        return new ExportSettings("/fra/sauvegarde/dmp", 15, "D:\\UIT backups");
    }

    public long expectedBytes() {
        return expectedGigabytes * 1024L * 1024L * 1024L;
    }
}
