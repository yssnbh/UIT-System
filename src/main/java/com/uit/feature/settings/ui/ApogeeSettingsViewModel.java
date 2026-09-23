package com.uit.feature.settings.ui;

import com.uit.feature.settings.application.LoadApogeeSettings;
import com.uit.feature.settings.application.SaveApogeeSettings;
import com.uit.feature.settings.domain.ApogeeSettings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class ApogeeSettingsViewModel {

    private final LoadApogeeSettings loadApogeeSettings;
    private final SaveApogeeSettings saveApogeeSettings;
    private final StringProperty status = new SimpleStringProperty("");

    public ApogeeSettingsViewModel(LoadApogeeSettings loadApogeeSettings, SaveApogeeSettings saveApogeeSettings) {
        this.loadApogeeSettings = loadApogeeSettings;
        this.saveApogeeSettings = saveApogeeSettings;
    }

    public ApogeeSettings load() {
        status.set("");
        return loadApogeeSettings.execute();
    }

    public void save(ApogeeSettings settings) {
        saveApogeeSettings.execute(settings);
        status.set("Saved");
    }

    public StringProperty statusProperty() {
        return status;
    }
}
