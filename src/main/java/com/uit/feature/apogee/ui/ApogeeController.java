package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.application.AccountUnlockResult;
import com.uit.feature.apogee.application.LockAccounts;
import com.uit.feature.apogee.application.LockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockAccounts;
import com.uit.feature.apogee.application.UnlockApogeeAccounts;
import com.uit.feature.apogee.application.UnlockStatistics;
import com.uit.feature.apogee.job.JobsUpdated;
import com.uit.feature.apogee.job.ResultTone;
import com.uit.feature.apogee.job.ScriptResultLine;
import com.uit.shared.event.EventBus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Popup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public final class ApogeeController {

    private final UnlockApogeeAccounts unlockApogeeAccounts;
    private final LockApogeeAccounts lockApogeeAccounts;
    private final JobsViewModel jobsViewModel;
    private final EventBus events;
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
    @FXML
    private TableView<JobRow> jobs;

    public ApogeeController(
            UnlockApogeeAccounts unlockApogeeAccounts,
            LockApogeeAccounts lockApogeeAccounts,
            JobsViewModel jobsViewModel,
            EventBus events
    ) {
        this.unlockApogeeAccounts = unlockApogeeAccounts;
        this.lockApogeeAccounts = lockApogeeAccounts;
        this.jobsViewModel = jobsViewModel;
        this.events = events;
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
        configureJobs();
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

    private void configureJobs() {
        TableColumn<JobRow, String> script = column("Script", JobRow::name, 180);
        TableColumn<JobRow, String> when = column("Run", JobRow::when, 150);
        TableColumn<JobRow, String> last = column("Last execute", JobRow::lastExecute, 150);
        TableColumn<JobRow, String> success = column("Success", JobRow::success, 100);
        success.setCellFactory(columnView -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("job-ok", "job-danger");
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add("Failed".equals(item) ? "job-danger" : "job-ok");
            }
        });
        TableColumn<JobRow, JobRow> result = new TableColumn<>("Result");
        result.setPrefWidth(360);
        result.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        result.setCellFactory(columnView -> new TableCell<>() {
            private final Button details = new Button("Details");
            private final Circle dot = new Circle(8);
            private final Label summary = new Label();
            private final HBox content = new HBox(10, details, dot, summary);
            private final VBox lines = new VBox(6);
            private final Popup tip = new Popup();

            {
                setAlignment(Pos.CENTER_LEFT);
                content.setAlignment(Pos.CENTER_LEFT);
                details.getStyleClass().add("job-action");
                details.setFocusTraversable(false);
                lines.setStyle("""
                        -fx-background-color: #fffef8;
                        -fx-background-radius: 6;
                        -fx-border-color: #cbd5e1;
                        -fx-border-radius: 6;
                        -fx-padding: 10 12;
                        """);
                lines.setMaxWidth(480);
                tip.getContent().add(lines);
                tip.setAutoHide(true);
                details.setOnAction(event -> {
                    if (tip.isShowing()) {
                        tip.hide();
                        return;
                    }
                    Bounds bounds = details.localToScreen(details.getBoundsInLocal());
                    tip.show(details, bounds.getMinX(), bounds.getMaxY() + 6);
                });
            }

            @Override
            protected void updateItem(JobRow item, boolean empty) {
                super.updateItem(item, empty);
                tip.hide();
                if (empty || item == null || item.result().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    applyRowHeight(getTableRow(), "result", 0);
                    return;
                }
                int problems = 0;
                lines.getChildren().clear();
                for (ScriptResultLine line : item.result()) {
                    boolean problem = line.tone() == ResultTone.DANGER;
                    if (problem) {
                        problems++;
                    }
                    Circle mark = new Circle(4);
                    mark.setFill(Color.web(problem ? "#b91c1c" : "#15803d"));
                    Label text = new Label(line.text());
                    text.setWrapText(true);
                    text.setMaxWidth(440);
                    text.setTextFill(Color.web(problem ? "#b91c1c" : "#0f172a"));
                    HBox row = new HBox(8, mark, text);
                    row.setAlignment(Pos.CENTER_LEFT);
                    lines.getChildren().add(row);
                }
                boolean good = problems == 0;
                dot.setFill(Color.web(good ? "#15803d" : "#b91c1c"));
                summary.getStyleClass().removeAll("job-ok", "job-danger");
                summary.getStyleClass().add(good ? "job-ok" : "job-danger");
                summary.setText(good ? "Everything is good" : problems + (problems == 1 ? " problem" : " problems"));
                setGraphic(content);
                applyRowHeight(getTableRow(), "result", 44);
            }
        });
        when.setPrefWidth(220);
        when.setCellFactory(columnView -> new TableCell<>() {
            private final Label text = new Label();

            {
                setAlignment(Pos.TOP_LEFT);
                text.setWrapText(true);
                widthProperty().addListener((obs, oldWidth, newWidth) -> fitWhen());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    applyRowHeight(getTableRow(), "when", 0);
                    return;
                }
                text.setText(item);
                setGraphic(text);
                fitWhen();
            }

            private void fitWhen() {
                if (getGraphic() == null || getTableRow() == null) {
                    return;
                }
                double width = getWidth() - snappedLeftInset() - snappedRightInset();
                double contentWidth = width > 0 ? width : -1;
                if (contentWidth > 0) {
                    text.setPrefWidth(contentWidth);
                }
                applyRowHeight(getTableRow(), "when", text.prefHeight(contentWidth) + 8);
            }
        });
        jobs.getColumns().setAll(actionsColumn(), script, when, last, success, result);
        jobs.setFixedCellSize(Region.USE_COMPUTED_SIZE);
        jobs.setItems(jobsViewModel.rows());
        jobs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        jobsViewModel.refresh();
        events.subscribe(JobsUpdated.class, event -> Platform.runLater(jobsViewModel::refresh));
    }

    private TableColumn<JobRow, JobRow> actionsColumn() {
        TableColumn<JobRow, JobRow> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(128);
        actions.setMinWidth(128);
        actions.setMaxWidth(128);
        actions.setSortable(false);
        actions.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        actions.setCellFactory(columnView -> new TableCell<>() {
            private final Button execute = iconButton(MaterialDesignP.PLAY, "Execute now");
            private final Button edit = iconButton(MaterialDesignP.PENCIL, "Edit");
            private final HBox buttons = new HBox(4, execute, edit);

            {
                buttons.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(JobRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                execute.setOnAction(event -> jobsViewModel.executeNow(item.scriptId()));
                edit.setOnAction(event -> editSchedule(item));
                setGraphic(buttons);
            }
        });
        return actions;
    }

    private void editSchedule(JobRow item) {
        boolean tablespaces = "check-tablespaces".equals(item.scriptId());
        boolean serverSpace = "check-server-space".equals(item.scriptId());
        ScheduleEditor.show(
                jobs.getScene().getWindow(),
                item.name(),
                jobsViewModel.schedule(item.scriptId()),
                tablespaces ? jobsViewModel.tablespaceRules() : null,
                serverSpace ? jobsViewModel.partitionRules() : null
        ).ifPresent(edit -> {
            jobsViewModel.saveSchedule(item.scriptId(), edit.times());
            if (edit.conditions() != null) {
                jobsViewModel.saveTablespaceRules(edit.conditions());
            }
            if (edit.partitions() != null) {
                jobsViewModel.savePartitionRules(edit.partitions());
            }
        });
    }

    private static void applyRowHeight(TableRow<?> row, String part, double contentHeight) {
        if (row == null) {
            return;
        }
        row.getProperties().put(part, contentHeight);
        double result = heightOf(row, "result");
        double when = heightOf(row, "when");
        double height = Math.max(36, Math.max(result, when));
        if (Math.abs(row.getPrefHeight() - height) < 1) {
            return;
        }
        row.setMinHeight(height);
        row.setPrefHeight(height);
        row.setMaxHeight(height);
    }

    private static double heightOf(TableRow<?> row, String part) {
        Object stored = row.getProperties().get(part);
        return stored instanceof Double value ? value : 0;
    }

    private static Button iconButton(org.kordamp.ikonli.Ikon icon, String action) {
        FontIcon graphic = new FontIcon(icon);
        graphic.setIconSize(16);
        graphic.setIconColor(Color.web("#1e3a5f"));
        Button button = new Button();
        button.setGraphic(graphic);
        button.getStyleClass().add("job-action");
        button.setFocusTraversable(false);
        Tooltip tooltip = new Tooltip(action);
        tooltip.setShowDelay(Duration.millis(200));
        button.setTooltip(tooltip);
        return button;
    }

    private static TableColumn<JobRow, String> column(String title, java.util.function.Function<JobRow, String> value, double width) {
        TableColumn<JobRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        return column;
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
