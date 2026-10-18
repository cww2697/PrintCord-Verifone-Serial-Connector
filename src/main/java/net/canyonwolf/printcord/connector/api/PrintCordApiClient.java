package net.canyonwolf.printcord.connector.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.canyonwolf.printcord.connector.model.PrintMessage;
import net.canyonwolf.printcord.connector.model.RegisterRequest;
import net.canyonwolf.printcord.connector.model.RegisterResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class PrintCordApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PrintCordApiClient(String baseUrl) {
        this(baseUrl, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public PrintCordApiClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:8080/api/connectors";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public RegisterResponse register(String code, String name) throws ApiException {
        RegisterRequest requestBody = new RegisterRequest(code, name);
        try {
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/register"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), RegisterResponse.class);
            } else {
                throw new ApiException(response.statusCode(),
                        "Registration failed: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("Registration network/IO error: " + extractErrorMessage(e), e);
        }
    }

    public List<PrintMessage> poll(String apiToken) throws ApiException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ApiException(401, "API token is missing or empty");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/poll"))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<PrintMessage>>() {});
            } else {
                throw new ApiException(response.statusCode(),
                        "Poll failed: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("Poll network/IO error: " + extractErrorMessage(e), e);
        }
    }

    public void sendHeartbeat(String apiToken) throws ApiException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ApiException(401, "API token is missing or empty");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/status"))
                    .header("Authorization", "Bearer " + apiToken)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(response.statusCode(),
                        "Heartbeat status update failed: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("Heartbeat network/IO error: " + extractErrorMessage(e), e);
        }
    }

    public void deregister(String apiToken) throws ApiException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new ApiException(401, "API token is missing or empty");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + apiToken)
                    .timeout(Duration.ofSeconds(15))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(response.statusCode(),
                        "Deregistration failed: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("Deregistration network/IO error: " + extractErrorMessage(e), e);
        }
    }

    private static String extractErrorMessage(Throwable t) {
        if (t == null) {
            return "Unknown error";
        }
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        String simpleName = root.getClass().getSimpleName();
        if (root instanceof java.nio.channels.UnresolvedAddressException || "UnresolvedAddressException".equals(simpleName)) {
            return "Could not resolve server domain name (UnresolvedAddressException). Check your internet connection or API URL.";
        }
        if (root instanceof java.net.UnknownHostException || "UnknownHostException".equals(simpleName)) {
            return "Unknown host: " + (msg != null && !msg.isBlank() ? msg : "hostname could not be resolved");
        }
        if (root instanceof java.net.ConnectException || "ConnectException".equals(simpleName)) {
            return "Connection refused / unreachable: " + (msg != null && !msg.isBlank() ? msg : "check if server is running");
        }
        if (msg == null || msg.isBlank()) {
            msg = t.getMessage();
        }
        if (msg == null || msg.isBlank()) {
            return simpleName;
        }
        return msg + " (" + simpleName + ")";
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
