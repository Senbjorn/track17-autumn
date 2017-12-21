package ru.track.prefork;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.SQL.SQLConnector;
import ru.track.prefork.nioserver.BinaryProtocol;
import ru.track.prefork.nioserver.Protocol;
import ru.track.prefork.nioserver.ProtocolException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class Server {
    public static Logger log = LoggerFactory.getLogger(Server.class);
    private ServerByteProtocol instance;
    private int port;
    private Protocol<Message> protocol;
    final private AtomicLong idCounter = new AtomicLong();
    final private ConcurrentMap<Long, DefaultUser> activeClients = new ConcurrentHashMap<>();
    final private ConcurrentMap<Long, Future<?>> activeClientsTasks = new ConcurrentHashMap<>();
    final private ConcurrentMap<Long, Connection> connections = new ConcurrentHashMap<>();
    final private ExecutorService pool = new ThreadPoolExecutor(20, 100,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2),
            (r) -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public Server(int port, Protocol<Message> protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    public void serve() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port, 10, InetAddress.getByName("localhost"));
        } catch (IOException e) {
            log.error("Host is not valid");
            return;
        }
        connections.put(0L, SQLConnector.getConnection("tdb-1.trail5.net:"));
        connections.put(1L, SQLConnector.getConnection("tdb-2.trail5.net:"));
        connections.put(2L, SQLConnector.getConnection("tdb-3.trail5.net:"));
        Handler handler = new Handler(serverSocket);
        handler.setDaemon(true);
        handler.start();
        Scanner adminIn = new Scanner(System.in);
        while(true) {
            String command = adminIn.nextLine();
            if (command.matches("^exit$")) {
                break;
            } else if (command.matches("^list$")) {
                activeClients.forEach((k, v) ->
                        System.out.println(String.format("Client[%d]@%s:%d(%s)",
                                k,
                                v.getSocket().getInetAddress(),
                                v.getSocket().getPort(),
                                v.getName()))
                );
            } else if (command.matches("^drop -id (?<id>\\d+)$")){
                Matcher m = Pattern.compile("^drop -id (?<id>\\d+)$").matcher(command);
                m.matches();
                try {
                    Long id = Long.parseLong(m.group("id"));
                    DefaultUser user = activeClients.get(id);
                    if (user == null) {
                        System.out.println("Id does not exist!");
                    } else {
                        removeWorker(id);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Incorrect number!");
                }
            } else {
                System.out.println("Unknown command!");
            }
        }
    }

    private long baseNumber(String userName) {
        Matcher m = Pattern.compile("(?<fl>[A-Za-z])").matcher(userName);//FIXME
        if (m.matches()) {
            char l = m.group("fl").toLowerCase().charAt(0);
            log.info("" + l);
            return (long) ((l - 'a') / 10);
        }
        return 0;
    }

    private void toBase(Message msg) {
        long n = baseNumber(msg.getAuthor());
        log.info("" + n);
//        Connection connection = connections.get(n);
//        String insert = "INSERT INTO messages (user_name, text, ts) VALUES (?, ?, now())";
//        PreparedStatement preparedStmt = null;
//        try {
//            preparedStmt = connection.prepareStatement(
//                    insert, Statement.RETURN_GENERATED_KEYS);
//            preparedStmt.setString(1, msg.getAuthor());
//            preparedStmt.setString(2, msg.getText());
//            preparedStmt.execute();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String... args) {
        Server server = new Server(8000, new BinaryProtocol<>());
        server.serve();
    }


    class Worker implements Runnable {

        private DefaultUser user;

        public Worker(DefaultUser user) {
            this.user = user;
        }

        public Socket getSocket() {
            return user.getSocket();
        }

        @Override
        public void run() {
            Thread.currentThread().setName(
                    String.format("Client[%d]@%s:%d",
                    user.getId(),
                    user.getSocket().getInetAddress(),
                    user.getSocket().getPort()));
            try {
                handleSocket(user.getSocket(), user.getId());
            } catch (IOException e) {
                log.error(e.getClass().getName() + ": " + e.getMessage());
            } catch (ProtocolException e) {
                log.error(e.getClass().getName() + ": " + e.getMessage());
            } catch (ServerByteProtocolException e) {
                log.error(e.getClass().getName() + ": " + e.getMessage());
            }
            broadcastQuietly(new Message("Client " + activeClients.get(user.getId()).getName() + " left the server."), user.getId());
            activeClients.remove(user.getId());
            activeClientsTasks.remove(user.getId());
            IOUtils.closeQuietly(getSocket());
            log.info("Connection closed.");
        }
    }


    private void broadcast(Message msg, long id) throws IOException,
            ProtocolException {
        for (DefaultUser user: activeClients.values()) {
            try {
                if (user.getId() == id) continue;
                ServerByteProtocol sbp = new ServerIOByteProtocol(user.getSocket());
                sbp.write(protocol.encode(msg));
            }
            catch (ServerByteProtocolException e) {}
        }
    }

    private void broadcastQuietly(Message msg, long id) {
        try {
            broadcast(msg, id);
        } catch (Exception e) {}
    }

    private void handleSocket(Socket socket, long id) throws IOException, ProtocolException, ServerByteProtocolException {
        ServerByteProtocol serverByteProtocol = new ServerIOByteProtocol(socket);
        serverByteProtocol.write(protocol.encode(new Message("Enter your name")));
        log.info("Setting name...");
        activeClients.get(id).setName(protocol.decode(serverByteProtocol.read()).getText());
        Message name = new Message(System.currentTimeMillis(), "Client " + activeClients.get(id).getName() + " have joined the server.");
        broadcast(name, id);
        while(!Thread.currentThread().isInterrupted()) {
            log.info("Reading line...");
            byte[] buffer = serverByteProtocol.read();
            Message msg = protocol.decode(buffer);
            if (msg.getText().equals("exit")) break;
            msg = new Message(msg.getTime(), activeClients.get(id).getName(), msg.getText());
            log.info(msg.toString());
            log.info("Broadcasting...");
            toBase(msg);
            broadcast(msg, id);
        }
        log.info("On exit...");
    }


    class Handler extends Thread {

        ServerSocket serverSocket;

        public Handler(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    log.info("Client accepted");
                    addWorker(socket);
                } catch(IOException e){
                    log.info("connection failed.");
                }
            }
        }
    }


    private void addWorker(Socket socket) {
        long t = System.currentTimeMillis();
        DefaultUser client = new DefaultUser(idCounter.getAndIncrement(), t, t, socket, "AnonymousUser");
        activeClients.put(client.getId(), client);
        activeClientsTasks.put(client.getId(), pool.submit(new Worker(client)));
    }

    private void removeWorker(long id) {
        activeClientsTasks.get(id).cancel(true);
        IOUtils.closeQuietly(activeClients.get(id).getSocket());
    }
}
