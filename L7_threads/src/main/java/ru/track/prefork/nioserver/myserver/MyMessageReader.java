package ru.track.prefork.nioserver.myserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.nioserver.ByteMessage;
import ru.track.prefork.nioserver.MessageBuffer;
import ru.track.prefork.nioserver.MessageReader;
import ru.track.prefork.nioserver.InnerSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MyMessageReader implements MessageReader {

    static final Logger log = LoggerFactory.getLogger(MyMessageReader.class);

    MessageBuffer messageBuffer = null;
    ByteMessage currentMessage = null;
    ByteBuffer byteBuffer = null;
    int length = 0;


    @Override
    public void init(MessageBuffer mb) {
        messageBuffer = mb;
        byteBuffer = ByteBuffer.allocate(512);
    }

    @Override
    public void read(InnerSocket innerSocket) throws IOException {
        int bytes = innerSocket.getSocketChannel().read(byteBuffer);
        if (bytes == -1) {
            innerSocket.close();
            return;
        }
        byteBuffer.flip();
        if (currentMessage == null) {
            currentMessage = new ByteMessage();
        }
        if (currentMessage.getSize() == 0 && byteBuffer.remaining() >= 4) {
            byte[] blength = new byte[4];
            byteBuffer.get(blength);
            currentMessage.writeToMessage(blength);
            for (int i = 0; i < 4; i++) {
                length += ((blength[i] & (0xFF)) << ((3 - i) * 8));
            }
            length += 4;
        }
        if (length - currentMessage.getSize() != 0)
            currentMessage.writeToMessage(byteBuffer, length - currentMessage.getSize());
        byteBuffer.compact();
        if (currentMessage.getSize() == length) {
            messageBuffer.putMessage(currentMessage);
            length = 0;
            currentMessage = null;
        }
    }

    @Override
    public List<ByteMessage> getMessages() {
        List<ByteMessage> messages = messageBuffer.getMessages();
        List<ByteMessage> result = new ArrayList<>();
        for (ByteMessage message: messages) {
            result.add(pureMessage(message));
        }
        return result;
    }

    private ByteMessage pureMessage(ByteMessage msg) {
        byte[] bytes = new byte[msg.getSize() - 4];
        System.arraycopy(msg.getByteArray(), 4, bytes, 0, bytes.length);
        ByteMessage message = new ByteMessage();
        message.writeToMessage(bytes);
        return message;
    }
}
