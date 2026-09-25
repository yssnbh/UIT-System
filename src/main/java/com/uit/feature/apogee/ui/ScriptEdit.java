package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.job.ExportSettings;
import com.uit.feature.apogee.job.PartitionRule;
import com.uit.feature.apogee.job.ScheduleSlot;
import com.uit.feature.apogee.job.TablespaceRule;

import java.util.List;

public record ScriptEdit(
        List<ScheduleSlot> times,
        List<TablespaceRule> conditions,
        List<PartitionRule> partitions,
        ExportSettings export
) {
}
