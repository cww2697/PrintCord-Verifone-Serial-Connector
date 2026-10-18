package net.canyonwolf.printcord.connector.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerTest {

    @Test
    void testConfigLoadSave(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("test-config.json");
        ConfigManager manager = new ConfigManager(configFile);

        ConnectorConfig initial = manager.load();
        assertNotNull(initial);
        assertEquals(9600, initial.getBaudRate());
        assertEquals(24, initial.getPrintWidth());
        assertFalse(initial.isRegistered());

        initial.setPortName("COM3");
        initial.setBaudRate(19200);
        initial.setPrintWidth(32);
        initial.setLineFeedLines(6);
        initial.setApiToken("test-token-123");
        initial.setConnectorId("uuid-1234");
        initial.setPrinterName("Kitchen Verifone");

        manager.save(initial);

        // Load again from disk
        ConfigManager reloadedManager = new ConfigManager(configFile);
        ConnectorConfig loaded = reloadedManager.load();

        assertEquals("COM3", loaded.getPortName());
        assertEquals(19200, loaded.getBaudRate());
        assertEquals(32, loaded.getPrintWidth());
        assertEquals(6, loaded.getLineFeedLines());
        assertEquals("test-token-123", loaded.getApiToken());
        assertEquals("uuid-1234", loaded.getConnectorId());
        assertEquals("Kitchen Verifone", loaded.getPrinterName());
        assertTrue(loaded.isRegistered());
    }

    @Test
    void testConfigPathProvider() {
        Path path = ConfigPathProvider.getDefaultConfigFile();
        assertNotNull(path);
        assertTrue(path.toString().contains("PrintCord") || path.toString().contains("printcord"));
    }
}
