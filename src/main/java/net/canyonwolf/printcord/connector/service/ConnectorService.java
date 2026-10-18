package net.canyonwolf.printcord.connector.service;

import net.canyonwolf.printcord.connector.api.ApiException;
import net.canyonwolf.printcord.connector.api.PrintCordApiClient;
import net.canyonwolf.printcord.connector.config.ConfigManager;
import net.canyonwolf.printcord.connector.config.ConnectorConfig;
import net.canyonwolf.printcord.connector.model.PrintMessage;
import net.canyonwolf.printcord.connector.printer.ReceiptPrinter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ConnectorService {

    private final ConfigManager configManager;
    private final PrintCordApiClient apiClient;
    private final ReceiptPrinter printer;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> pollingTask;
    private ScheduledFuture<?> heartbeatTask;
    private Instant lastSuccessfulPoll = null;

    private Consumer<PrintMessage> onMessagePrintedListener;
    private Consumer<String> onStatusListener;
    private Runnable onDeregisteredListener;

    public ConnectorService(ConfigManager configManager,
                            PrintCordApiClient apiClient,
                            ReceiptPrinter printer) {
        this(configManager, apiClient, printer, Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "printcord-connector-worker");
            t.setDaemon(true);
            return t;
        }));
    }

    public ConnectorService(ConfigManager configManager,
                            PrintCordApiClient apiClient,
                            ReceiptPrinter printer,
                            ScheduledExecutorService scheduler) {
        this.configManager = configManager;
        this.apiClient = apiClient;
        this.printer = printer;
        this.scheduler = scheduler;
    }

    public synchronized void start() {
        if (running.get()) {
            log("ConnectorService is already running.");
            return;
        }

        ConnectorConfig config = configManager.getConfig();
        if (!config.isRegistered()) {
            log("Connector is not registered. Please register first with a valid code.");
            return;
        }

        running.set(true);
        log("Starting ConnectorService polling loop for printer: " + config.getPrinterName());

        try {
            printer.open();
            log("Serial printer connected successfully on port: " + config.getPortName());
        } catch (Exception e) {
            log("Warning: Could not immediately connect to serial printer (" + e.getMessage() + "). Will retry on incoming jobs.");
        }

        int interval = Math.max(1, config.getPollIntervalSeconds());
        pollingTask = scheduler.scheduleWithFixedDelay(this::pollOnce, 0, interval, TimeUnit.SECONDS);

        // Schedule heartbeat check every 2 minutes as a backup if no messages
        heartbeatTask = scheduler.scheduleWithFixedDelay(this::heartbeatCheck, 2, 2, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        log("Stopping ConnectorService...");

        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }

        try {
            printer.close();
        } catch (Exception e) {
            log("Error closing printer: " + e.getMessage());
        }

        log("ConnectorService stopped.");
    }

    public void pollOnce() {
        ConnectorConfig config = configManager.getConfig();
        String apiToken = config.getApiToken();

        if (apiToken == null || apiToken.isBlank()) {
            log("API token is missing. Stopping polling.");
            stop();
            return;
        }

        try {
            List<PrintMessage> messages = apiClient.poll(apiToken);
            lastSuccessfulPoll = Instant.now();

            if (messages != null && !messages.isEmpty()) {
                log("Received " + messages.size() + " message(s) to print.");
                for (PrintMessage message : messages) {
                    processPrintMessage(message);
                }
            }
        } catch (ApiException e) {
            if (e.isUnauthorized()) {
                handleUnauthorized();
            } else {
                log("API Error during poll: " + e.getMessage());
            }
        } catch (Exception e) {
            log("Unexpected error during poll: " + e.getMessage());
        }
    }

    private void heartbeatCheck() {
        if (!running.get()) {
            return;
        }
        // If last successful poll was more than 3 minutes ago, send explicit heartbeat
        if (lastSuccessfulPoll == null || Duration.between(lastSuccessfulPoll, Instant.now()).toMinutes() >= 3) {
            try {
                String token = configManager.getConfig().getApiToken();
                if (token != null && !token.isBlank()) {
                    apiClient.sendHeartbeat(token);
                    log("Heartbeat sent successfully.");
                }
            } catch (ApiException e) {
                if (e.isUnauthorized()) {
                    handleUnauthorized();
                } else {
                    log("Heartbeat failed: " + e.getMessage());
                }
            } catch (Exception e) {
                log("Unexpected error sending heartbeat: " + e.getMessage());
            }
        }
    }

    private void processPrintMessage(PrintMessage message) {
        log("Printing message from " + (message.getSender() != null ? message.getSender() : "Unknown") + ": " + message.getContent());
        try {
            printer.printMessage(message);
            if (onMessagePrintedListener != null) {
                onMessagePrintedListener.accept(message);
            }
        } catch (IOException e) {
            log("Failed to print message via serial port: " + e.getMessage());
            // Attempt to reopen next time
            printer.close();
        }
    }

    private void handleUnauthorized() {
        log("Received 401 Unauthorized: The connector has been deregistered or the token is invalid.");
        ConnectorConfig config = configManager.getConfig();
        config.setApiToken(null);
        config.setConnectorId(null);
        try {
            configManager.save(config);
            log("Cleared API token and saved config.");
        } catch (IOException e) {
            log("Failed to update config file after 401: " + e.getMessage());
        }

        stop();

        if (onDeregisteredListener != null) {
            onDeregisteredListener.run();
        }
    }

    public void setOnMessagePrintedListener(Consumer<PrintMessage> listener) {
        this.onMessagePrintedListener = listener;
    }

    public void setOnStatusListener(Consumer<String> listener) {
        this.onStatusListener = listener;
    }

    public void setOnDeregisteredListener(Runnable listener) {
        this.onDeregisteredListener = listener;
    }

    public boolean isRunning() {
        return running.get();
    }

    public Instant getLastSuccessfulPoll() {
        return lastSuccessfulPoll;
    }

    private void log(String message) {
        if (onStatusListener != null) {
            onStatusListener.accept(message);
        } else {
            System.out.println("[Connector] " + message);
        }
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
