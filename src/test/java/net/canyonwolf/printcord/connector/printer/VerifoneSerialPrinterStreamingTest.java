package net.canyonwolf.printcord.connector.printer;

import net.canyonwolf.printcord.connector.config.ConnectorConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VerifoneSerialPrinterStreamingTest {

    @Test
    void testStreamDataIntegrity() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConnectorConfig config = new ConnectorConfig();
        config.setStreamChunkSize(8);
        config.setStreamChunkDelayMs(0); // 0 delay for fast unit test
        config.setStreamLineDelayMs(0);

        String sampleText = "The quick brown fox jumps over the lazy dog\r\nSecond Line\r\nThird Line";
        byte[] inputBytes = sampleText.getBytes(StandardCharsets.US_ASCII);

        VerifoneSerialPrinter.streamData(inputBytes, out, config);

        String result = out.toString(StandardCharsets.US_ASCII);
        assertEquals(sampleText, result);
    }

    @Test
    void testStreamDataChunkingByNewlines() throws IOException {
        List<byte[]> writtenChunks = new ArrayList<>();
        ByteArrayOutputStream trackingOut = new ByteArrayOutputStream() {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                byte[] chunk = new byte[len];
                System.arraycopy(b, off, chunk, 0, len);
                writtenChunks.add(chunk);
                super.write(b, off, len);
            }
        };

        ConnectorConfig config = new ConnectorConfig();
        config.setStreamChunkSize(16);
        config.setStreamChunkDelayMs(0);
        config.setStreamLineDelayMs(0);

        String input = "12345\n67890123456789012345\n";
        VerifoneSerialPrinter.streamData(input.getBytes(StandardCharsets.US_ASCII), trackingOut, config);

        assertEquals(input, trackingOut.toString(StandardCharsets.US_ASCII));
        // First chunk should end with '\n' (length 6: "12345\n")
        assertEquals("12345\n", new String(writtenChunks.get(0), StandardCharsets.US_ASCII));
    }

    @Test
    void testStreamDataEmptyOrNull() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConnectorConfig config = new ConnectorConfig();
        VerifoneSerialPrinter.streamData(null, out, config);
        VerifoneSerialPrinter.streamData(new byte[0], out, config);
        assertEquals(0, out.size());
    }
}
