package net.canyonwolf.printcord.connector;

import com.fazecast.jSerialComm.SerialPort;
import net.canyonwolf.printcord.connector.api.ApiException;
import net.canyonwolf.printcord.connector.api.PrintCordApiClient;
import net.canyonwolf.printcord.connector.config.ConfigManager;
import net.canyonwolf.printcord.connector.config.ConfigPathProvider;
import net.canyonwolf.printcord.connector.config.ConnectorConfig;
import net.canyonwolf.printcord.connector.model.RegisterResponse;
import net.canyonwolf.printcord.connector.printer.VerifoneSerialPrinter;
import net.canyonwolf.printcord.connector.service.ConnectorService;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String configPathOverride = null;
        String apiUrlOverride = null;
        Integer feedLinesOverride = null;
        boolean runBackground = false;
        boolean runConfigWizard = false;
        boolean deregisterRequested = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--config") && i + 1 < args.length) {
                configPathOverride = args[++i];
            } else if (arg.equals("--url") && i + 1 < args.length) {
                apiUrlOverride = args[++i];
            } else if ((arg.equals("--feed-lines") || arg.equals("--feedLines")) && i + 1 < args.length) {
                try {
                    feedLinesOverride = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid --feed-lines argument, must be an integer.");
                }
            } else if (arg.equals("--setup") || arg.equals("--configure")) {
                runConfigWizard = true;
            } else if (arg.equals("--daemon") || arg.equals("--background") || arg.equals("-d")) {
                runBackground = true;
            } else if (arg.equals("--deregister")) {
                deregisterRequested = true;
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return;
            }
        }

        Path configPath = configPathOverride != null
                ? Paths.get(configPathOverride)
                : ConfigPathProvider.getDefaultConfigFile();

        System.out.println("=========================================");
        System.out.println("  PrintCord Verifone Serial Connector");
        System.out.println("=========================================");
        System.out.println("Configuration file: " + configPath.toAbsolutePath());

        ConfigManager configManager = new ConfigManager(configPath);
        ConnectorConfig config;
        try {
            config = configManager.load();
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            config = configManager.getConfig();
        }

        if (apiUrlOverride != null && !apiUrlOverride.isBlank()) {
            config.setApiUrl(apiUrlOverride);
            try {
                configManager.save(config);
            } catch (IOException e) {
                System.err.println("Failed to save updated API URL: " + e.getMessage());
            }
        }

        if (feedLinesOverride != null && feedLinesOverride >= 0) {
            config.setLineFeedLines(feedLinesOverride);
            try {
                configManager.save(config);
            } catch (IOException e) {
                System.err.println("Failed to save updated feed lines: " + e.getMessage());
            }
        }

        PrintCordApiClient apiClient = new PrintCordApiClient(config.getApiUrl());

        if (deregisterRequested) {
            handleDeregister(configManager, apiClient);
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // Check if configuration or registration is missing or requested
        if (runConfigWizard || !isConfigComplete(config)) {
            configureWizard(configManager, scanner);
            config = configManager.getConfig();
            apiClient = new PrintCordApiClient(config.getApiUrl());
        }

        if (!config.isRegistered()) {
            System.out.println("\nConnector is not registered with PrintCord bot.");
            promptRegistration(configManager, scanner);
            config = configManager.getConfig();
            apiClient = new PrintCordApiClient(config.getApiUrl());
            if (!config.isRegistered()) {
                System.err.println("Registration aborted or failed. Exiting.");
                return;
            }
        }

        System.out.println("\nStarting Verifone Serial Connector...");
        System.out.println("Printer Name: " + config.getPrinterName());
        System.out.println("Port: " + config.getPortName() + " @ " + config.getBaudRate() + " baud");
        System.out.println("Width: " + config.getPrintWidth() + " columns");
        System.out.println("Feed lines: " + config.getLineFeedLines());
        System.out.println("Poll interval: " + config.getPollIntervalSeconds() + "s");

        VerifoneSerialPrinter printer = new VerifoneSerialPrinter(config);
        ConnectorService service = new ConnectorService(configManager, apiClient, printer);

        service.setOnStatusListener(msg -> System.out.println("[Service] " + msg));
        service.setOnMessagePrintedListener(msg -> System.out.println("[Print Job] Printed message from " + msg.getSender()));
        service.setOnDeregisteredListener(() -> {
            System.out.println("[Service] Connector was deregistered on server side. Stopping.");
        });

        // Add Shutdown Hook for clean termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down connector gracefully...");
            service.shutdown();
        }, "shutdown-hook"));

        service.start();

        if (runBackground) {
            System.out.println("Running in background mode. Press Ctrl+C to terminate.");
            while (service.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } else {
            System.out.println("Connector is running. Type 'quit' or 'q' to stop, 'status' for heartbeat info.");
            while (service.isRunning()) {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("q") || line.equalsIgnoreCase("exit")) {
                        break;
                    } else if (line.equalsIgnoreCase("status")) {
                        System.out.println("Status: Active. Last poll: " + service.getLastSuccessfulPoll());
                    } else if (line.equalsIgnoreCase("poll")) {
                        service.pollOnce();
                    } else if (!line.isEmpty()) {
                        System.out.println("Unknown command: " + line + " (commands: 'quit', 'status', 'poll')");
                    }
                } else {
                    // No stdin available (e.g. piped or running non-interactively)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            service.shutdown();
        }
    }

    private static boolean isConfigComplete(ConnectorConfig config) {
        return config.getPortName() != null && !config.getPortName().isBlank()
                && config.getBaudRate() > 0
                && config.getPrintWidth() > 0;
    }

    private static void configureWizard(ConfigManager configManager, Scanner scanner) {
        ConnectorConfig config = configManager.getConfig();
        System.out.println("\n--- Printer & Serial Configuration ---");

        // Serial Port Selection
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.println("Available serial COM ports:");
        if (ports.length == 0) {
            System.out.println("  (No serial COM ports detected on system)");
        } else {
            for (int i = 0; i < ports.length; i++) {
                System.out.printf("  [%d] %s (%s)%n", i + 1, ports[i].getSystemPortName(), ports[i].getDescriptivePortName());
            }
        }

        String defaultPort = config.getPortName() != null ? config.getPortName() : (ports.length > 0 ? ports[0].getSystemPortName() : "COM1");
        System.out.print("Enter COM port / device [" + defaultPort + "]: ");
        String portInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (portInput.isEmpty()) {
            config.setPortName(defaultPort);
        } else {
            // Check if entered number corresponding to list
            try {
                int portIdx = Integer.parseInt(portInput) - 1;
                if (portIdx >= 0 && portIdx < ports.length) {
                    config.setPortName(ports[portIdx].getSystemPortName());
                } else {
                    config.setPortName(portInput);
                }
            } catch (NumberFormatException e) {
                config.setPortName(portInput);
            }
        }

        // Baud Rate
        int defaultBaud = config.getBaudRate() > 0 ? config.getBaudRate() : 9600;
        System.out.print("Enter BAUD rate (e.g. 9600, 19200, 38400, 115200) [" + defaultBaud + "]: ");
        String baudInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!baudInput.isEmpty()) {
            try {
                config.setBaudRate(Integer.parseInt(baudInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid baud rate format. Keeping default " + defaultBaud);
                config.setBaudRate(defaultBaud);
            }
        } else {
            config.setBaudRate(defaultBaud);
        }

        // Width
        int defaultWidth = config.getPrintWidth() > 0 ? config.getPrintWidth() : 24;
        System.out.print("Enter receipt character column width (e.g. 24, 32, 40, 48) [" + defaultWidth + "]: ");
        String widthInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!widthInput.isEmpty()) {
            try {
                config.setPrintWidth(Integer.parseInt(widthInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid width format. Keeping default " + defaultWidth);
                config.setPrintWidth(defaultWidth);
            }
        } else {
            config.setPrintWidth(defaultWidth);
        }

        // Feed Lines
        int defaultFeedLines = config.getLineFeedLines() >= 0 ? config.getLineFeedLines() : 4;
        System.out.print("Enter number of feed lines after receipt [" + defaultFeedLines + "]: ");
        String feedLinesInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!feedLinesInput.isEmpty()) {
            try {
                config.setLineFeedLines(Integer.parseInt(feedLinesInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid feed lines format. Keeping default " + defaultFeedLines);
                config.setLineFeedLines(defaultFeedLines);
            }
        } else {
            config.setLineFeedLines(defaultFeedLines);
        }

        // Printer Name
        String defaultName = config.getPrinterName() != null ? config.getPrinterName() : "Verifone Serial Printer";
        System.out.print("Enter printer name [" + defaultName + "]: ");
        String nameInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!nameInput.isEmpty()) {
            config.setPrinterName(nameInput);
        }

        // API URL
        String defaultApiUrl = config.getApiUrl() != null ? config.getApiUrl() : "http://printcord.canyonwolf.net/api/connectors";
        System.out.print("Enter PrintCord API base URL [" + defaultApiUrl + "]: ");
        String apiUrlInput = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!apiUrlInput.isEmpty()) {
            config.setApiUrl(apiUrlInput);
        }

        try {
            configManager.save(config);
            System.out.println("Settings saved to " + configManager.getConfigFilePath());
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    private static void promptRegistration(ConfigManager configManager, Scanner scanner) {
        ConnectorConfig config = configManager.getConfig();
        System.out.println("\n--- Discord Bot Registration ---");
        System.out.println("API Server URL: " + config.getApiUrl());
        System.out.println("1. Use '/register' on Discord to generate an 8-character registration code (XXXX-XXXX).");

        while (!config.isRegistered()) {
            System.out.print("Enter registration code (or 'url' to change API URL, 'q' to cancel): ");

            if (!scanner.hasNextLine()) {
                return;
            }
            String code = scanner.nextLine().trim();
            if (code.isEmpty() || code.equalsIgnoreCase("q") || code.equalsIgnoreCase("quit") || code.equalsIgnoreCase("exit")) {
                System.out.println("Registration cancelled.");
                return;
            }

            if (code.equalsIgnoreCase("url")) {
                System.out.print("Enter PrintCord API base URL [" + config.getApiUrl() + "]: ");
                String newUrl = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                if (!newUrl.isEmpty()) {
                    config.setApiUrl(newUrl);
                    try {
                        configManager.save(config);
                        System.out.println("API URL updated to: " + config.getApiUrl());
                    } catch (IOException e) {
                        System.err.println("Failed to save settings: " + e.getMessage());
                    }
                }
                continue;
            }

            String name = config.getPrinterName();
            if (name == null || name.isBlank()) {
                name = "Verifone Serial Printer";
            }

            System.out.println("Registering printer '" + name + "' at " + config.getApiUrl() + " with code '" + code + "'...");
            PrintCordApiClient client = new PrintCordApiClient(config.getApiUrl());
            try {
                RegisterResponse response = client.register(code, name);
                config.setConnectorId(response.getConnectorId());
                config.setApiToken(response.getApiToken());
                configManager.save(config);
                System.out.println("Registration successful! Connector ID: " + response.getConnectorId());
                break;
            } catch (ApiException e) {
                System.err.println("Registration failed: " + e.getMessage());
                System.out.println("Tip: If the API server is hosted on a different address or port, type 'url' to update it.");
            } catch (IOException e) {
                System.err.println("Failed to save registration config: " + e.getMessage());
                break;
            }
        }
    }

    private static void handleDeregister(ConfigManager configManager, PrintCordApiClient apiClient) {
        ConnectorConfig config = configManager.getConfig();
        if (!config.isRegistered()) {
            System.out.println("Connector is not currently registered.");
            return;
        }

        System.out.println("Deregistering connector with server...");
        try {
            apiClient.deregister(config.getApiToken());
            System.out.println("Connector deregistered from server successfully.");
        } catch (ApiException e) {
            System.err.println("Server responded with error during deregistration: " + e.getMessage());
        }

        config.setApiToken(null);
        config.setConnectorId(null);
        try {
            configManager.save(config);
            System.out.println("Config updated.");
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar verifone-serial-connector.jar [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --setup, --configure     Run printer & serial configuration wizard");
        System.out.println("  --url <url>              Override PrintCord API base URL");
        System.out.println("  --feed-lines <count>     Override number of trailing paper feed lines");
        System.out.println("  --config <path>          Custom configuration JSON file path");
        System.out.println("  --background, -d         Run in background daemon mode without interactive prompt");
        System.out.println("  --deregister             Deregister current connector from PrintCord server");
        System.out.println("  --help, -h               Display this help message");
    }
}
