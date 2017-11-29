package ru.track.prefork;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class Server {
    public static Logger log = LoggerFactory.getLogger(Server.class);

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void serve() {
        ServerSocket serverSocket = null;
        AtomicLong id = new AtomicLong();
        try {
            serverSocket = new ServerSocket(port, 10, InetAddress.getByName("localhost"));
        } catch (IOException e) {
            System.out.print("host is not valid");
        }
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                final long clientId = id.getAndIncrement();
                final Socket clientSocket = socket;
                String idStr = "ClientId[" + clientId + "]@" + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
                Thread client = new Thread(() -> {
                    try {
                        InputStream input = clientSocket.getInputStream();
                        OutputStream output = clientSocket.getOutputStream();

                        log.info("Client  accepted.");
                        log.info("Reading line...");

                        String line;
                        byte[] buffer = new byte[1024];
                        int nRead = 0;

                            nRead = input.read(buffer);

                        while(nRead != -1) {
                            line = new String(buffer, 0, nRead);
                            log.info("Line: " + line);
                            log.info("Writing...");
                            output.write(line.getBytes());
                            output.flush();
                            nRead = input.read(buffer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        IOUtils.closeQuietly(clientSocket);
                    }
                    log.info("Connection closed!");
                });
                client.setName(idStr);
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String... args) {
        Server server = new Server(8000);
        server.serve();
    }
}
