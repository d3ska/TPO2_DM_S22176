/**
 * @author Deska Mateusz S22176
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer extends Thread {
    private static final String TIME_FORMAT = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    private final String host;
    private final int port;
    private final Map<SocketChannel, String> clients;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final Lock lock;
    private final List<String> logs;
    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.logs = new ArrayList<>();
        this.clients = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public void startServer() throws IOException {
        setUpServerSocketChannel();
        setUpSelector();
        start();
        System.out.println("\nServer started");
    }

    public void stopServer() throws IOException {
        try {
            lock.lock();
            interrupt();
            selector.close();
            serverChannel.close();
            System.out.println("\nServer stopped");
        } finally {
            lock.unlock();
        }
    }
    public void run() {
        while (!isInterrupted()) {
            try {
                processSelectedKeys();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void processSelectedKeys() throws IOException {
        selector.select();

        if (!isInterrupted()) {
            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (key.isAcceptable()) {
                    registerNewClientChannel();
                } else if (key.isReadable()) {
                    handleClientRequest(key);
                }
            }
        }
    }
    private void handleClientRequest(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        handleRequest(clientChannel);
    }
    private void registerNewClientChannel() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }
    private String readSocket(SocketChannel socketChannel) throws IOException {
        try {
            lock.lock();
            StringBuilder stringBuilder = new StringBuilder();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            Charset charset = StandardCharsets.UTF_8;
            boolean foundNewline = false;
            while (!foundNewline) {
                int n = socketChannel.read(buffer);
                if (n > 0) {
                    buffer.flip();
                    CharBuffer charBuffer = charset.decode(buffer);
                    while (charBuffer.hasRemaining() && !foundNewline) {
                        char c = charBuffer.get();
                        if (c == '\n') {
                            foundNewline = true;
                        } else {
                            stringBuilder.append(c);
                        }
                    }
                }
            }
            return stringBuilder.toString();
        } finally {
            lock.unlock();
        }
    }
    private void handleRequest(SocketChannel socketChannel) throws IOException {
        String incomingMessage = readSocket(socketChannel);
        Matcher matcher = matchIncomingMessage(incomingMessage);
        if (matcher.find()) {
            EventType eventType = EventType.valueOf(matcher.group(1).toUpperCase());
            String message = matcher.group(2);
            if (EventType.LOGIN == eventType) {
                handleLogin(socketChannel, message);
            }
            String clientId = clients.get(socketChannel);
            String formattedMessage = formatMessage(eventType, message, clientId);
            logs.add(String.format("%s %s", TIME_FORMAT, formattedMessage));
            broadcastToEveryNode(formattedMessage);
        }
    }
    private void handleLogin(SocketChannel socketChannel, String clientId) {
        clients.put(socketChannel, clientId);
    }

    private Matcher matchIncomingMessage(String incomingMessage) {
        Pattern pattern = Pattern.compile("([^\\t]+)\\t(.+)");
        return pattern.matcher(incomingMessage);
    }

    public String getServerLog() {
        return String.join("\n", this.logs);
    }

    private void broadcastToEveryNode(String response) {
        clients.keySet()
                .forEach(clientSocketChannel -> {
                    try {
                        clientSocketChannel.write(StandardCharsets.UTF_8.encode(response + '\n'));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private String formatMessage(EventType eventType, String message, String clientId) {
        switch (eventType) {
            case LOGIN: {
                return String.format("%s logged in", clientId);
            }
            case MESSAGE: {
                return String.format("%s: %s", clientId, message);
            }
            case LOGOUT: {
                return String.format("%s logged out", clientId);
            }
            default:
                logs.add(String.format("%s Unsupported event type %s", TIME_FORMAT, eventType));
                return "";
        }
    }

    private void setUpServerSocketChannel() throws IOException {
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.socket().bind(new InetSocketAddress(this.host, this.port));
        this.serverChannel.configureBlocking(false);
    }

    private void setUpSelector() throws IOException {
        this.selector = Selector.open();
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
}
