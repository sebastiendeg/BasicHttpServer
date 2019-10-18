package com.adobe.aem.http;

/**
 * Holds configuration.
 *
 * 1. Central place to change default value
 * 2. Other class don't need to know that the values are static.
 * 3. TODO Should be injected as a singleton.
 */
public class ServerConfig {

    private static final int PORT = 8080;
    private static final int THREAD_COUNT = 1024;
    private static final String ROOT_FOLDER = "./";

    //basic singleton, doesn't need to be more complicated given that it doesn't consume resources.
    private static final ServerConfig _instance = new ServerConfig();

    private ServerConfig() {
    }

    public static ServerConfig getInstance() {
        return _instance;
    }

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
