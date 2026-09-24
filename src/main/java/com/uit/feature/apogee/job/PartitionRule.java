package com.uit.feature.apogee.job;

public record PartitionRule(String mount, int maxPercent) {

    public static final String OTHERS = "*";

    public PartitionRule {
        if (mount == null || mount.isBlank()) {
            throw new IllegalArgumentException("A mount path is required");
        }
        if (maxPercent < 0 || maxPercent > 100) {
            throw new IllegalArgumentException("Used space must be from 0 to 100");
        }
    }

    public boolean others() {
        return OTHERS.equals(mount);
    }

    public boolean passes(int usedPercent) {
        return usedPercent <= maxPercent;
    }

    public String label() {
        String name = others() ? "Other partitions" : mount;
        return name + " used space <= " + maxPercent + "%";
    }
}
