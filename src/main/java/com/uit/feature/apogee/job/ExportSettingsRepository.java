package com.uit.feature.apogee.job;

public interface ExportSettingsRepository {

    ExportSettings load();

    void save(ExportSettings settings);
}
