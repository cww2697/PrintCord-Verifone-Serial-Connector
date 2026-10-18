package net.canyonwolf.printcord.connector.api;

import net.canyonwolf.printcord.connector.model.PrintMessage;
import net.canyonwolf.printcord.connector.model.RegisterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PrintCordApiClientTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;
    private PrintCordApiClient apiClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);
        apiClient = new PrintCordApiClient("http://localhost:8080/api/connectors", mockHttpClient);
    }

    @Test
    void testRegisterSuccess() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"connectorId\":\"uuid-123\",\"apiToken\":\"token-xyz\"}");
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        RegisterResponse res = apiClient.register("1234-5678", "Test Printer");
        assertNotNull(res);
        assertEquals("uuid-123", res.getConnectorId());
        assertEquals("token-xyz", res.getApiToken());
    }

    @Test
    void testRegisterFailure() throws Exception {
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"error\":\"Invalid code\"}");
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        ApiException ex = assertThrows(ApiException.class, () -> apiClient.register("0000-0000", "Test"));
        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Invalid code"));
    }

    @Test
    void testRegisterNetworkErrorWithNullMessage() throws Exception {
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException(new java.net.UnknownHostException()));

        ApiException ex = assertThrows(ApiException.class, () -> apiClient.register("1234-5678", "Test"));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Unknown host"));
        assertFalse(ex.getMessage().contains("null"));
    }

    @Test
    void testRegisterUnresolvedAddressException() throws Exception {
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException(new java.nio.channels.UnresolvedAddressException()));

        ApiException ex = assertThrows(ApiException.class, () -> apiClient.register("1234-5678", "Test"));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("UnresolvedAddressException"));
        assertTrue(ex.getMessage().contains("Could not resolve server domain name"));
    }

    @Test
    void testPollSuccess() throws Exception {
        String json = """
                [
                  {
                    "content": "Hello from Discord!",
                    "sender": "Cody",
                    "timestamp": "2024-03-20T12:00:00Z"
                  }
                ]
                """;
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        List<PrintMessage> messages = apiClient.poll("valid-token");
        assertEquals(1, messages.size());
        assertEquals("Hello from Discord!", messages.get(0).getContent());
        assertEquals("Cody", messages.get(0).getSender());
    }

    @Test
    void testPoll401Unauthorized() throws Exception {
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Unauthorized");
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        ApiException ex = assertThrows(ApiException.class, () -> apiClient.poll("invalid-token"));
        assertTrue(ex.isUnauthorized());
    }

    @Test
    void testSendHeartbeat() throws Exception {
        when(mockResponse.statusCode()).thenReturn(204);
        when(mockResponse.body()).thenReturn("");
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> apiClient.sendHeartbeat("valid-token"));
    }

    @Test
    void testDeregister() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"status\":\"ok\"}");
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> apiClient.deregister("valid-token"));
    }
}
