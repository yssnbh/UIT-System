package com.uit.feature.apogee.job;

import java.util.List;

public interface TablespaceRuleRepository {

    List<TablespaceRule> load();

    void save(List<TablespaceRule> rules);
}
