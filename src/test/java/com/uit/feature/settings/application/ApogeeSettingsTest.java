package com.uit.feature.settings.application;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.infrastructure.SqliteApogeeSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApogeeSettingsTest {

    @TempDir
    Path folder;

    @Test
    void savesAndLoadsApogeeConnectionDetails() {
        SqliteDatabase database = SqliteDatabase.open(folder.resolve("uit.db"));
        try {
            SqliteApogeeSettingsRepository repository = new SqliteApogeeSettingsRepository(database);
            assertEquals(ApogeeSettings.empty(), new LoadApogeeSettings(repository).execute());

            ApogeeSettings settings = new ApogeeSettings(
                    "10.1.1.20",
                    "22",
                    "root",
                    "root-secret",
                    "oracle",
                    "oracle-secret",
                    "apogee",
                    "admin",
                    "sys-secret",
                    "system",
                    "sysman",
                    "10.1.1.30",
                    "1521",
                    "APOGEE"
            );
            new SaveApogeeSettings(repository).execute(settings);

            assertEquals(settings, new LoadApogeeSettings(repository).execute());
        } finally {
            database.close();
        }
    }
}
