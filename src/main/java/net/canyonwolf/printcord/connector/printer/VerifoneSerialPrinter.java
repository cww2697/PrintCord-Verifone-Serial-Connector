package net.canyonwolf.printcord.connector.printer;

import com.fazecast.jSerialComm.SerialPort;
import net.canyonwolf.printcord.connector.config.ConnectorConfig;
import net.canyonwolf.printcord.connector.model.PrintMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VerifoneSerialPrinter implements ReceiptPrinter {

    private final ConnectorConfig config;
    private final ReceiptFormatter formatter;
    private SerialPort serialPort;
    private OutputStream outputStream;

    public VerifoneSerialPrinter(ConnectorConfig config) {
        this.config = config;
        this.formatter = new ReceiptFormatter(config.getPrintWidth());
    }

    public static List<String> listAvailablePorts() {
        List<String> ports = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            ports.add(port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")");
        }
        return ports;
    }

    public static SerialPort findPort(String portName) {
        if (portName == null || portName.isBlank()) {
            return null;
        }
        String search = portName.trim();
        for (SerialPort port : SerialPort.getCommPorts()) {
            if (port.getSystemPortName().equalsIgnoreCase(search)
                    || port.getSystemPortPath().equalsIgnoreCase(search)
                    || port.getDescriptivePortName().equalsIgnoreCase(search)) {
                return port;
            }
        }
        // Fallback to getCommPort with exact name or path
        return SerialPort.getCommPort(search);
    }

    @Override
    public synchronized void open() throws IOException {
        if (isOpen()) {
            return;
        }

        String portName = config.getPortName();
        if (portName == null || portName.isBlank()) {
            throw new IOException("Serial port name is not configured.");
        }

        serialPort = findPort(portName);
        if (serialPort == null) {
            throw new IOException("Serial port not found: " + portName);
        }

        serialPort.setBaudRate(config.getBaudRate());
        serialPort.setNumDataBits(config.getDataBits() > 0 ? config.getDataBits() : 8);
        serialPort.setNumStopBits(config.getStopBits() > 0 ? config.getStopBits() : SerialPort.ONE_STOP_BIT);
        serialPort.setParity(parseParity(config.getParity()));

        // Set timeouts: block with 1000ms write timeout
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 1000);

        if (!serialPort.openPort()) {
            throw new IOException("Failed to open serial port " + portName + ". Check permissions or if port is in use.");
        }

        outputStream = serialPort.getOutputStream();
        outputStream.flush();
    }

    private int parseParity(String parity) {
        if (parity == null) {
            return SerialPort.NO_PARITY;
        }
        return switch (parity.trim().toLowerCase(Locale.ROOT)) {
            case "even" -> SerialPort.EVEN_PARITY;
            case "odd" -> SerialPort.ODD_PARITY;
            case "mark" -> SerialPort.MARK_PARITY;
            case "space" -> SerialPort.SPACE_PARITY;
            default -> SerialPort.NO_PARITY;
        };
    }

    @Override
    public synchronized boolean isOpen() {
        return serialPort != null && serialPort.isOpen() && outputStream != null;
    }

    @Override
    public synchronized void printMessage(PrintMessage message) throws IOException {
        if (!isOpen()) {
            open();
        }
        byte[] payload = formatter.formatMessage(message, config.getLineFeedLines());
        printRaw(payload);
    }

    @Override
    public synchronized void printRaw(byte[] data) throws IOException {
        if (!isOpen()) {
            open();
        }
        if (outputStream == null) {
            throw new IOException("Output stream is null on open serial port");
        }
        if (data == null || data.length == 0) {
            return;
        }

        streamData(data, outputStream, config);
    }

    /**
     * Stream data to output stream in small chunks with delay pacing to prevent buffer overrun
     * on Verifone printers with small hardware/receive buffers.
     */
    public static void streamData(byte[] data, OutputStream out, ConnectorConfig config) throws IOException {
        if (data == null || data.length == 0 || out == null) {
            return;
        }

        int chunkSize = config != null && config.getStreamChunkSize() > 0 ? config.getStreamChunkSize() : 16;
        int chunkDelay = config != null && config.getStreamChunkDelayMs() >= 0 ? config.getStreamChunkDelayMs() : 40;
        int lineDelay = config != null && config.getStreamLineDelayMs() >= 0 ? config.getStreamLineDelayMs() : 100;

        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(chunkSize, data.length - offset);

            // Check if there is a newline within this slice to give printer mechanism time to advance
            boolean containsNewline = false;
            for (int i = 0; i < len; i++) {
                if (data[offset + i] == '\n') {
                    containsNewline = true;
                    // Cut chunk right after newline so newline gets processed with lineDelay
                    len = i + 1;
                    break;
                }
            }

            out.write(data, offset, len);
            out.flush();
            offset += len;

            int sleepMs = containsNewline ? Math.max(chunkDelay, lineDelay) : chunkDelay;
            if (sleepMs > 0 && offset < data.length) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Streaming interrupted", e);
                }
            }
        }
    }

    @Override
    public synchronized void printText(String text) throws IOException {
        if (text == null) {
            return;
        }
        printRaw(text.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public synchronized void feed(int lines) throws IOException {
        if (lines <= 0) {
            return;
        }
        String feedStr = "\r\n".repeat(lines);
        printRaw(feedStr.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public synchronized void close() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
            outputStream = null;
        }
        if (serialPort != null) {
            try {
                if (serialPort.isOpen()) {
                    serialPort.closePort();
                }
            } catch (Exception ignored) {
            }
            serialPort = null;
        }
    }

    public ReceiptFormatter getFormatter() {
        return formatter;
    }

    public ConnectorConfig getConfig() {
        return config;
    }
}
