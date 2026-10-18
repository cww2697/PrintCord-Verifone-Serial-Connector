package net.canyonwolf.printcord.connector.service;

import net.canyonwolf.printcord.connector.api.ApiException;
import net.canyonwolf.printcord.connector.api.PrintCordApiClient;
import net.canyonwolf.printcord.connector.config.ConfigManager;
import net.canyonwolf.printcord.connector.config.ConnectorConfig;
import net.canyonwolf.printcord.connector.model.PrintMessage;
import net.canyonwolf.printcord.connector.printer.ReceiptPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConnectorServiceTest {

    private ConfigManager configManager;
    private PrintCordApiClient mockApiClient;
    private ReceiptPrinter mockPrinter;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("test-connector.json");
        configManager = new ConfigManager(configPath);
        ConnectorConfig config = new ConnectorConfig();
        config.setPortName("COM1");
        config.setApiToken("test-token");
        config.setConnectorId("test-id");
        configManager.save(config);

        mockApiClient = mock(PrintCordApiClient.class);
        mockPrinter = mock(ReceiptPrinter.class);
    }

    @Test
    void testPollOnceDispatchesToPrinter() throws Exception {
        PrintMessage msg = new PrintMessage("Hello Test", "Alice", Instant.now());
        when(mockApiClient.poll("test-token")).thenReturn(List.of(msg));

        ConnectorService service = new ConnectorService(configManager, mockApiClient, mockPrinter);

        List<PrintMessage> printed = new ArrayList<>();
        service.setOnMessagePrintedListener(printed::add);

        service.pollOnce();

        verify(mockPrinter, times(1)).printMessage(msg);
        assertEquals(1, printed.size());
        assertEquals("Hello Test", printed.get(0).getContent());
        assertNotNull(service.getLastSuccessfulPoll());

        service.shutdown();
    }

    @Test
    void testPollOnceHandles401Unauthorized() throws Exception {
        when(mockApiClient.poll("test-token")).thenThrow(new ApiException(401, "Unauthorized"));

        ConnectorService service = new ConnectorService(configManager, mockApiClient, mockPrinter);

        AtomicBoolean deregisteredCalled = new AtomicBoolean(false);
        service.setOnDeregisteredListener(() -> deregisteredCalled.set(true));

        service.pollOnce();

        assertFalse(service.isRunning());
        assertTrue(deregisteredCalled.get());
        assertNull(configManager.getConfig().getApiToken());
        assertNull(configManager.getConfig().getConnectorId());

        service.shutdown();
    }
}
