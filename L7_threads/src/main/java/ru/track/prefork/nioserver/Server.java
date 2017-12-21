package ru.track.prefork.nioserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.Message;
import ru.track.prefork.nioserver.myserver.MyMessageReader;
import ru.track.prefork.nioserver.myserver.MyMessageWriter;

import java.util.Scanner;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Server extends Thread{

    static final Logger log = LoggerFactory.getLogger(Server.class);

    private InetSocketAddress address;
    private int backlog;
    private SocketProcessor socketProcessor;
    private SocketAcceptor socketAcceptor;

    public Server(InetSocketAddress address, int backlog) {
        this.address = address;
        this.backlog = backlog;
    }

    public void init() {
        log.info("initializing server");
        Queue<InnerSocket> socketQueue = new ArrayBlockingQueue<>(1024);
        socketAcceptor = new SocketAcceptor(address, backlog, socketQueue);
        socketProcessor = new SocketProcessor(socketQueue, new BinaryProtocol<Message>(), ()->new MyMessageReader(), ()->new MyMessageWriter());
        Thread acceptor = new Thread(socketAcceptor);
        Thread processor = new Thread(socketProcessor);
        acceptor.setDaemon(true);
        processor.setDaemon(true);
        log.info("starting acceptor and processor");
        acceptor.start();
        processor.start();
        adminConsole();
    }

    public void adminConsole() {
        Scanner admin = new Scanner(System.in);
        while (true) {
            String line = admin.nextLine();
        }
    }

    public static void main(String... args) {
        try {
            Server ms = new Server(new InetSocketAddress(InetAddress.getByName("localhost"), 8000), 10);
            ms.init();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
