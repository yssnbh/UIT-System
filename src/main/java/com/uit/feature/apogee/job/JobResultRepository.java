package com.uit.feature.apogee.job;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JobResultRepository {

    void save(JobResult result);

    Optional<JobResult> latest(String scriptId);

    void deleteOlderThan(LocalDateTime cutoff);
}
