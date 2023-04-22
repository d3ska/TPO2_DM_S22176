package zad1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;

public class BufferReader {

    protected int readData(Lock lock, SocketChannel channel, ByteBuffer buffer) {
        int bytesRead = 0;
        try {
            lock.lock();
            bytesRead = channel.read(buffer);
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            lock.unlock();
        }

        return bytesRead;
    }

    protected String decodeBufferToStringUntilNewLine(ByteBuffer buffer) {
        Charset charset = StandardCharsets.UTF_8;
        buffer.flip();
        StringBuilder stringBuilder = new StringBuilder();
        charset.decode(buffer)
                .chars()
                .mapToObj(c -> (char) c)
                .filter(c -> c != '\n')
                .forEachOrdered(stringBuilder::append);

        return stringBuilder.toString();
    }
}
