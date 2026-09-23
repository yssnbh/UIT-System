package com.uit.feature.settings.domain;

public interface ApogeeSettingsRepository {

    ApogeeSettings load();

    void save(ApogeeSettings settings);
}
