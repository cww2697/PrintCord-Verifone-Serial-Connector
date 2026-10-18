package net.canyonwolf.printcord.connector.printer;

import net.canyonwolf.printcord.connector.model.PrintMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiptFormatterTest {

    @Test
    void testWrapTextStandard() {
        String input = "Hello world from Discord printer test!";
        List<String> lines = ReceiptFormatter.wrapText(input, 20);

        assertTrue(lines.size() >= 2);
        for (String line : lines) {
            assertTrue(line.length() <= 20, "Line exceeds max width: '" + line + "' (" + line.length() + ")");
        }
    }

    @Test
    void testWrapTextLongWord() {
        String input = "Supercalifragilisticexpialidocious";
        List<String> lines = ReceiptFormatter.wrapText(input, 10);

        assertEquals(4, lines.size());
        assertEquals("Supercalif", lines.get(0));
        assertEquals("ragilistic", lines.get(1));
        assertEquals("expialidoc", lines.get(2));
        assertEquals("ious", lines.get(3));
    }

    @Test
    void testFormatMessage() throws IOException {
        ReceiptFormatter formatter = new ReceiptFormatter(24);
        PrintMessage message = new PrintMessage();
        message.setSender("Cody");
        message.setContent("Hello this is a test");
        message.setTimestamp(Instant.parse("2024-03-20T12:00:00Z"));

        byte[] rawOutput = formatter.formatMessage(message, 3);
        assertNotNull(rawOutput);
        assertTrue(rawOutput.length > 0);

        String textString = new String(rawOutput, StandardCharsets.US_ASCII);
        assertTrue(textString.contains("=== PRINTCORD ==="));
        assertTrue(textString.contains("From: Cody"));
        assertTrue(textString.contains("Hello this is a test"));
        assertTrue(textString.contains("------------------------"));
        assertTrue(textString.endsWith("\r\n\r\n\r\n"));
        // Ensure no raw ESC/POS control escape bytes (0x1B) exist in the output stream
        for (byte b : rawOutput) {
            assertNotEquals(0x1B, b, "Output should not contain ESC/POS control code 0x1B");
        }
    }

    @Test
    void testCenterText() {
        assertEquals("   Hello", ReceiptFormatter.centerText("Hello", 11));
        assertEquals("Exact", ReceiptFormatter.centerText("Exact", 5));
        assertEquals("TooLongText", ReceiptFormatter.centerText("TooLongText", 5));
    }
}
