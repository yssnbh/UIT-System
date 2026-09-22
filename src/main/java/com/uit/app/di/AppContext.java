package com.uit.app.di;

import com.uit.app.navigation.Navigator;
import com.uit.app.navigation.Screen;
import com.uit.feature.home.application.ShowHome;
import com.uit.feature.home.ui.HomeScreen;
import com.uit.feature.home.ui.HomeViewModel;
import com.uit.shared.event.EventBus;

import java.util.List;

/**
 * Composition root. Register each new feature by adding its screen here.
 */
public final class AppContext {

    private final String signedInUsername;
    private final EventBus eventBus;
    private final Navigator navigator;

    public AppContext(String signedInUsername) {
        this.signedInUsername = signedInUsername;
        eventBus = new EventBus();
        navigator = new Navigator(List.of(homeScreen()));
    }

    public Navigator navigator() {
        return navigator;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public String signedInUsername() {
        return signedInUsername;
    }

    private Screen homeScreen() {
        HomeViewModel viewModel = new HomeViewModel(new ShowHome(signedInUsername));
        return new HomeScreen(viewModel);
    }
}
