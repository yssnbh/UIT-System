package com.uit.app.navigation;

import javafx.scene.Parent;

/**
 * One navigable page. A feature supplies an implementation and the shell displays it.
 */
public interface Screen {

    String id();

    String title();

    Parent createView();
}
