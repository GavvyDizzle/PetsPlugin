package com.github.gavvydizzle.petsplugin.storage;

import java.util.Map;

public class Database {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;

    public Database(Map<String, Object> map) {
        host = (String) map.getOrDefault("host", "localhost");
        port = (int) map.getOrDefault("port", "3306");
        user = (String) map.getOrDefault("user", "root");
        password = (String) map.getOrDefault("password", "passy");
        database = (String) map.getOrDefault("database", "db");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase(){ return database; }
}
