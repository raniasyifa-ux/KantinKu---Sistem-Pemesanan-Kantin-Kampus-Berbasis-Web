package config;

public final class DatabaseConfig {
    public static final String PASSWORD_ENVIRONMENT_VARIABLE = "KANTINKU_MYSQL_PASSWORD";
    public static final String USERNAME_ENVIRONMENT_VARIABLE = "KANTINKU_MYSQL_USERNAME";

    private DatabaseConfig() {
    }

    public static String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    public static String jdbcUrl() {
        return System.getProperty("kantinku.mysql.jdbcUrl",
                "jdbc:mysql://localhost:3306/db_kantinkudesktop?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Jakarta");
    }

    public static String username() {
        String configured = System.getProperty("kantinku.mysql.username");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(USERNAME_ENVIRONMENT_VARIABLE);
        }
        return configured == null || configured.isBlank() ? "root" : configured.trim();
    }

    public static char[] password() {
        String password = System.getenv(PASSWORD_ENVIRONMENT_VARIABLE);
        return password == null ? new char[0] : password.toCharArray();
    }
}
