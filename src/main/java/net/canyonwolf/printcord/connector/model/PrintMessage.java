package net.canyonwolf.printcord.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintMessage {

    private String content;
    private String sender;
    private Instant timestamp;

    public PrintMessage() {
    }

    public PrintMessage(String content, String sender, Instant timestamp) {
        this.content = content;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
