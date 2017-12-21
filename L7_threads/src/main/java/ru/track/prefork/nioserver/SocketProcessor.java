package ru.track.prefork.nioserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.Message;
import ru.track.prefork.SQL.SQLConnector;
import ru.track.prefork.commands.*;
import ru.track.prefork.nioserver.myserver.MyMessageBuffer;
import ru.track.prefork.SQL.ConversationService;
import ru.track.prefork.nioserver.myserver.MySQLService;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.sql.SQLException;
import java.util.*;

public class SocketProcessor implements Runnable{

    static final Logger log = LoggerFactory.getLogger(SocketProcessor.class);

    private Long counterId;
    private Queue<InnerSocket> socketQueue;
    private Protocol<Message> protocol;
    private MessageWriterFactory messageWriterFactory;
    private MessageReaderFactory messageReaderFactory;
    private Map<Long, InnerSocket> connected;
    private Selector selector = null;
    private CommandManager commandManager = null;
    private ConversationService service = null;

    //it's just for testing, bad approach
    private Map<Long, InnerSocket> authorized;
    private Map<Long, String> passwords;
    private Map<String, Long> connectionsByName;


    public SocketProcessor(Queue<InnerSocket> socketQueue, Protocol<Message> protocol, MessageReaderFactory mrf, MessageWriterFactory mwf) {
        counterId = 0L;
        connected = new HashMap<>();
        authorized = new HashMap<>();
        passwords = new HashMap<>();
        connectionsByName = new HashMap<>();
        this.socketQueue = socketQueue;
        this.protocol = protocol;
        messageReaderFactory = mrf;
        messageWriterFactory = mwf;
        initCommandManager();
    }

    public void run() {
        try {
            selector = Selector.open();
            service = new MySQLService(
                    SQLConnector.getConnection("tdb-1.trail5.net:"),
                    SQLConnector.getConnection("tdb-2.trail5.net:"),
                    SQLConnector.getConnection("tdb-2.trail5.net:")
            );
            log.info("starting working cycle");
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    addNewSockets();
                    selector.select(1000);
                    rwSockets();
                    process();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            log.info("connection to database failed");
        }
        log.info("processing finished");
    }

    public void wakeup() {
        if (selector != null && selector.isOpen())
            selector.wakeup();
    }

    public void dropUser(long id) {
        if (!connected.containsKey(id)) {
            return;
        }
        InnerSocket user = connected.get(id);

    }

    private void addNewSockets() {
        InnerSocket innerSocket = socketQueue.poll();
        while (innerSocket != null) {
            log.info("adding new socket");
            MessageReader mr = messageReaderFactory.instantiateReader();
            mr.init(new MyMessageBuffer());
            MessageWriter mw = messageWriterFactory.instantiateWriter();
            mw.init(new MyMessageBuffer());
            innerSocket.setSocketId(counterId++);
            innerSocket.setMessageReader(mr);
            innerSocket.setMessageWriter(mw);
            connected.put(innerSocket.getSocketId(), innerSocket);
            try {
                innerSocket.getSocketChannel().configureBlocking(false);
                innerSocket.registerReader(selector);
                log.info(getClientInfo(innerSocket) + " is accepted");
                sendMessage(innerSocket.getSocketId(), new Message("SERVER", "authorization"));
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            innerSocket = socketQueue.poll();
        }
    }

    private void rwSockets() {
        try {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keys = selectionKeys.iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                InnerSocket mc = ((InnerSocket)key.attachment());
                if (mc.isClosed()) {
                    mc.getSocketChannel().socket().close();
                    mc.getSocketChannel().close();
                    log.info(getClientInfo(mc));
                    connected.remove(mc.getSocketId());
                    key.attach(null);
                    key.cancel();
                    keys.remove();
                    continue;
                }
                if (key.isReadable()) {
                    log.info("reading from socket");
                    mc.getMessageReader().read(mc);
                    if (!key.isWritable()) keys.remove();
                }
                if (key.isWritable()) {
                    log.info("writing to socket:" + mc.getSocketChannel().getRemoteAddress());
                    mc.getMessageWriter().write(mc, selector);
                    if (!key.isReadable()) keys.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void process() throws ProtocolException, IOException{
//        log.info("in process");
        for (InnerSocket innerSocket : connected.values()) {
            List<ByteMessage> messagesFrom = innerSocket.getMessageReader().getMessages();
            for (ByteMessage message: messagesFrom) {
                Message messageObject = protocol.decode(message.getByteArray());
                try {
                    log.info("Message text=" + messageObject.getText());
                    CommandInstance ci = commandManager.paresCommand(messageObject.getText());
                    commandManager.handleCommand(ci, getServerMessageHandler(
                            ci,
                            innerSocket.getSocketId(),
                            messageObject.getTime(),
                            messageObject.getAuthor()
                    ));
                    if (!ci.getCommand().getCommandName().equals("response")) {
                        Map<String, Object> responseParam = new HashMap<>();
                        responseParam.put("command", ci.getCommand().getCommandName());
                        responseParam.put("message", "OK");
                        String text = commandManager.getCommand("response").generateCommand(responseParam);
                        Message responseMessage = new Message("SERVER", text);
                        sendMessage(innerSocket.getSocketId(), responseMessage);
                    }
                } catch (CommandManagerException e) {
                    Map<String, Object> responseParam = new HashMap<>();
                    responseParam.put("command", "unknown");
                    responseParam.put("message", "ERROR: " + e.getMessage());
                    String text = commandManager.getCommand("response").generateCommand(responseParam);
                    Message responseMessage = new Message("SERVER", text);
                    sendMessage(innerSocket.getSocketId(), responseMessage);
                    e.printStackTrace();
                }
                log.info("handled");
            }
        }
    }

    private void sendMessage(long id, Message msg) throws IOException, ProtocolException {
        InnerSocket innerSocket = connected.get(id);
        innerSocket.getMessageWriter().put(innerSocket.getMessageWriter().convert(protocol.encode(msg)), innerSocket, selector);
    }

    private String getClientInfo(InnerSocket mc) {
        return String.format(
                "Client[%d]@%s:%d is closed",
                mc.getSocketId(),
                mc.getSocketChannel().socket().getInetAddress(),
                mc.getSocketChannel().socket().getPort()
        );
    }

    private void authRequired(String username) throws CommandHandlerException {
        if (! connectionsByName.containsKey(username)) {
            throw new CommandHandlerException("only for registered users");
        }
    }

    private void initCommandManager() {
        commandManager = new CommandManager();
        //login
        //register
        //request
        //send command +
        commandManager.addCommand(ServerCommands.commandSend());
        commandManager.addCommand(ServerCommands.commandAuthorization());
        commandManager.addCommand(ServerCommands.commandResponse());
        commandManager.addCommand(ServerCommands.commandMessage());
        commandManager.addCommand(ServerCommands.commandRegister());
        commandManager.addCommand(ServerCommands.commandLogin());
    }

    private class ServerMessageHandlerModel implements CommandHandler {

        String author;
        long id;
        long ts;

        public ServerMessageHandlerModel(long id, long ts, String author) {
            this.id = id;
            this.ts = ts;
            this.author = author;
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {}

    }

    private CommandHandler getServerMessageHandler(CommandInstance ci, long id, long ts, String author) {
        switch (ci.getCommand().getCommandName()){
            case "send":
                return new ServerSendHandler(id, ts, author);
            case "login":
                return new ServerLoginHandler(id, ts, author);
            case "register":
                return new ServerRegisterHandler(id, ts, author);
            case "response":
                return new ServerResponseHandler(id, ts, author);
            default:
                return new ServerCommands.UnavailableCommandHandler();
        }
    }

    private class ServerSendHandler extends ServerMessageHandlerModel {

        public ServerSendHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            log.info("Send request author: " + author);
            authRequired(author);
            String username = (String) options.get("username");
            if (!connectionsByName.containsKey(username)) throw new CommandHandlerException("unknown username");
            String text = (String) options.get("text");
            Map<String, Object> map = new HashMap<>();
            map.put("text", text);
            String commandText = commandManager.getCommand("message").generateCommand(map);
            try {
                sendMessage(connectionsByName.get(username), new Message(ts, author, commandText));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerResponseHandler extends ServerMessageHandlerModel {

        public ServerResponseHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            String comm = (String) options.get("command");
            String message = (String) options.get("message");
            log.info("response: " + comm + "; message: " + message);
//            Map<String, Object> responseParam = new HashMap<>();
//            responseParam.put("command", "response");
//            responseParam.put("message", "balls");
//            String text = commandManager.getCommand("response").generateCommand(responseParam);
//            Message responseMessage = new Message(author, text);
//            try {
//                sendMessage(id, responseMessage);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (ProtocolException e) {
//                e.printStackTrace();
//            }
        }

    }

    private class ServerRegisterHandler extends ServerMessageHandlerModel {

        public ServerRegisterHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            String login = (String) options.get("login");
            if (connectionsByName.containsKey(login) || connectionsByName.containsKey(author)) {
                throw new CommandHandlerException("already authorized");
            }
            String password = (String) options.get("password");
            String confirmPassword = (String) options.get("confirmpassword");
            if (password.equals(confirmPassword)) {
                connectionsByName.put(login, id);
                passwords.put(id, password);
                Map<String, Object> responseParam = new HashMap<>();
                responseParam.put("command", "register");
                responseParam.put("message", "OK");
                responseParam.put("authorised", new Object[]{id, login});
                String text = commandManager.getCommand("response").generateCommand(responseParam);
                Message responseMessage = new Message("SERVER", text);
                try {
                    sendMessage(id, responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            } else {
                throw new CommandHandlerException("passwords don't match each other");
            }
        }

    }

    private class ServerLoginHandler extends ServerMessageHandlerModel {

        public ServerLoginHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            if (connectionsByName.containsKey(author)) {
                throw new CommandHandlerException("already authorized");
            }
            String login = (String) options.get("login");
            String password = (String) options.get("password");
            if (!connectionsByName.containsKey(login)) {
                throw new CommandHandlerException("unknown login");
            } else if (!passwords.get(connectionsByName.get(login)).equals(password)) {
                throw new CommandHandlerException("wrong password");
            } else if (passwords.get(connectionsByName.get(login)).equals(password) && !connected.containsKey(connectionsByName.get(login))) {
                connectionsByName.put(login, id);
                passwords.put(id, password);
                Map<String, Object> responseParam = new HashMap<>();
                responseParam.put("command", "login");
                responseParam.put("message", "OK");
                responseParam.put("authorised", new Object[]{id, login});
                String text = commandManager.getCommand("response").generateCommand(responseParam);
                Message responseMessage = new Message("SERVER", text);
                try {
                    sendMessage(id, responseMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }

            } else {
                throw new CommandHandlerException("active user");
            }
        }
    }

}
