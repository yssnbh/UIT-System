package com.uit.feature.apogee.job;

import java.time.LocalDateTime;

public record JobResult(String scriptId, LocalDateTime executedAt, boolean success, String storedResult) {
}
