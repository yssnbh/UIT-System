package com.uit.feature.settings.application;

import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettingsRepository;

public final class LoadApogeeSettings {

    private final ApogeeSettingsRepository repository;

    public LoadApogeeSettings(ApogeeSettingsRepository repository) {
        this.repository = repository;
    }

    public ApogeeSettings execute() {
        return repository.load();
    }
}
