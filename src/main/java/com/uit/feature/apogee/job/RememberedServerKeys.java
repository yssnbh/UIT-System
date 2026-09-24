package com.uit.feature.apogee.job;

import com.uit.shared.exception.AppException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

/**
 * Remembers the server key the first time and rejects a later change.
 */
final class RememberedServerKeys implements HostKeyVerifier {

    private final Path file;

    RememberedServerKeys(Path file) {
        this.file = file;
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        String identity = hostname + ":" + port;
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (!Files.exists(file)) {
                Files.writeString(file, identity + " " + encoded + System.lineSeparator());
                return true;
            }
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(" ", 2);
                if (parts.length == 2 && parts[0].equals(identity)) {
                    return parts[1].equals(encoded);
                }
            }
            Files.writeString(file, identity + " " + encoded + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND);
            return true;
        } catch (IOException exception) {
            throw new AppException("Unable to verify the server", exception);
        }
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        return List.of();
    }
}
