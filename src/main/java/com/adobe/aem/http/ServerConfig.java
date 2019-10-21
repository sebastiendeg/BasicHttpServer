package com.adobe.aem.http;

import com.google.inject.Singleton;

/**
 * Holds configuration.
 *
 * 1. Central place to change default value
 * 2. Values can move to a config file easily.
 */

@Singleton
public class ServerConfig {

    private static final int PORT = 8080;
    private static final int THREAD_COUNT = 10;
    private static final String ROOT_FOLDER = "/";

    public int getPort() {
        return PORT;
    }

    public int getThreadCount() {
        return THREAD_COUNT;
    }

    public String getRootFolder() {
        return ROOT_FOLDER;
    }
}
