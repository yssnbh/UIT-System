package com.uit.feature.apogee.job;

public interface RemoteShell {

    String run(String host, int port, String username, String password, String command);
}
