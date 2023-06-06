package com.github.gavvydizzle.petsplugin.storage;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceProvider {

    public static DataSource initMySQLDataSource(Plugin plugin, Database database) throws SQLException {
        // SQL connection pool data source for MySQL.
        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();

        // Set credentials
        dataSource.setServerName(database.getHost());
        dataSource.setPassword(database.getPassword());
        dataSource.setPortNumber(database.getPort());
        dataSource.setUser(database.getUser());
        dataSource.setDatabaseName(database.getDatabase());

        // Test connection
        testDataSource(plugin, dataSource);

        return dataSource;
    }

    private static void testDataSource(Plugin plugin, DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
        if (plugin != null) {
            plugin.getLogger().info("§2Database connection established.");
        }
    }
}