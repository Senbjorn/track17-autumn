package ru.track.prefork.nioserver;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class InnerSocket {

    private long socketId = 0;
    private SocketChannel socketChannel = null;
    private MessageWriter messageWriter = null;
    private MessageReader messageReader = null;
    private boolean isClosed = false;

    public InnerSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public InnerSocket(long socketId, SocketChannel sc, MessageWriter mw, MessageReader mr) {
        this.socketId = socketId;
        socketChannel = sc;
        messageWriter = mw;
        messageReader = mr;
    }

    public SelectionKey registerWriter(Selector selector) throws ClosedChannelException {
        return socketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
    }

    public SelectionKey registerReader(Selector selector) throws ClosedChannelException {
        return socketChannel.register(selector, SelectionKey.OP_READ, this);
    }

    public long getSocketId() {
        return socketId;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public MessageWriter getMessageWriter() {
        return messageWriter;
    }

    public MessageReader getMessageReader() {
        return messageReader;
    }

    public void setSocketId(long socketId) {
        this.socketId = socketId;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setMessageWriter(MessageWriter messageWriter) {
        this.messageWriter = messageWriter;
    }

    public void setMessageReader(MessageReader messageReader) {
        this.messageReader = messageReader;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() {
        isClosed = true;
    }


}
