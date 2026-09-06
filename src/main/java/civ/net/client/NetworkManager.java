package civ.net.client;

import civ.net.protocol.Message;
import civ.net.protocol.MessageCodec;
import civ.net.protocol.response.ErrorResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * The only class in the client that knows a socket exists.
 * The view and the controller talk to this; they never talk to java.net.
 */
public class NetworkManager {

    private static final Message POISON = new ErrorResponse("__shutdown__");

    private final MessageCodec codec = new MessageCodec();
    private final BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();

    private Socket socket;
    private volatile boolean connected = false;
    private volatile boolean intentionalDisconnect = false;

    /** Where incoming messages go. Always invoked on the EDT. */
    private Consumer<Message> onMessage = message -> {
    };
    private Runnable onConnectionLost = () -> {
    };

    public void setOnMessage(Consumer<Message> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnConnectionLost(Runnable action) {
        this.onConnectionLost = action;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect(String host, int port) throws IOException {
        intentionalDisconnect = false;
        socket = new Socket(host, port);
        connected = true;

        Thread listener = new Thread(this::listenLoop, "server-listener");
        listener.setDaemon(true);
        listener.start();

        Thread writer = new Thread(this::writeLoop, "server-writer");
        writer.setDaemon(true);
        writer.start();
    }

    /** Called from the EDT on every button click. Returns instantly. */
    public void send(Message message) {
        if (!connected) {
            return;
        }
        message.withRequestId(UUID.randomUUID().toString());
        outbox.offer(message);
    }

    private void writeLoop() {
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            while (connected) {
                Message message = outbox.take();
                if (message == POISON) {
                    break;
                }
                out.write(codec.encode(message));
                out.write('\n');
                out.flush();
            }
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void listenLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while (connected && (line = in.readLine()) != null) {
                final Message message = codec.decode(line);
                SwingUtilities.invokeLater(() -> onMessage.accept(message));
            }
        } catch (IOException ex) {
            // fall through
        } finally {
            boolean wasConnected = connected;
            connected = false;
            outbox.offer(POISON);
            if (wasConnected && !intentionalDisconnect) {
                SwingUtilities.invokeLater(onConnectionLost);
            }
        }
    }

    public void disconnect() {
        intentionalDisconnect = true;
        connected = false;
        outbox.offer(POISON);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
