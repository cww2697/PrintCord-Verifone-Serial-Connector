package net.canyonwolf.printcord.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Path configFilePath;
    private final ObjectMapper objectMapper;
    private ConnectorConfig config;

    public ConfigManager() {
        this(ConfigPathProvider.getDefaultConfigFile());
    }

    public ConfigManager(Path configFilePath) {
        this.configFilePath = configFilePath;
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized ConnectorConfig load() throws IOException {
        if (Files.exists(configFilePath)) {
            this.config = objectMapper.readValue(configFilePath.toFile(), ConnectorConfig.class);
        } else {
            this.config = new ConnectorConfig();
        }
        return this.config;
    }

    public synchronized void save(ConnectorConfig config) throws IOException {
        this.config = config;
        Path parent = configFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        objectMapper.writeValue(configFilePath.toFile(), config);
    }

    public synchronized ConnectorConfig getConfig() {
        if (this.config == null) {
            try {
                load();
            } catch (IOException e) {
                this.config = new ConnectorConfig();
            }
        }
        return this.config;
    }

    public Path getConfigFilePath() {
        return configFilePath;
    }
}
