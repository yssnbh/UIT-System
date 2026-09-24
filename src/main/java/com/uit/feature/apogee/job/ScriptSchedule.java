package com.uit.feature.apogee.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public sealed interface ScriptSchedule permits ScriptSchedule.Daily, ScriptSchedule.DailyTimes, ScriptSchedule.Monthly {

    DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    String label();

    boolean isDue(LocalDateTime now, LocalDateTime lastRun);

    List<ScheduleSlot> slots();

    record Daily(LocalTime time) implements ScriptSchedule {
        @Override
        public String label() {
            return "Daily at " + CLOCK.format(time);
        }

        @Override
        public boolean isDue(LocalDateTime now, LocalDateTime lastRun) {
            LocalDateTime slot = LocalDateTime.of(now.toLocalDate(), time);
            if (now.isBefore(slot)) {
                slot = slot.minusDays(1);
            }
            return lastRun == null || lastRun.isBefore(slot);
        }

        @Override
        public List<ScheduleSlot> slots() {
            return List.of(ScheduleSlot.daily(time));
        }
    }

    record DailyTimes(List<LocalTime> times) implements ScriptSchedule {
        public DailyTimes {
            if (times == null || times.isEmpty()) {
                throw new IllegalArgumentException("At least one time is required");
            }
            times = times.stream().sorted().distinct().toList();
        }

        @Override
        public String label() {
            List<String> clocks = new ArrayList<>();
            for (LocalTime time : times) {
                clocks.add(CLOCK.format(time));
            }
            return "Daily at " + String.join(", ", clocks);
        }

        @Override
        public boolean isDue(LocalDateTime now, LocalDateTime lastRun) {
            LocalDateTime latest = null;
            for (LocalTime time : times) {
                LocalDateTime slot = LocalDateTime.of(now.toLocalDate(), time);
                if (now.isBefore(slot)) {
                    slot = slot.minusDays(1);
                }
                if (latest == null || slot.isAfter(latest)) {
                    latest = slot;
                }
            }
            return lastRun == null || lastRun.isBefore(latest);
        }

        @Override
        public List<ScheduleSlot> slots() {
            List<ScheduleSlot> slots = new ArrayList<>();
            for (LocalTime time : times) {
                slots.add(ScheduleSlot.daily(time));
            }
            return List.copyOf(slots);
        }
    }

    record Monthly(int dayOfMonth, LocalTime time) implements ScriptSchedule {
        public Monthly {
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                throw new IllegalArgumentException("Day of month must be from 1 to 31");
            }
        }

        @Override
        public String label() {
            return "Monthly at day " + dayOfMonth + " " + CLOCK.format(time);
        }

        @Override
        public boolean isDue(LocalDateTime now, LocalDateTime lastRun) {
            LocalDateTime slot = occurrence(now.toLocalDate());
            if (now.isBefore(slot)) {
                slot = occurrence(now.toLocalDate().minusMonths(1));
            }
            return lastRun == null || lastRun.isBefore(slot);
        }

        private LocalDateTime occurrence(LocalDate month) {
            int day = Math.min(dayOfMonth, month.lengthOfMonth());
            return LocalDateTime.of(month.withDayOfMonth(day), time);
        }

        @Override
        public List<ScheduleSlot> slots() {
            return List.of(ScheduleSlot.monthly(dayOfMonth, time));
        }
    }
}
