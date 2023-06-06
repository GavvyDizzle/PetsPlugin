package com.github.gavvydizzle.petsplugin.storage;

import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DbSetup {

    public static void initDb(Plugin plugin, DataSource dataSource) throws SQLException, IOException {
        // Read setup file to create new table
        String setup;
        try (InputStream in = DbSetup.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            assert in != null;
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            throw e;
        }

        String[] queries = setup.split(";");
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            // execute each query to the database.
            for (String query : queries) {
                if (query.trim().isEmpty()) continue;
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.execute();
                }
            }
            conn.commit();
        }
        plugin.getLogger().info("§2Database setup complete.");
    }
}
