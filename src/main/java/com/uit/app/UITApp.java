package com.uit.app;

import com.uit.app.di.AppContext;
import com.uit.app.shell.ShellController;
import com.uit.feature.login.domain.User;
import com.uit.feature.login.domain.UserRepository;
import com.uit.feature.login.infrastructure.GoogleAuthConfig;
import com.uit.feature.login.infrastructure.GoogleOAuthClient;
import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.login.infrastructure.SqliteUserRepository;
import com.uit.feature.login.ui.LoginWindow;
import com.uit.shared.exception.AppException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class UITApp extends Application {

    private SqliteDatabase database;

    @Override
    public void start(Stage stage) {
        database = SqliteDatabase.openDefault();
        UserRepository users = new SqliteUserRepository(database);
        GoogleOAuthClient google = new GoogleOAuthClient(GoogleAuthConfig.load());
        new LoginWindow(users, google).open(stage, user -> openWorkspace(stage, user));
    }

    private void openWorkspace(Stage stage, User user) {
        AppContext context = new AppContext(user.username(), database);
        Parent shell = loadShell(context);
        Scene scene = new Scene(shell);
        scene.getStylesheets().add(stylesheet());

        double width = stage.getWidth();
        double height = stage.getHeight();
        stage.setTitle("UIT System");
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    private static Parent loadShell(AppContext context) {
        URL layout = UITApp.class.getResource("/com/uit/app/shell/shell.fxml");
        if (layout == null) {
            throw new AppException("Missing application shell layout");
        }
        FXMLLoader loader = new FXMLLoader(layout);
        loader.setController(new ShellController(context.navigator(), context.signedInUsername()));
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new AppException("Unable to open the application shell", exception);
        }
    }

    private static String stylesheet() {
        URL stylesheet = UITApp.class.getResource("/com/uit/app/shell/app.css");
        return Objects.requireNonNull(stylesheet, "Missing application stylesheet").toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
