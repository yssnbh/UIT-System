package com.uit.feature.apogee.job;

import com.uit.shared.event.AppEvent;

public record DownloadProgressed(String scriptId, int index, int total, double fraction) implements AppEvent {
}
