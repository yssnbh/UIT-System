package com.uit.feature.apogee.job;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptScheduleTest {

    @Test
    void describesDailyAndMonthlyTimes() {
        assertEquals("Daily at 08:00", new ScriptSchedule.Daily(LocalTime.of(8, 0)).label());
        assertEquals(
                "Daily at 06:00, 12:00, 18:00",
                new ScriptSchedule.DailyTimes(List.of(LocalTime.of(18, 0), LocalTime.of(6, 0), LocalTime.of(12, 0))).label()
        );
        assertEquals("Monthly at day 1 06:30", new ScriptSchedule.Monthly(1, LocalTime.of(6, 30)).label());
    }

    @Test
    void dailyRunIsDueOnceTheClockPassesAndNotAgainTheSameDay() {
        ScriptSchedule schedule = new ScriptSchedule.Daily(LocalTime.of(8, 0));
        LocalDateTime later = LocalDateTime.of(2026, 9, 23, 9, 0);
        LocalDateTime earlier = LocalDateTime.of(2026, 9, 23, 7, 0);

        assertTrue(schedule.isDue(later, null));
        assertTrue(schedule.isDue(later, earlier));
        assertFalse(schedule.isDue(later, later));
        assertFalse(schedule.isDue(earlier, LocalDateTime.of(2026, 9, 22, 8, 5)));
    }

    @Test
    void severalDailyTimesAreDueAfterEachSlot() {
        ScriptSchedule schedule = new ScriptSchedule.DailyTimes(List.of(
                LocalTime.of(6, 0),
                LocalTime.of(12, 0),
                LocalTime.of(18, 0)
        ));
        LocalDateTime afternoon = LocalDateTime.of(2026, 9, 23, 13, 0);

        assertTrue(schedule.isDue(afternoon, LocalDateTime.of(2026, 9, 23, 6, 30)));
        assertFalse(schedule.isDue(afternoon, LocalDateTime.of(2026, 9, 23, 12, 5)));
        assertFalse(schedule.isDue(
                LocalDateTime.of(2026, 9, 23, 5, 0),
                LocalDateTime.of(2026, 9, 22, 18, 10)
        ));
    }
}
