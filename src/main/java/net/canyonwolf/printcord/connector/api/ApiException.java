package net.canyonwolf.printcord.connector.api;

public class ApiException extends Exception {

    private final int statusCode;

    public ApiException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public ApiException(int statusCode, String message) {
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public ApiException(int statusCode, String message, Throwable cause) {
        super(message + " (HTTP " + statusCode + ")", cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }
}
