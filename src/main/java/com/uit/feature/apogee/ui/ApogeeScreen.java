package com.uit.feature.apogee.ui;

import com.uit.app.navigation.Screen;
import com.uit.feature.apogee.application.LockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockApogeeAccounts;
import com.uit.shared.exception.AppException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public final class ApogeeScreen implements Screen {

    private final UnlockApogeeAccounts unlockApogeeAccounts;
    private final LockApogeeAccounts lockApogeeAccounts;

    public ApogeeScreen(UnlockApogeeAccounts unlockApogeeAccounts, LockApogeeAccounts lockApogeeAccounts) {
        this.unlockApogeeAccounts = unlockApogeeAccounts;
        this.lockApogeeAccounts = lockApogeeAccounts;
    }

    @Override
    public String id() {
        return "apogee";
    }

    @Override
    public String title() {
        return "Apogee";
    }

    @Override
    public Parent createView() {
        URL layout = ApogeeScreen.class.getResource("/com/uit/feature/apogee/ui/apogee.fxml");
        if (layout == null) {
            throw new AppException("Missing Apogee screen layout");
        }
        FXMLLoader loader = new FXMLLoader(layout);
        loader.setController(new ApogeeController(unlockApogeeAccounts, lockApogeeAccounts));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new AppException("Unable to open Apogee", exception);
        }
    }
}
