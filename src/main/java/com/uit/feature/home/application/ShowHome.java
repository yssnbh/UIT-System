package com.uit.feature.home.application;

/**
 * Use case for the Home screen. Business actions stay here, away from the JavaFX controller.
 */
public final class ShowHome {

    private final String username;

    public ShowHome(String username) {
        this.username = username;
    }

    public String execute() {
        return "Welcome back, " + username + ".";
    }
}
