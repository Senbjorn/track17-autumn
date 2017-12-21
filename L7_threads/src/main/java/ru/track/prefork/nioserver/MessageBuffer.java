package ru.track.prefork.nioserver;

import java.util.List;

public interface MessageBuffer {

    void putMessage(ByteMessage message);

    ByteMessage getMessage();

    List<ByteMessage> getMessages();

    boolean isEmpty();
}
