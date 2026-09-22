package com.uit.feature.home.ui;

import com.uit.feature.home.application.ShowHome;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class HomeViewModel {

    private final StringProperty message = new SimpleStringProperty();

    public HomeViewModel(ShowHome showHome) {
        message.set(showHome.execute());
    }

    public StringProperty messageProperty() {
        return message;
    }
}
