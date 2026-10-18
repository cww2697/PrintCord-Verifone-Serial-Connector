package net.canyonwolf.printcord.connector.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class ConfigPathProvider {

    private static final String APP_DIR_NAME = "PrintCord";

    public static Path getDefaultConfigDirectory() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home", ".");

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_DIR_NAME);
            }
            return Paths.get(userHome, "AppData", "Roaming", APP_DIR_NAME);
        } else if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME);
        } else {
            // Linux / Unix / BSD
            String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
                return Paths.get(xdgConfigHome, APP_DIR_NAME.toLowerCase(Locale.ROOT));
            }
            return Paths.get(userHome, ".config", APP_DIR_NAME.toLowerCase(Locale.ROOT));
        }
    }

    public static Path getDefaultConfigFile() {
        return getDefaultConfigDirectory().resolve("verifone-connector.json");
    }
}
