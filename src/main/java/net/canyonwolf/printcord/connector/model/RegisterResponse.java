package net.canyonwolf.printcord.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterResponse {

    private String connectorId;
    private String apiToken;

    public RegisterResponse() {
    }

    public RegisterResponse(String connectorId, String apiToken) {
        this.connectorId = connectorId;
        this.apiToken = apiToken;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
