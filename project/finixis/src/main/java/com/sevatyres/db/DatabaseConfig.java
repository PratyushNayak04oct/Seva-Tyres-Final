package com.sevatyres.db;

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
 */
public final class DatabaseConfig {

    private static HikariDataSource dataSource;

    private DatabaseConfig() {}

    public static void init() {
        if (dataSource != null) return;

        Properties props = loadProperties();
        String dbType = props.getProperty("db.type", "postgresql").trim();

        HikariConfig cfg = buildPoolConfig(props);

        if ("postgresql".equals(dbType)) {
            String pgUrl  = props.getProperty("pg.url", "jdbc:postgresql://localhost:5432/sevatyres");
            String pgUser = props.getProperty("pg.user", "postgres");
            String pgPass = props.getProperty("pg.password", "");
            cfg.setJdbcUrl(pgUrl);
            cfg.setUsername(pgUser);
            cfg.setPassword(pgPass);
            cfg.setDriverClassName("org.postgresql.Driver");

            // Auto-create the database if it does not exist yet
            ensurePostgresDatabase(pgUrl, pgUser, pgPass);

            try {
                dataSource = new HikariDataSource(cfg);
                System.out.println("[DB] Connected to PostgreSQL: " + pgUrl);
            } catch (Exception pgEx) {
                System.err.println("[DB] PostgreSQL connection failed (" + pgEx.getMessage()
                        + ") — falling back to H2 embedded database.");
                dataSource = null;
                cfg = buildPoolConfig(props);
                cfg.setJdbcUrl(h2Url(props));
                cfg.setUsername(props.getProperty("h2.user", "sa"));
                cfg.setPassword(props.getProperty("h2.password", ""));
                cfg.setDriverClassName("org.h2.Driver");
                dataSource = new HikariDataSource(cfg);
                System.out.println("[DB] Connected to H2 fallback database.");
            }
        } else {
            cfg.setJdbcUrl(h2Url(props));
            cfg.setUsername(props.getProperty("h2.user", "sa"));
            cfg.setPassword(props.getProperty("h2.password", ""));
            cfg.setDriverClassName("org.h2.Driver");
            dataSource = new HikariDataSource(cfg);
            System.out.println("[DB] Connected to H2 embedded database.");
        }

        runSchema();
        runMigrations();
    }

    /** Safely adds new columns/tables that may not exist in older databases. */
    private static void runMigrations() {
        String[] migrations = {
            "ALTER TABLE Inventory ADD COLUMN barcode VARCHAR(100)",
            "ALTER TABLE Sale_Transaction ADD COLUMN unit_price DECIMAL(15,2) NOT NULL DEFAULT 0",
            "ALTER TABLE Sale_Transaction ADD COLUMN inventory_item_id INTEGER",
            // H2: ALTER COLUMN syntax; PostgreSQL: SET DEFAULT '' then OK
            "ALTER TABLE Sale_Transaction ALTER COLUMN bill_no SET DEFAULT ''",
            "ALTER TABLE Sale_Transaction ALTER COLUMN bill_no VARCHAR(20)"
        };
        try (Connection conn = get(); Statement stmt = conn.createStatement()) {
            for (String sql : migrations) {
                try { stmt.execute(sql); }
                catch (Exception ignored) {} // column already exists — fine
            }
        } catch (Exception e) {
            System.err.println("[DB] Migration note: " + e.getMessage());
        }
    }

    private static HikariConfig buildPoolConfig(Properties props) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("SevaTyresPool");
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("pool.maximumPoolSize", "10")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("pool.minimumIdle", "2")));
        cfg.setConnectionTimeout(Long.parseLong(props.getProperty("pool.connectionTimeoutMs", "30000")));
        cfg.setIdleTimeout(Long.parseLong(props.getProperty("pool.idleTimeoutMs", "600000")));
        cfg.setMaxLifetime(Long.parseLong(props.getProperty("pool.maxLifetimeMs", "1800000")));
        return cfg;
    }

    private static String h2Url(Properties props) {
        return props.getProperty("h2.url",
                "jdbc:h2:~/seva-tyres-data/sevatyres;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
    }

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

    /**
     * Connects to the default "postgres" maintenance database and creates the
     * target database (e.g. "sevatyres") if it does not already exist.
     * Silently skips if the maintenance-DB connection itself fails.
     */
    private static void ensurePostgresDatabase(String jdbcUrl, String user, String password) {
        // Extract database name from URL: jdbc:postgresql://host:port/dbname
        String dbName = null;
        try {
            String path = jdbcUrl.replaceFirst(".*//[^/]+/", "").split("\\?")[0].trim();
            if (!path.isEmpty()) dbName = path;
        } catch (Exception ignored) {}
        if (dbName == null) return;

        // Connect to the "postgres" maintenance DB to issue CREATE DATABASE
        String maintenanceUrl = jdbcUrl.replaceFirst("/[^/?]+([?].*)?$", "/postgres");
        try {
            Class.forName("org.postgresql.Driver");
            try (java.sql.Connection con = java.sql.DriverManager.getConnection(maintenanceUrl, user, password);
                 java.sql.Statement st = con.createStatement()) {
                java.sql.ResultSet rs = con.getMetaData().getCatalogs();
                boolean found = false;
                while (rs.next()) {
                    if (dbName.equalsIgnoreCase(rs.getString(1))) { found = true; break; }
                }
                rs.close();
                if (!found) {
                    st.execute("CREATE DATABASE \"" + dbName + "\"");
                    System.out.println("[DB] Created PostgreSQL database: " + dbName);
                }
            }
        } catch (Exception e) {
            System.err.println("[DB] Could not auto-create database '" + dbName + "': " + e.getMessage());
        }
    }
}
