package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqliteJobScheduleRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void storesTheChosenTimesAndKeepsThemAfterReload() {
        SqliteDatabase database = SqliteDatabase.open(folder.resolve("uit.db"));
        try {
            SqliteJobScheduleRepository repository = new SqliteJobScheduleRepository(database);
            List<ScheduleSlot> defaults = List.of(ScheduleSlot.daily(LocalTime.of(8, 0)));
            assertEquals("08:00", ScriptSchedule.CLOCK.format(repository.load("check-server-space", defaults).getFirst().time()));

            repository.save("check-server-space", List.of(
                    ScheduleSlot.weekly(DayOfWeek.FRIDAY, LocalTime.of(18, 30)),
                    ScheduleSlot.monthly(15, LocalTime.of(6, 0))
            ));

            List<ScheduleSlot> stored = repository.load("check-server-space", defaults);
            assertEquals(ScheduleFrequency.WEEKLY, stored.get(0).frequency());
            assertEquals(DayOfWeek.FRIDAY.getValue(), stored.get(0).day());
            assertEquals(LocalTime.of(18, 30), stored.get(0).time());
            assertEquals(ScheduleFrequency.MONTHLY, stored.get(1).frequency());
            assertEquals(15, stored.get(1).day());
        } finally {
            database.close();
        }
    }
}
