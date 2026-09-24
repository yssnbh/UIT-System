package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteJobResultRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void keepsTheLatestResultAndDropsRowsOlderThanSixtyDays() {
        SqliteDatabase database = SqliteDatabase.open(folder.resolve("uit.db"));
        try {
            SqliteJobResultRepository repository = new SqliteJobResultRepository(database);
            LocalDateTime now = LocalDateTime.of(2026, 9, 23, 9, 0);
            repository.save(new JobResult("check-server-space", now.minusDays(61), true, "old"));
            repository.save(new JobResult("check-server-space", now, true, "current"));

            repository.deleteOlderThan(now.minusDays(60));

            JobResult latest = repository.latest("check-server-space").orElseThrow();
            assertEquals("current", latest.storedResult());
            assertTrue(latest.success());
        } finally {
            database.close();
        }
    }
}
