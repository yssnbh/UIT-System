package com.uit.feature.apogee.job;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulePlanTest {

    @Test
    void weeklyRunIsDueAfterTheChosenDayAndTime() {
        ScheduleSlot monday = ScheduleSlot.weekly(DayOfWeek.MONDAY, LocalTime.of(8, 0));
        LocalDateTime after = LocalDateTime.of(2026, 9, 21, 9, 0);
        LocalDateTime before = LocalDateTime.of(2026, 9, 21, 7, 0);

        assertEquals("Weekly on Monday at 08:00", monday.label());
        assertTrue(new SchedulePlan(List.of(monday)).isDue(after, before));
        assertFalse(new SchedulePlan(List.of(monday)).isDue(before, LocalDateTime.of(2026, 9, 14, 8, 5)));
    }

    @Test
    void severalDailyTimesStayOnOneLine() {
        SchedulePlan plan = new SchedulePlan(List.of(
                ScheduleSlot.daily(LocalTime.of(18, 0)),
                ScheduleSlot.daily(LocalTime.of(6, 0)),
                ScheduleSlot.daily(LocalTime.of(12, 0))
        ));

        assertEquals("Daily at 06:00, 12:00, 18:00", plan.label());
    }
}
