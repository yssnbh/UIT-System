package com.uit.feature.settings.ui;

import com.uit.feature.settings.domain.ApogeeSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class SettingsController {

    private final ApogeeSettingsViewModel viewModel;

    @FXML
    private TextField serverIp;
    @FXML
    private TextField port;
    @FXML
    private TextField rootUsername;
    @FXML
    private PasswordField rootPassword;
    @FXML
    private TextField oracleUsername;
    @FXML
    private PasswordField oraclePassword;
    @FXML
    private PasswordField apogeeUser;
    @FXML
    private PasswordField adminUser;
    @FXML
    private PasswordField sysUser;
    @FXML
    private PasswordField systemUser;
    @FXML
    private PasswordField sysmanUser;
    @FXML
    private TextField databaseIp;
    @FXML
    private TextField databasePort;
    @FXML
    private TextField databaseSid;
    @FXML
    private Label status;

    public SettingsController(ApogeeSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    private void initialize() {
        status.textProperty().bind(viewModel.statusProperty());
        show(viewModel.load());
    }

    @FXML
    private void save() {
        viewModel.save(new ApogeeSettings(
                serverIp.getText(),
                port.getText(),
                rootUsername.getText(),
                rootPassword.getText(),
                oracleUsername.getText(),
                oraclePassword.getText(),
                apogeeUser.getText(),
                adminUser.getText(),
                sysUser.getText(),
                systemUser.getText(),
                sysmanUser.getText(),
                databaseIp.getText(),
                databasePort.getText(),
                databaseSid.getText()
        ));
    }

    private void show(ApogeeSettings settings) {
        serverIp.setText(settings.serverIp());
        port.setText(settings.port());
        rootUsername.setText(settings.rootUsername());
        rootPassword.setText(settings.rootPassword());
        oracleUsername.setText(settings.oracleUsername());
        oraclePassword.setText(settings.oraclePassword());
        apogeeUser.setText(settings.apogeePassword());
        adminUser.setText(settings.adminPassword());
        sysUser.setText(settings.sysPassword());
        systemUser.setText(settings.systemPassword());
        sysmanUser.setText(settings.sysmanPassword());
        databaseIp.setText(settings.databaseIp());
        databasePort.setText(settings.databasePort());
        databaseSid.setText(settings.databaseSid());
    }
}
