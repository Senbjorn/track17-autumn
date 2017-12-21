package ru.track.prefork.nioserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class SocketAcceptor implements Runnable {

    static final Logger log = LoggerFactory.getLogger(SocketAcceptor.class);

    private InetSocketAddress address;
    private int backlog;
    private Queue<InnerSocket> socketQueue;

    public SocketAcceptor(InetSocketAddress address, int backlog, Queue<InnerSocket> socketChannels) {
        socketQueue = socketChannels;
        this.address = address;
        this.backlog = backlog;
    }

    public void run() {
        try {
            log.info("opening of server socket...");
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(address, backlog);
            log.info("working cycle");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("on accept");
                    SocketChannel socket = serverSocketChannel.accept();
                    socketQueue.add(new InnerSocket(socket));
                    log.info("client queued");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
