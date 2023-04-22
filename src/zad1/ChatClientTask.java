/**
 * @author Deska Mateusz S22176
 */

package zad1;


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


public class ChatClientTask extends FutureTask<ChatClient> {
    public ChatClientTask(Callable<ChatClient> callable) {
        super(callable);
    }

    public static ChatClientTask create(ChatClient client, List<String> messages, int wait) {
        return new ChatClientTask(() -> {
            client.login();
            sleepFor(wait);
            for (String message : messages) {
                client.sendMessage(message);
                sleepFor(wait);
            }
            client.logout();
            sleepFor(wait);
            return client;
        });
    }

    private static void sleepFor(int wait) throws InterruptedException {
        int sleepTimeInMillis = Math.max(wait, 100);
        Thread.sleep(sleepTimeInMillis);
    }

    public ChatClient getClient() {
        try {
            return get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
