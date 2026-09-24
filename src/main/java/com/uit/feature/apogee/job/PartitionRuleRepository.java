package com.uit.feature.apogee.job;

import java.util.List;

public interface PartitionRuleRepository {

    List<PartitionRule> load();

    void save(List<PartitionRule> rules);
}
