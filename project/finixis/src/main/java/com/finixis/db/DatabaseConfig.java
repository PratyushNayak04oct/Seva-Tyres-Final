package com.finixis.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton that owns the HikariCP connection pool.
 * Reads connection settings from application.properties.
 * Call {@link #init()} once at application startup,
 * {@link #get()} to borrow a connection, and {@link #shutdown()} on exit.
 */
public final class DatabaseConfig {

    private static HikariDataSource dataSource;

    private DatabaseConfig() {}

    /** Initialise the pool and run schema.sql (idempotent — uses IF NOT EXISTS). */
    public static void init() {
        if (dataSource != null) return;

        Properties props = loadProperties();
        String dbType = props.getProperty("db.type", "h2").trim();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("FinixisPool");
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("pool.maximumPoolSize", "10")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("pool.minimumIdle", "2")));
        cfg.setConnectionTimeout(Long.parseLong(props.getProperty("pool.connectionTimeoutMs", "30000")));
        cfg.setIdleTimeout(Long.parseLong(props.getProperty("pool.idleTimeoutMs", "600000")));
        cfg.setMaxLifetime(Long.parseLong(props.getProperty("pool.maxLifetimeMs", "1800000")));

        switch (dbType) {
            case "postgresql" -> {
                cfg.setJdbcUrl(props.getProperty("pg.url"));
                cfg.setUsername(props.getProperty("pg.user"));
                cfg.setPassword(props.getProperty("pg.password"));
                cfg.setDriverClassName("org.postgresql.Driver");
            }
            default -> { // h2 (embedded)
                cfg.setJdbcUrl(props.getProperty("h2.url",
                        "jdbc:h2:~/finixis-data/finixis;MODE=PostgreSQL;"
                        + "DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE"));
                cfg.setUsername(props.getProperty("h2.user", "sa"));
                cfg.setPassword(props.getProperty("h2.password", ""));
                cfg.setDriverClassName("org.h2.Driver");
            }
        }

        dataSource = new HikariDataSource(cfg);
        runSchema();
    }

    /** Borrow a connection from the pool (caller must close it — use try-with-resources). */
    public static Connection get() throws SQLException {
        if (dataSource == null) throw new IllegalStateException("DatabaseConfig not initialised. Call init() first.");
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static void runSchema() {
        try (InputStream is = DatabaseConfig.class.getResourceAsStream("/db/schema.sql")) {
            if (is == null) throw new RuntimeException("schema.sql not found on classpath");
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Strip full-line comments, then split on semicolons
            StringBuilder clean = new StringBuilder();
            for (String line : raw.split("\n")) {
                String stripped = line.strip();
                if (!stripped.startsWith("--")) {
                    clean.append(line).append('\n');
                }
            }

            try (Connection conn = get(); Statement stmt = conn.createStatement()) {
                for (String part : clean.toString().split(";")) {
                    String trimmed = part.strip();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            SeedData.seed();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise database schema", e);
        }
    }

    private static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream is = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {}
        return p;
    }
}
