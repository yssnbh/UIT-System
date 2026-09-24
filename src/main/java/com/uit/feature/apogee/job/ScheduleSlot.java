package com.uit.feature.apogee.job;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public record ScheduleSlot(ScheduleFrequency frequency, int day, LocalTime time) {

    public ScheduleSlot {
        if (time == null) {
            throw new IllegalArgumentException("A time is required");
        }
        if (frequency == ScheduleFrequency.WEEKLY && (day < 1 || day > 7)) {
            throw new IllegalArgumentException("Day of week must be from 1 to 7");
        }
        if (frequency == ScheduleFrequency.MONTHLY && (day < 1 || day > 31)) {
            throw new IllegalArgumentException("Day of month must be from 1 to 31");
        }
    }

    public static ScheduleSlot daily(LocalTime time) {
        return new ScheduleSlot(ScheduleFrequency.DAILY, 0, time);
    }

    public static ScheduleSlot weekly(DayOfWeek day, LocalTime time) {
        return new ScheduleSlot(ScheduleFrequency.WEEKLY, day.getValue(), time);
    }

    public static ScheduleSlot monthly(int dayOfMonth, LocalTime time) {
        return new ScheduleSlot(ScheduleFrequency.MONTHLY, dayOfMonth, time);
    }

    public String label() {
        String clock = ScriptSchedule.CLOCK.format(time);
        return switch (frequency) {
            case DAILY -> "Daily at " + clock;
            case WEEKLY -> "Weekly on " + DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " at " + clock;
            case MONTHLY -> "Monthly at day " + day + " " + clock;
        };
    }

    public LocalDateTime latestPassed(LocalDateTime now) {
        return switch (frequency) {
            case DAILY -> dailySlot(now);
            case WEEKLY -> weeklySlot(now);
            case MONTHLY -> monthlySlot(now);
        };
    }

    private LocalDateTime dailySlot(LocalDateTime now) {
        LocalDateTime slot = LocalDateTime.of(now.toLocalDate(), time);
        if (now.isBefore(slot)) {
            slot = slot.minusDays(1);
        }
        return slot;
    }

    private LocalDateTime weeklySlot(LocalDateTime now) {
        int back = (now.getDayOfWeek().getValue() - day + 7) % 7;
        LocalDateTime slot = LocalDateTime.of(now.toLocalDate().minusDays(back), time);
        if (now.isBefore(slot)) {
            slot = slot.minusWeeks(1);
        }
        return slot;
    }

    private LocalDateTime monthlySlot(LocalDateTime now) {
        LocalDateTime slot = occurrence(now.toLocalDate());
        if (now.isBefore(slot)) {
            slot = occurrence(now.toLocalDate().minusMonths(1));
        }
        return slot;
    }

    private LocalDateTime occurrence(LocalDate month) {
        int chosen = Math.min(day, month.lengthOfMonth());
        return LocalDateTime.of(month.withDayOfMonth(chosen), time);
    }
}
