/**
 * @author Deska Mateusz S22176
 */

package zad1;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatClient extends Thread {
    private SocketChannel channel;

    private final BufferReader bufferReader;

    private final String id;
    private final List<String> logs;
    private final Lock lock;
    private final String host;
    private final int port;

    public ChatClient(String host, int port, String id) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.lock = new ReentrantLock();
        this.logs = new ArrayList<>();
        this.logs.add(String.format("\n=== %s chat view", id));
        this.bufferReader = new BufferReader();
    }

    public void login() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            channel.configureBlocking(false);
            sendMessage(EventType.LOGIN, id);
            start();
        } catch (Exception e) {
            this.logException(e);
        }
    }

    public void logout() {
        try {
            sendMessage(EventType.LOGOUT, this.id);
            logs.add(String.format("%s logged out", this.id));
            lock.lock();
            interrupt();
        } catch (Exception e) {
            logException(e);
        } finally {
            lock.unlock();
        }
    }

    public void sendMessage(String request) {
        sendMessage(EventType.MESSAGE, request);
    }

    private void sendMessage(EventType event, String data) {
        try {
            String requestString = String.format("%s\t%s", event.getName(), data);
            channel.write(StandardCharsets.UTF_8.encode(requestString + '\n'));
        } catch (Exception e) {
            logException(e);
        }
    }

    public String getChatView() {
        return String.join("\n", logs);
    }

    public void run() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        int bytesRead;

        while (!isInterrupted()) {
            bytesRead = bufferReader.readData(lock, channel, buffer);
            if (bytesRead > 0) {
                String response = bufferReader.decodeBufferToStringUntilNewLine(buffer);
                addResponseToLogs(response);
            }
            buffer.clear();
        }
    }


    private void addResponseToLogs(String response) {
        if (!response.isEmpty()) {
            logs.add(response);
        }
    }

    private void logException(Exception exception) {
        logs.add(String.format("%s", exception.toString()));
    }

}
