package net.canyonwolf.printcord.connector.printer;

import net.canyonwolf.printcord.connector.model.PrintMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReceiptFormatter {

    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final int width;

    public ReceiptFormatter(int width) {
        this.width = Math.max(16, width);
    }

    public int getWidth() {
        return width;
    }

    public byte[] formatMessage(PrintMessage message, int feedLines) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. Header: Centered App / Bot banner
        writeLine(out, centerText("=== PRINTCORD ===", width));

        // 2. Sender info
        if (message.getSender() != null && !message.getSender().isBlank()) {
            String fromLine = "From: " + message.getSender();
            for (String line : wrapText(fromLine, width)) {
                writeLine(out, line);
            }
        }

        // 3. Timestamp info
        if (message.getTimestamp() != null) {
            String timeStr = TIME_FORMATTER.format(message.getTimestamp());
            for (String line : wrapText(timeStr, width)) {
                writeLine(out, line);
            }
        }

        // 4. Divider
        String divider = "-".repeat(width);
        writeLine(out, divider);

        // 5. Content
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            List<String> wrapped = wrapParagraphs(message.getContent(), width);
            for (String line : wrapped) {
                writeLine(out, line);
            }
        }

        // 6. Divider bottom
        writeLine(out, divider);

        // 7. Line feed
        int effectiveFeed = feedLines > 0 ? feedLines : 2;
        for (int i = 0; i < effectiveFeed; i++) {
            out.write(CRLF.getBytes(StandardCharsets.US_ASCII));
        }

        return out.toByteArray();
    }

    private static void writeLine(ByteArrayOutputStream out, String line) throws IOException {
        out.write((line + CRLF).getBytes(StandardCharsets.US_ASCII));
    }

    public static String centerText(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (text.length() >= maxWidth) {
            return text;
        }
        int totalPadding = maxWidth - text.length();
        int leftPadding = totalPadding / 2;
        return " ".repeat(leftPadding) + text;
    }

    public static List<String> wrapParagraphs(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }

        String[] lines = text.split("\\r?\\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                result.add("");
            } else {
                result.addAll(wrapText(line, maxWidth));
            }
        }
        return result;
    }

    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                // Keep multiple consecutive spaces if part of line
                if (currentLine.length() > 0 && currentLine.length() + 1 <= maxWidth) {
                    currentLine.append(" ");
                }
                continue;
            }

            if (currentLine.length() == 0) {
                // If single word exceeds maxWidth, break it up
                if (word.length() > maxWidth) {
                    int idx = 0;
                    while (idx < word.length()) {
                        int end = Math.min(idx + maxWidth, word.length());
                        String chunk = word.substring(idx, end);
                        if (chunk.length() == maxWidth || end == word.length()) {
                            if (end < word.length()) {
                                lines.add(chunk);
                            } else {
                                currentLine.append(chunk);
                            }
                        }
                        idx = end;
                    }
                } else {
                    currentLine.append(word);
                }
            } else {
                if (currentLine.length() + 1 + word.length() <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    if (word.length() > maxWidth) {
                        int idx = 0;
                        while (idx < word.length()) {
                            int end = Math.min(idx + maxWidth, word.length());
                            String chunk = word.substring(idx, end);
                            if (end < word.length()) {
                                lines.add(chunk);
                            } else {
                                currentLine.append(chunk);
                            }
                        }
                    } else {
                        currentLine.append(word);
                    }
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
