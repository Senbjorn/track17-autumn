package ru.track.prefork.nioserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.track.prefork.Message;
import ru.track.prefork.commands.*;
import ru.track.prefork.nioserver.myserver.MyMessageBuffer;
import ru.track.prefork.nioserver.myserver.MyMessageReader;
import ru.track.prefork.nioserver.myserver.MyMessageWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientService {

    static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private CommandManager commandManager;
    private Selector selector;
    private InetSocketAddress address;
    private MessageWriterFactory messageWriterFactory;
    private MessageReaderFactory messageReaderFactory;
    private Protocol<Message> protocol;
    private InnerSocket innerSocket;
    private String serverUsername = "Anonymous"; // use server data model
    private Long serverId = -1L;

    public ClientService(
            InetSocketAddress address,
            Protocol<Message> protocol,
            MessageReaderFactory mrf,
            MessageWriterFactory mwf
    )
    {
        this.address = address;
        this.protocol = protocol;
        messageReaderFactory = mrf;
        messageWriterFactory = mwf;
    }

    public void init() throws IOException {
        MessageWriter messageWriter = messageWriterFactory.instantiateWriter();
        MessageReader messageReader = messageReaderFactory.instantiateReader();
        messageReader.init(new MyMessageBuffer());
        messageWriter.init(new MyMessageBuffer());
        selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open(address);
        socketChannel.configureBlocking(false);
        innerSocket = new InnerSocket(socketChannel);
        innerSocket.setSocketId(0);
        innerSocket.setMessageReader(messageReader);
        innerSocket.setMessageWriter(messageWriter);
        selector = Selector.open();
        innerSocket.registerReader(selector);
        initCommandManager();
        Thread inputThread = new InputThread();
        inputThread.setDaemon(true);
        inputThread.start();
    }

    public void start() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (!innerSocket.isClosed()) {
            try {
                readWrite();
                process();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
        }
    }

    private void readWrite() throws IOException {
//        log.info("on select");
        int ready = selector.select(1000);
//        log.info("selected " + ready);
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> iter = keys.iterator();
        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            InnerSocket socket = (InnerSocket) key.attachment();
            if (key.isReadable()) {
                socket.getMessageReader().read(socket);
                if (!key.isWritable()) {
                    iter.remove();
                }
            }
            if (key.isWritable()) {
                socket.getMessageWriter().write(socket, selector);
                if (!key.isReadable()) {
                    iter.remove();
                }
            }
        }
    }
//register --password=1234 --confirmpassword=1234 --login=sema
//send --username=sema --text=hello
//login --password=1234 --login=sema
    private void process() throws IOException, ProtocolException {
//        log.info("on progress");
        List<ByteMessage> messageList = innerSocket.getMessageReader().getMessages();
        for (ByteMessage message: messageList) {
            Message messageObject = protocol.decode(message.getByteArray());
            try {
                log.info("Message: " + messageObject.getText());
                CommandInstance ci = commandManager.paresCommand(messageObject.getText());
                log.info("Command: " + ci.getCommand().getCommandName());
                commandManager.handleCommand(ci, getClientMessageHandler(
                        ci,
                        serverId,
                        messageObject.getTime(),
                        messageObject.getAuthor()
                ));
                if (!ci.getCommand().getCommandName().equals("response")) {
                    Map<String, Object> responseParam = new HashMap<>();
                    responseParam.put("command", ci.getCommand().getCommandName());
                    responseParam.put("message", "OK");
                    String text = commandManager.getCommand("response").generateCommand(responseParam);
                    Message responseMessage = new Message(serverUsername, text);
                    log.info("response: " + responseMessage.getText());
                    sendToWriter(responseMessage);
                }
            } catch (CommandManagerException e) {
                Map<String, Object> responseParam = new HashMap<>();
                responseParam.put("command", "unknown");
                responseParam.put("message", "ERROR");
                String text = commandManager.getCommand("response").generateCommand(responseParam);
                Message responseMessage = new Message(serverUsername, text);
                sendToWriter(responseMessage);
                e.printStackTrace();
            }
            log.info("handled");
        }
    }

    private void sendToWriter(Message msg) throws IOException, ProtocolException {
        innerSocket.getMessageWriter().put(
                innerSocket.getMessageWriter().convert(protocol.encode(msg)),
                innerSocket,
                selector
        );
    }

    public class InputThread extends Thread{

        @Override
        public void run() {
            log.info("console activated");
            Scanner input = new Scanner(System.in);
            while (!Thread.currentThread().isInterrupted()) {
                log.info("reading line");
                //work on exceptions
                try {
                    log.info("AUTHOR: " + serverUsername);
                    String text = input.nextLine();
                    sendToWriter(new Message(serverUsername, text));
                    selector.wakeup();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void initCommandManager() {
        commandManager = new CommandManager();
        commandManager.addCommand(ServerCommands.commandAuthorization());
        commandManager.addCommand(ServerCommands.commandSend());
        commandManager.addCommand(ServerCommands.commandResponse());
        commandManager.addCommand(ServerCommands.commandMessage());
        commandManager.addCommand(ServerCommands.commandRegister());
        commandManager.addCommand(ServerCommands.commandLogin());
    }

    public CommandHandler getClientMessageHandler(CommandInstance ci, long id, long ts, String author) {
        switch (ci.getCommand().getCommandName()){
            case "authorization":
                return new ClientAuthorizationHandler(id, ts, author);
            case "message":
                return new ClientMessageCommandHandler(id, ts, author);
            case "response":
                return new ClientResponseHandler(id, ts, author);
            default:
                return new ServerCommands.UnavailableCommandHandler();
        }
    }

    public class ClientMessageHandler implements CommandHandler {

        long id;
        long ts;
        String author;

        public ClientMessageHandler(long id, long ts, String author) {
            this.id = id;
            this.ts = ts;
            this.author = author;
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException { }
    }

    public class ClientAuthorizationHandler extends ClientMessageHandler {

        public ClientAuthorizationHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            System.out.println("Authorization required:\nuse register or login commands");
        }

    }

    public class ClientMessageCommandHandler extends ClientMessageHandler {

        public ClientMessageCommandHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            String text = (String) options.get("text");
            System.out.println(String.format("%s> %s", author, text));
        }

    }

    public class ClientResponseHandler  extends ClientMessageHandler {

        public ClientResponseHandler(long id, long ts, String author) {
            super(id, ts, author);
        }

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            String message = (String) options.get("message");
            System.out.println(String.format("%s> response: %s", author, message));
            if (options.containsKey("authorised")) {
                long id = (long) (((Object[]) options.get("authorised"))[0]);
                String login = (String) (((Object[]) options.get("authorised"))[1]);
                serverUsername = login;
                serverId = id;
                log.info("AUTHORISED!!!!!!!!!!!!!!!!!!!" + serverUsername);
            }
        }

    }

    public static void main(String... args) {
        try {
            ClientService cs = new ClientService(
                    new InetSocketAddress(InetAddress.getByName("localhost"), 8000),
                    new BinaryProtocol<>(),
                    ()->new MyMessageReader(),
                    ()->new MyMessageWriter()
            );
            cs.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
