package config;

import java.io.IOException;
import java.sql.SQLException;

public final class DatabaseStartup {
    private DatabaseStartup() {
    }

    public static void initialize() throws IOException {
        try {
            DatabaseConnection.verifyConnection();
        } catch (SQLException exception) {
            throw new IOException("Koneksi MySQL belum dapat disiapkan: " + exception.getMessage(), exception);
        }
    }
}
