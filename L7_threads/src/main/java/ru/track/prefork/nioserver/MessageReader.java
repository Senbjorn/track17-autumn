package ru.track.prefork.nioserver;

import java.io.IOException;
import java.util.List;

public interface MessageReader {

    void init(MessageBuffer mb);

    void read(InnerSocket innerSocket) throws IOException;

    List<ByteMessage> getMessages();

}
