package com.nations.plugin;


import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;

public class DatabaseInterface {

    private final String url;
    private final String user;
    private final String password;

    // The ONE AND ONLY connection
    private Connection connection;

    public DatabaseInterface(String host, int port, String database, String user, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.user = user;
        this.password = password;

        System.out.println("[DB] Initializing DatabaseInterface…");

        try {
            connect();
            runAllSqlFiles("sql");
        } catch (Exception e) {
            System.out.println("[DB] Initialization FAILED.");
            System.out.println("[DB] Reason: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the single connection.
     */
    public void connect() throws SQLException {
        System.out.println("[DB] Attempting connection to: " + url);

        if (connection != null && !connection.isClosed()) {
            System.out.println("[DB] Already connected.");
            return;
        }

        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("[DB] Connection established successfully.");
        } catch (SQLException e) {
            System.out.println("[DB] Connection FAILED.");
            System.out.println("[DB] Reason: " + e.getMessage());
            throw e;
        }
    }

    public String generateUniqueUuid(String tableName) throws SQLException {
        Connection conn = getConnection();

        while (true) {
            String uuid = java.util.UUID.randomUUID().toString();

            String sql = "SELECT uuid FROM " + tableName + " WHERE uuid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    return uuid; // UUID is unused — safe to use
                }
            }
        }
    }

    public String generateUniqueCityUuid() throws SQLException {
        return generateUniqueUuid("Cities");
    }

    public String generateUniqueNationUuid() throws SQLException {
        return generateUniqueUuid("Nations");
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public void runAllSqlFiles(String folderName) throws Exception {
        System.out.println("[DB] Loading SQL folder: " + folderName);

        String[] orderedFiles = new String[] {
                "Blocks.sql"
        };

        for (String fileName : orderedFiles) {
            String resourcePath = folderName + "/" + fileName;
            System.out.println("[DB] Executing SQL file: " + fileName);
            runSqlFile(resourcePath);
        }
    }

    public void runSqlFile(String resourcePath) throws Exception {
        String sql;

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("[DB] SQL file not found: " + resourcePath);
            }
            sql = new String(in.readAllBytes());
        }

        // Use the existing connection — DO NOT close it
        if (connection == null || connection.isClosed()) {
            throw new IllegalStateException("[DB] Connection is not open.");
        }

        try (Statement stmt = connection.createStatement()) {

            for (String command : sql.split(";")) {
                String trimmed = command.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }

            System.out.println("[DB] Executed: " + resourcePath);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DB] Connection closed.");
            } catch (SQLException e) {
                System.out.println("[DB] Error while closing connection: " + e.getMessage());
            }
        } else {
            System.out.println("[DB] No connection to close.");
        }
    }
}