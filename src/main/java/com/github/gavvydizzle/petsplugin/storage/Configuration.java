package com.github.gavvydizzle.petsplugin.storage;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;

public class Configuration {

    private final Plugin plugin;
    private Database database;

    public Configuration(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("host", plugin.getConfig().getString("database.host"));
        map.put("port", plugin.getConfig().getInt("database.port"));
        map.put("user", plugin.getConfig().getString("database.user"));
        map.put("password", plugin.getConfig().getString("database.password"));
        map.put("database", plugin.getConfig().getString("database.database"));

        database = new Database(map);
    }

    public Database getDatabase() {
        return database;
    }
}