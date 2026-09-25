package com.uit.feature.apogee.job;

import com.uit.feature.settings.domain.ApogeeSettings;

public interface JobScript {

    String id();

    String name();

    ScriptSchedule schedule();

    JobExecution execute(ApogeeSettings settings);

    default JobExecution execute(ApogeeSettings settings, JobProgress progress) {
        return execute(settings);
    }
}
