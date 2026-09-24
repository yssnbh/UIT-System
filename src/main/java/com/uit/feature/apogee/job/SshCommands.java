package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.shared.exception.AppException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class SshCommands implements RemoteShell {

    private final RememberedServerKeys hostKeys = new RememberedServerKeys(SqliteDatabase.directory().resolve("known-hosts"));

    @Override
    public String run(String host, int port, String username, String password, String command) {
        try (SSHClient client = new SSHClient()) {
            client.addHostKeyVerifier(hostKeys);
            client.setConnectTimeout(15_000);
            client.setTimeout(30_000);
            client.connect(host, port);
            client.authPassword(username, password);
            try (Session session = client.startSession()) {
                Command remote = session.exec(command);
                String output = new String(remote.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                remote.join(30, TimeUnit.SECONDS);
                Integer status = remote.getExitStatus();
                if (status != null && status != 0) {
                    String error = new String(remote.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    throw new AppException(error.isBlank() ? "The server command failed" : error);
                }
                return output;
            }
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            String message = exception.getMessage();
            throw new AppException(message == null || message.isBlank() ? "Unable to reach the server" : message, exception);
        }
    }
}
