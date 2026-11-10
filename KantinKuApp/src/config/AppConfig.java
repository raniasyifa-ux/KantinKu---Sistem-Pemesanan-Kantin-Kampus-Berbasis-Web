package config;

import java.nio.file.Path;

/** Shared paths for application-owned data outside the source tree. */
public final class AppConfig {
    public static final String DATA_DIRECTORY_NAME = ".kantinku-desktop";

    private AppConfig() {
    }

    public static Path dataDirectory() {
        return Path.of(System.getProperty("user.home"), DATA_DIRECTORY_NAME);
    }
}
