package com.uit.feature.apogee.job;

import com.uit.feature.login.infrastructure.SqliteDatabase;
import com.uit.feature.settings.domain.ApogeeSettings;
import com.uit.shared.exception.AppException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public final class SshDumpStore implements RemoteDumpStore {

    private final RememberedServerKeys hostKeys = new RememberedServerKeys(SqliteDatabase.directory().resolve("known-hosts"));

    @Override
    public List<RemoteDump> list(ApogeeSettings settings, String directory) {
        return connect(settings, sftp -> {
            List<RemoteDump> files = new ArrayList<>();
            for (RemoteResourceInfo info : sftp.ls(directory)) {
                if (info.isDirectory()) {
                    continue;
                }
                files.add(new RemoteDump(info.getName(), info.getAttributes().getSize()));
            }
            return files;
        });
    }

    @Override
    public String readText(ApogeeSettings settings, String path) {
        return connect(settings, sftp -> {
            try (var remote = sftp.open(path)) {
                long length = Math.min(remote.length(), 2_000_000);
                byte[] bytes = new byte[(int) length];
                int offset = 0;
                while (offset < bytes.length) {
                    int read = remote.read(offset, bytes, offset, bytes.length - offset);
                    if (read < 0) {
                        break;
                    }
                    offset += read;
                }
                return new String(bytes, 0, offset, StandardCharsets.UTF_8);
            }
        });
    }

    @Override
    public void download(ApogeeSettings settings, String remotePath, Path localFile, DoubleConsumer fraction) {
        connect(settings, sftp -> {
            try (var remote = sftp.open(remotePath);
                 OutputStream output = Files.newOutputStream(localFile)) {
                long size = Math.max(remote.length(), 1);
                byte[] buffer = new byte[256 * 1024];
                long position = 0;
                int read;
                while ((read = remote.read(position, buffer, 0, buffer.length)) > 0) {
                    output.write(buffer, 0, read);
                    position += read;
                    fraction.accept(Math.min(1, position / (double) size));
                }
            }
            return null;
        });
    }

    private <T> T connect(ApogeeSettings settings, Transfer<T> transfer) {
        int port;
        try {
            port = Integer.parseInt(settings.port());
        } catch (NumberFormatException exception) {
            throw new AppException("Server port is not valid");
        }
        try (SSHClient client = new SSHClient()) {
            client.addHostKeyVerifier(hostKeys);
            client.setConnectTimeout(15_000);
            client.setTimeout(30_000);
            client.connect(settings.serverIp(), port);
            client.authPassword(settings.rootUsername(), settings.rootPassword());
            try (SFTPClient sftp = client.newSFTPClient()) {
                return transfer.apply(sftp);
            }
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            String message = exception.getMessage();
            throw new AppException(message == null || message.isBlank() ? "Unable to reach the server" : message, exception);
        }
    }

    private interface Transfer<T> {
        T apply(SFTPClient sftp) throws Exception;
    }
}
