package net.canyonwolf.printcord.connector.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorConfig {

    private String apiUrl = "http://localhost:8080/api/connectors";
    private String apiToken;
    private String connectorId;
    private String printerName = "Verifone Serial Printer";

    // Serial port configuration
    private String portName;      // e.g. COM1, COM3, /dev/ttyUSB0, /dev/tty.usbserial
    private int baudRate = 9600;  // Verifone defaults typically 9600 or 19200
    private int dataBits = 8;
    private int stopBits = 1;
    private String parity = "None"; // None, Even, Odd, Mark, Space

    // Printer layout
    private int printWidth = 24;  // Verifone 24-column roll or 32/40/42/48 standard receipt width
    private int lineFeedLines = 4; // Feed lines after print job

    // Streaming & pacing configuration for small printer buffers
    private int streamChunkSize = 16;       // bytes per chunk (Verifone buffer safe size)
    private int streamChunkDelayMs = 40;     // delay in ms between chunks to allow printer mechanism/buffer to drain
    private int streamLineDelayMs = 100;     // delay in ms after newline/CRLF for line feed execution

    // Polling settings
    private int pollIntervalSeconds = 5;

    public ConnectorConfig() {
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public String getParity() {
        return parity;
    }

    public void setParity(String parity) {
        this.parity = parity;
    }

    public int getPrintWidth() {
        return printWidth;
    }

    public void setPrintWidth(int printWidth) {
        this.printWidth = printWidth;
    }

    public int getLineFeedLines() {
        return lineFeedLines;
    }

    public void setLineFeedLines(int lineFeedLines) {
        this.lineFeedLines = lineFeedLines;
    }

    public int getStreamChunkSize() {
        return streamChunkSize;
    }

    public void setStreamChunkSize(int streamChunkSize) {
        this.streamChunkSize = streamChunkSize;
    }

    public int getStreamChunkDelayMs() {
        return streamChunkDelayMs;
    }

    public void setStreamChunkDelayMs(int streamChunkDelayMs) {
        this.streamChunkDelayMs = streamChunkDelayMs;
    }

    public int getStreamLineDelayMs() {
        return streamLineDelayMs;
    }

    public void setStreamLineDelayMs(int streamLineDelayMs) {
        this.streamLineDelayMs = streamLineDelayMs;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public boolean isRegistered() {
        return apiToken != null && !apiToken.isBlank();
    }
}
