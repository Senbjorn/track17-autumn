package ru.track.prefork.nioserver;


import java.io.IOException;
import java.nio.channels.Selector;

public interface MessageWriter {

    void init(MessageBuffer mb);

    void write(InnerSocket innerSocket, Selector selector) throws IOException;

    void put(ByteMessage message, InnerSocket innerSocket, Selector selector);

    ByteMessage convert(byte[] message);

    boolean isReady();

}
