package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        char[] password = DatabaseConfig.password();
        try {
            Class.forName(DatabaseConfig.driverClassName());
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL Connector/J tidak ditemukan. Pastikan lib/mysql-connector-j*.jar "
                    + "tersedia di classpath.",
                    exception);
        }

        Properties properties = new Properties();
        properties.setProperty("user", DatabaseConfig.username());
        properties.setProperty("password", new String(password));
        java.util.Arrays.fill(password, '\0');
        try {
            return DriverManager.getConnection(DatabaseConfig.jdbcUrl(), properties);
        } catch (SQLException exception) {
            throw new SQLException("Koneksi ke MySQL db_kantinkudesktop gagal: " + exception.getMessage(), exception);
        }
    }

    public static void verifyConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Koneksi MySQL gagal melewati pemeriksaan validitas.");
            }
            if (!"db_kantinkudesktop".equalsIgnoreCase(connection.getCatalog())) {
                throw new SQLException("Koneksi aktif bukan ke database db_kantinkudesktop.");
            }
        }
    }
}
