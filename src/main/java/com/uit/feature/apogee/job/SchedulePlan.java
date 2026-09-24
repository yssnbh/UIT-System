package com.uit.feature.apogee.job;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record SchedulePlan(List<ScheduleSlot> slots) {

    public SchedulePlan {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("At least one time is required");
        }
        slots = List.copyOf(slots);
    }

    public String label() {
        boolean allDaily = true;
        for (ScheduleSlot slot : slots) {
            if (slot.frequency() != ScheduleFrequency.DAILY) {
                allDaily = false;
                break;
            }
        }
        if (allDaily && slots.size() > 1) {
            List<String> clocks = new ArrayList<>();
            for (ScheduleSlot slot : slots) {
                clocks.add(ScriptSchedule.CLOCK.format(slot.time()));
            }
            clocks.sort(String::compareTo);
            return "Daily at " + String.join(", ", clocks);
        }
        if (slots.size() == 1) {
            return slots.getFirst().label();
        }
        List<String> lines = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            lines.add(slot.label());
        }
        return String.join("\n", lines);
    }

    public boolean isDue(LocalDateTime now, LocalDateTime lastRun) {
        LocalDateTime latest = null;
        for (ScheduleSlot slot : slots) {
            LocalDateTime passed = slot.latestPassed(now);
            if (latest == null || passed.isAfter(latest)) {
                latest = passed;
            }
        }
        return lastRun == null || lastRun.isBefore(latest);
    }
}
