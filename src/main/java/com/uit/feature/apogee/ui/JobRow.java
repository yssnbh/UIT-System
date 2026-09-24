package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.job.ScriptResultLine;

import java.util.List;

public record JobRow(
        String scriptId,
        String name,
        String when,
        String lastExecute,
        String success,
        boolean failed,
        List<ScriptResultLine> result
) {
}
