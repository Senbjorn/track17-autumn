package ru.track.prefork.nioserver.myserver;

import ru.track.prefork.nioserver.ByteMessage;
import ru.track.prefork.nioserver.MessageBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MyMessageBuffer implements MessageBuffer {

    private List<ByteMessage> buffer = null;

    public MyMessageBuffer() {
        buffer = new ArrayList<>();
    }

    @Override
    public void putMessage(ByteMessage message) {
        buffer.add(message);
    }

    @Override
    public ByteMessage getMessage() {
        if (buffer.size() == 0) return null;
        return buffer.remove(buffer.size() - 1);
    }

    @Override
    public List<ByteMessage> getMessages() {
        Iterator<ByteMessage> iter = buffer.iterator();
        List<ByteMessage> messages = new ArrayList<>();
        while (iter.hasNext()) {
            messages.add(iter.next());
            iter.remove();
        }
        return messages;
    }

    @Override
    public boolean isEmpty() {
        return buffer.size() == 0;
    }
}
