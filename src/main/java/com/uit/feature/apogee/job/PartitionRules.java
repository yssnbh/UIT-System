package com.uit.feature.apogee.job;

import java.util.List;

public final class PartitionRules {

    private PartitionRules() {
    }

    public static List<PartitionRule> defaults() {
        return List.of(
                new PartitionRule("/data", 90),
                new PartitionRule("/fra", 60),
                new PartitionRule(PartitionRule.OTHERS, 70)
        );
    }
}
