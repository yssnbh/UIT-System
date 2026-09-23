package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.application.AccountUnlockResult;
import com.uit.feature.apogee.application.LockAccounts;
import com.uit.feature.apogee.application.LockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockAccounts;
import com.uit.feature.apogee.application.UnlockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockStatistics;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public final class ApogeeController {

    private final UnlockApogeeAccounts unlockApogeeAccounts;
    private final LockApogeeAccounts lockApogeeAccounts;
    private boolean busy;

    @FXML
    private TextField usernames;
    @FXML
    private TextField password;
    @FXML
    private Button unlock;
    @FXML
    private VBox statistics;
    @FXML
    private Label unlockedCount;
    @FXML
    private Label missingCount;
    @FXML
    private Label errorCount;
    @FXML
    private TextArea result;
    @FXML
    private TextField lockUsernames;
    @FXML
    private Button lock;
    @FXML
    private VBox lockStatistics;
    @FXML
    private Label lockedCount;
    @FXML
    private Label lockMissingCount;
    @FXML
    private Label lockErrorCount;
    @FXML
    private TextArea lockResult;

    public ApogeeController(UnlockApogeeAccounts unlockApogeeAccounts, LockApogeeAccounts lockApogeeAccounts) {
        this.unlockApogeeAccounts = unlockApogeeAccounts;
        this.lockApogeeAccounts = lockApogeeAccounts;
    }

    @FXML
    private void initialize() {
        usernames.textProperty().addListener((obs, previous, value) -> {
            keepUppercase(usernames, value);
            refreshUnlockButton();
        });
        password.textProperty().addListener((obs, previous, value) -> refreshUnlockButton());
        lockUsernames.textProperty().addListener((obs, previous, value) -> {
            keepUppercase(lockUsernames, value);
            refreshLockButton();
        });
        refreshUnlockButton();
        refreshLockButton();
    }

    @FXML
    private void unlockAccounts() {
        List<String> names = UnlockAccounts.usernames(usernames.getText());
        String newPassword = password.getText();
        if (names.isEmpty() || newPassword.isBlank() || busy) {
            return;
        }
        busy = true;
        refreshUnlockButton();
        refreshLockButton();
        showStatistics(new UnlockStatistics(0, 0, 0), "Connexion...");
        Task<List<AccountUnlockResult>> task = new Task<>() {
            @Override
            protected List<AccountUnlockResult> call() {
                return unlockApogeeAccounts.execute(names, newPassword);
            }
        };
        task.setOnSucceeded(event -> {
            List<AccountUnlockResult> results = task.getValue();
            showStatistics(UnlockStatistics.from(results), format(results));
            busy = false;
            refreshUnlockButton();
            refreshLockButton();
            refreshLockButton();
        });
        task.setOnFailed(event -> {
            showStatistics(new UnlockStatistics(0, 0, 1), errorMessage(task.getException()));
            busy = false;
            refreshUnlockButton();
            refreshLockButton();
        });
        Thread thread = new Thread(task, "apogee-unlock");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void lockAccounts() {
        List<String> names = UnlockAccounts.usernames(lockUsernames.getText());
        if (names.isEmpty() || busy) {
            return;
        }
        busy = true;
        refreshUnlockButton();
        refreshLockButton();
        showLockStatistics(new UnlockStatistics(0, 0, 0), "Connexion...");
        Task<List<AccountUnlockResult>> task = new Task<>() {
            @Override
            protected List<AccountUnlockResult> call() {
                return lockApogeeAccounts.execute(names);
            }
        };
        task.setOnSucceeded(event -> {
            List<AccountUnlockResult> results = task.getValue();
            showLockStatistics(UnlockStatistics.from(results), format(results));
            busy = false;
            refreshUnlockButton();
            refreshLockButton();
        });
        task.setOnFailed(event -> {
            showLockStatistics(new UnlockStatistics(0, 0, 1), errorMessage(task.getException()));
            busy = false;
            refreshUnlockButton();
            refreshLockButton();
        });
        Thread thread = new Thread(task, "apogee-lock");
        thread.setDaemon(true);
        thread.start();
    }

    private void keepUppercase(TextField field, String value) {
        if (value == null) {
            return;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.equals(value)) {
            return;
        }
        int caret = field.getCaretPosition();
        field.setText(upper);
        field.positionCaret(Math.min(caret, upper.length()));
    }

    private void refreshUnlockButton() {
        unlock.setText(UnlockAccounts.buttonLabel(usernames.getText()));
        boolean missing = UnlockAccounts.usernames(usernames.getText()).isEmpty()
                || password.getText() == null
                || password.getText().isBlank();
        unlock.setDisable(busy || missing);
    }

    private void refreshLockButton() {
        lock.setText(LockAccounts.buttonLabel(lockUsernames.getText()));
        lock.setDisable(busy || UnlockAccounts.usernames(lockUsernames.getText()).isEmpty());
    }

    private void showLockStatistics(UnlockStatistics summary, String details) {
        lockedCount.setText(Integer.toString(summary.unlocked()));
        lockMissingCount.setText(Integer.toString(summary.missing()));
        lockErrorCount.setText(Integer.toString(summary.errors()));
        lockResult.setText(details);
        lockStatistics.setVisible(true);
        lockStatistics.setManaged(true);
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        if (cause.getMessage() == null || cause.getMessage().isBlank()) {
            return error.getMessage();
        }
        return cause.getMessage();
    }

    private void showStatistics(UnlockStatistics summary, String details) {
        unlockedCount.setText(Integer.toString(summary.unlocked()));
        missingCount.setText(Integer.toString(summary.missing()));
        errorCount.setText(Integer.toString(summary.errors()));
        result.setText(details);
        statistics.setVisible(true);
        statistics.setManaged(true);
    }

    private static String format(List<AccountUnlockResult> results) {
        StringBuilder text = new StringBuilder();
        for (AccountUnlockResult item : results) {
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(item.username()).append(": ").append(item.message());
        }
        return text.toString();
    }
}
