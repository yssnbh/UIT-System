package com.uit.feature.apogee.ui;

import com.uit.feature.apogee.job.ExportSettings;
import com.uit.feature.apogee.job.PartitionRule;
import com.uit.feature.apogee.job.ScheduleFrequency;
import com.uit.feature.apogee.job.ScheduleSlot;
import com.uit.feature.apogee.job.TablespaceMetric;
import com.uit.feature.apogee.job.TablespaceRule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ScheduleEditor {

    private ScheduleEditor() {
    }

    public static Optional<ScriptEdit> show(
            Window owner,
            String scriptName,
            List<ScheduleSlot> current,
            List<TablespaceRule> conditions,
            List<PartitionRule> partitions,
            ExportSettings export
    ) {
        Dialog<ScriptEdit> dialog = new Dialog<>();
        dialog.setTitle(scriptName);
        dialog.setHeaderText(null);
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().add(
                ScheduleEditor.class.getResource("/com/uit/app/shell/app.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        save.setText("Save");
        save.getStyleClass().add("primary-button");

        VBox times = new VBox(10);
        List<TimeRow> rows = new ArrayList<>();
        for (ScheduleSlot slot : current) {
            rows.add(addRow(times, rows, slot));
        }
        if (rows.isEmpty()) {
            rows.add(addRow(times, rows, ScheduleSlot.daily(LocalTime.of(8, 0))));
        }
        refreshRemove(rows);

        Button add = new Button("Add time");
        add.getStyleClass().add("job-action");
        add.setOnAction(event -> {
            rows.add(addRow(times, rows, ScheduleSlot.daily(LocalTime.of(8, 0))));
            refreshRemove(rows);
        });

        VBox page = new VBox(14, times, add);
        page.setPadding(new Insets(4, 0, 0, 0));
        page.setFillWidth(true);
        VBox.setVgrow(page, Priority.ALWAYS);

        Tab schedule = new Tab("Schedule", page);
        schedule.setClosable(false);
        List<ConditionRow> conditionRows = new ArrayList<>();
        List<PartitionRow> partitionRows = new ArrayList<>();
        TabPane tabs = new TabPane(schedule);
        if (conditions != null) {
            tabs.getTabs().add(conditionsTab(conditions, conditionRows));
        }
        if (partitions != null) {
            tabs.getTabs().add(partitionTab(partitions, partitionRows));
        }
        TextField exportPath = new TextField();
        TextField localPath = new TextField();
        Spinner<Integer> exportSize = new Spinner<>(1, 999, export == null ? 15 : export.expectedGigabytes());
        if (export != null) {
            tabs.getTabs().add(exportTab(export, exportPath, exportSize, localPath));
        }
        tabs.getStyleClass().add("settings-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setHgrow(tabs, Priority.ALWAYS);
        GridPane.setVgrow(tabs, Priority.ALWAYS);
        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().setPrefSize(560, 420);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            List<ScheduleSlot> chosen = new ArrayList<>();
            for (TimeRow row : rows) {
                chosen.add(row.slot());
            }
            List<TablespaceRule> chosenRules = null;
            if (conditions != null) {
                chosenRules = new ArrayList<>();
                for (ConditionRow row : conditionRows) {
                    chosenRules.add(row.rule());
                }
            }
            List<PartitionRule> chosenPartitions = null;
            if (partitions != null) {
                chosenPartitions = new ArrayList<>();
                for (PartitionRow row : partitionRows) {
                    chosenPartitions.add(row.rule());
                }
            }
            ExportSettings chosenExport = null;
            if (export != null) {
                chosenExport = new ExportSettings(exportPath.getText().trim(), exportSize.getValue(), localPath.getText().trim());
            }
            return new ScriptEdit(chosen, chosenRules, chosenPartitions, chosenExport);
        });
        return dialog.showAndWait();
    }

    private static Tab conditionsTab(List<TablespaceRule> conditions, List<ConditionRow> rows) {
        VBox list = new VBox(10);
        for (TablespaceRule rule : conditions) {
            ConditionRow row = new ConditionRow(rule);
            rows.add(row);
            list.getChildren().add(row.box());
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");
        VBox page = new VBox(scroll);
        page.setPadding(new Insets(4, 0, 0, 0));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Tab tab = new Tab("Conditions", page);
        tab.setClosable(false);
        return tab;
    }

    private static Tab partitionTab(List<PartitionRule> partitions, List<PartitionRow> rows) {
        VBox list = new VBox(10);
        for (PartitionRule rule : partitions) {
            PartitionRow row = new PartitionRow(rule);
            rows.add(row);
            list.getChildren().add(row.box());
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");
        VBox page = new VBox(scroll);
        page.setPadding(new Insets(4, 0, 0, 0));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Tab tab = new Tab("Conditions", page);
        tab.setClosable(false);
        return tab;
    }

    private static Tab exportTab(ExportSettings export, TextField path, Spinner<Integer> size, TextField localPath) {
        path.setText(export.path());
        path.getStyleClass().add("condition-label");
        size.setEditable(true);
        size.setPrefWidth(90);
        Label pathLabel = new Label("Folder in server");
        pathLabel.getStyleClass().add("field-label");
        Label sizeLabel = new Label("Expected size");
        sizeLabel.getStyleClass().add("field-label");
        Label sizeSubtitle = new Label("Expected file dmp of backup");
        sizeSubtitle.getStyleClass().add("field-subtitle");
        Label unit = new Label("GB");
        unit.getStyleClass().add("condition-label");
        HBox sizeRow = new HBox(8, size, unit);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        localPath.setText(export.localPath());
        Label localLabel = new Label("Folder on this PC");
        localLabel.getStyleClass().add("field-label");
        Label localSubtitle = new Label("Where the downloaded dumps are kept, for example D:\\UIT backups");
        localSubtitle.getStyleClass().add("field-subtitle");
        localSubtitle.setWrapText(true);
        VBox folder = new VBox(6, pathLabel, path);
        VBox expected = new VBox(6, sizeLabel, sizeSubtitle, sizeRow);
        VBox local = new VBox(6, localLabel, localSubtitle, localPath);
        VBox page = new VBox(14, folder, expected, local);
        page.setPadding(new Insets(8, 0, 0, 0));
        Tab tab = new Tab("Export", page);
        tab.setClosable(false);
        return tab;
    }

    private static TimeRow addRow(VBox times, List<TimeRow> rows, ScheduleSlot slot) {
        TimeRow row = new TimeRow(slot, () -> {
            rows.removeIf(candidate -> candidate.box().getParent() == null);
            refreshRemove(rows);
        });
        times.getChildren().add(row.box());
        return row;
    }

    private static void refreshRemove(List<TimeRow> rows) {
        boolean onlyOne = rows.size() < 2;
        for (TimeRow row : rows) {
            row.remove().setDisable(onlyOne);
        }
    }

    private static final class TimeRow {
        private final HBox box = new HBox(8);
        private final ComboBox<ScheduleFrequency> frequency = new ComboBox<>();
        private final ComboBox<Integer> day = new ComboBox<>();
        private final Spinner<Integer> hour;
        private final Spinner<Integer> minute;
        private final Button remove = new Button("\u2715");

        private TimeRow(ScheduleSlot slot, Runnable onRemove) {
            frequency.getItems().addAll(ScheduleFrequency.DAILY, ScheduleFrequency.WEEKLY, ScheduleFrequency.MONTHLY);
            frequency.setConverter(new StringConverter<>() {
                @Override
                public String toString(ScheduleFrequency value) {
                    if (value == null) {
                        return "";
                    }
                    return switch (value) {
                        case DAILY -> "Daily";
                        case WEEKLY -> "Weekly";
                        case MONTHLY -> "Monthly";
                    };
                }

                @Override
                public ScheduleFrequency fromString(String text) {
                    return null;
                }
            });
            frequency.setValue(slot.frequency());
            hour = spinner(0, 23, slot.time().getHour());
            minute = spinner(0, 59, slot.time().getMinute());
            showDay(slot.frequency(), slot.day());
            frequency.setOnAction(event -> showDay(frequency.getValue(), day.getValue() == null ? 1 : day.getValue()));

            FontIcon graphic = new FontIcon(MaterialDesignC.CLOSE);
            graphic.setIconSize(14);
            graphic.setIconColor(Color.web("#1e3a5f"));
            remove.setGraphic(graphic);
            remove.setText(null);
            remove.getStyleClass().add("job-action");
            remove.setFocusTraversable(false);
            remove.setOnAction(event -> {
                ((VBox) box.getParent()).getChildren().remove(box);
                onRemove.run();
            });

            Label at = new Label("at");
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(frequency, day, at, hour, new Label(":"), minute, remove);
            HBox.setHgrow(frequency, Priority.NEVER);
        }

        private HBox box() {
            return box;
        }

        private Button remove() {
            return remove;
        }

        private ScheduleSlot slot() {
            LocalTime time = LocalTime.of(hour.getValue(), minute.getValue());
            return switch (frequency.getValue()) {
                case DAILY -> ScheduleSlot.daily(time);
                case WEEKLY -> ScheduleSlot.weekly(DayOfWeek.of(day.getValue()), time);
                case MONTHLY -> ScheduleSlot.monthly(day.getValue(), time);
            };
        }

        private void showDay(ScheduleFrequency chosen, int current) {
            boolean needsDay = chosen != ScheduleFrequency.DAILY;
            day.setVisible(needsDay);
            day.setManaged(needsDay);
            if (!needsDay) {
                return;
            }
            day.getItems().clear();
            if (chosen == ScheduleFrequency.WEEKLY) {
                day.setConverter(weekday());
                for (int value = 1; value <= 7; value++) {
                    day.getItems().add(value);
                }
                day.setValue(current >= 1 && current <= 7 ? current : 1);
                return;
            }
            day.setConverter(number());
            for (int value = 1; value <= 31; value++) {
                day.getItems().add(value);
            }
            day.setValue(current >= 1 && current <= 31 ? current : 1);
        }

        private static Spinner<Integer> spinner(int min, int max, int value) {
            Spinner<Integer> spinner = new Spinner<>(min, max, value);
            spinner.setEditable(true);
            spinner.setPrefWidth(72);
            return spinner;
        }

        private static StringConverter<Integer> weekday() {
            return new StringConverter<>() {
                @Override
                public String toString(Integer value) {
                    if (value == null) {
                        return "";
                    }
                    return DayOfWeek.of(value).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                }

                @Override
                public Integer fromString(String text) {
                    return null;
                }
            };
        }

        private static StringConverter<Integer> number() {
            return new StringConverter<>() {
                @Override
                public String toString(Integer value) {
                    return value == null ? "" : Integer.toString(value);
                }

                @Override
                public Integer fromString(String text) {
                    return null;
                }
            };
        }
    }

    private static final class ConditionRow {
        private final HBox box = new HBox(8);
        private final String tablespace;
        private final TablespaceMetric metric;
        private final Spinner<Integer> amount;
        private final ComboBox<String> unit = new ComboBox<>();

        private ConditionRow(TablespaceRule rule) {
            tablespace = rule.tablespace();
            metric = rule.metric();
            boolean gigabytes = rule.megabytes() >= 1024 && rule.megabytes() % 1024 == 0;
            int shown = gigabytes ? rule.megabytes() / 1024 : rule.megabytes();
            amount = new Spinner<>(1, 99999, shown);
            amount.setEditable(true);
            amount.setPrefWidth(90);
            unit.getItems().addAll("MB", "GB");
            unit.setValue(gigabytes ? "GB" : "MB");
            Label name = new Label(tablespace);
            name.setMinWidth(110);
            name.getStyleClass().add("condition-label");
            Label ruleText = new Label(metric == TablespaceMetric.FREE ? "free space >=" : "size <=");
            ruleText.setMinWidth(110);
            ruleText.getStyleClass().add("condition-label");
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(name, ruleText, amount, unit);
        }

        private HBox box() {
            return box;
        }

        private TablespaceRule rule() {
            int value = amount.getValue();
            int megabytes = "GB".equals(unit.getValue()) ? value * 1024 : value;
            return new TablespaceRule(tablespace, metric, megabytes);
        }
    }

    private static final class PartitionRow {
        private final HBox box = new HBox(8);
        private final String mount;
        private final Spinner<Integer> percent;

        private PartitionRow(PartitionRule rule) {
            mount = rule.mount();
            percent = new Spinner<>(0, 100, rule.maxPercent());
            percent.setEditable(true);
            percent.setPrefWidth(90);
            Label name = new Label(rule.others() ? "Other partitions" : mount);
            name.setMinWidth(140);
            name.getStyleClass().add("condition-label");
            Label ruleText = new Label("used space <=");
            ruleText.getStyleClass().add("condition-label");
            Label unit = new Label("%");
            unit.getStyleClass().add("condition-label");
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(name, ruleText, percent, unit);
        }

        private HBox box() {
            return box;
        }

        private PartitionRule rule() {
            return new PartitionRule(mount, percent.getValue());
        }
    }
}
