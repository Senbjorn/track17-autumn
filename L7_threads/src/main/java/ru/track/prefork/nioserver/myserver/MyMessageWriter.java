package ru.track.prefork.nioserver.myserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.nioserver.ByteMessage;
import ru.track.prefork.nioserver.MessageBuffer;
import ru.track.prefork.nioserver.MessageWriter;
import ru.track.prefork.nioserver.InnerSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;

public class MyMessageWriter implements MessageWriter {

    static final Logger log = LoggerFactory.getLogger(MyMessageWriter.class);

    MessageBuffer messageBuffer = null;
    ByteMessage currentMessage = null;
    ByteBuffer byteBuffer = null;
    int bytesWritten = 0;
    int bytesRead = 0;


    @Override
    public void init(MessageBuffer mb) {
        messageBuffer = mb;
        byteBuffer = ByteBuffer.allocate(512);
    }

    @Override
    public void write(InnerSocket innerSocket, Selector selector) throws IOException{
        if (currentMessage == null)  {
            if (messageBuffer.isEmpty()) return;
            currentMessage = messageBuffer.getMessage();
        }
        bytesRead += currentMessage.readToBuffer(byteBuffer, bytesRead);
        byteBuffer.flip();
        bytesWritten += innerSocket.getSocketChannel().write(byteBuffer);
        byteBuffer.compact();
        if (bytesWritten == currentMessage.getSize()) {
            currentMessage = null;
            bytesWritten = 0;
            bytesRead = 0;
            log.info("message is written");
            if (!isReady()) {
                innerSocket.registerReader(selector);
            }
        }
    }

    @Override
    public void put(ByteMessage message, InnerSocket innerSocket, Selector selector) {
        try {
            innerSocket.registerWriter(selector);
            messageBuffer.putMessage(message);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ByteMessage convert(byte[] message) {
        byte[] length = new byte[4];
        for (int i = 0; i < 4; i++) {
            length[i] = (byte) ((message.length >> (8 * (3 - i))) & (0xFF));
        }
        ByteMessage byteMessage = new ByteMessage();
        byteMessage.writeToMessage(length);
        byteMessage.writeToMessage(message);
        return byteMessage;
    }

    @Override
    public boolean isReady() {
        return currentMessage != null || !messageBuffer.isEmpty();
    }

}
