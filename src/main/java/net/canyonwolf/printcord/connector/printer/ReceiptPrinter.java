package net.canyonwolf.printcord.connector.printer;

import net.canyonwolf.printcord.connector.model.PrintMessage;

import java.io.IOException;

public interface ReceiptPrinter extends AutoCloseable {

    void open() throws IOException;

    boolean isOpen();

    void printMessage(PrintMessage message) throws IOException;

    void printRaw(byte[] data) throws IOException;

    void printText(String text) throws IOException;

    void feed(int lines) throws IOException;

    @Override
    void close();
}
