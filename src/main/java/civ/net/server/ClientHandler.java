package civ.net.server;

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

public class ClientHandler {

    /** Put on the queue to tell the writer thread to stop. */
    private static final Message POISON = new ErrorResponse("__shutdown__");

    private final Socket socket;
    private final ServerSession session;
    private final ClientRegistry clients;
    private final MessageCodec codec = new MessageCodec();
    private final BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();

    private final String clientId = UUID.randomUUID().toString();
    private volatile long playerId = -1;
    private volatile boolean open = true;

    public ClientHandler(Socket socket, ServerSession session, ClientRegistry clients) {
        this.socket = socket;
        this.session = session;
        this.clients = clients;
    }

    public String getClientId() {
        return clientId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public void start() {
        clients.add(this);
        new Thread(this::readLoop, "reader-" + clientId).start();
        new Thread(this::writeLoop, "writer-" + clientId).start();
    }

    /** Never blocks. Safe to call from the game thread while broadcasting. */
    public void send(Message message) {
        if (open) {
            outbox.offer(message);
        }
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while (open && (line = in.readLine()) != null) {
                try {
                    Message message = codec.decode(line);
                    session.handle(this, message);
                } catch (IllegalArgumentException bad) {
                    send(new ErrorResponse("Malformed request: " + bad.getMessage()));
                }
            }
            } catch (IOException ex) {
                // Dropped connection is normal.
            } finally {
                close();
            }
        }

    private void writeLoop() {
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            while (open) {
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
        } finally {
            close();
        }
    }

    public synchronized void close() {
        if (!open) {
            return;
        }
        open = false;
        outbox.offer(POISON);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        clients.remove(this);
        session.onDisconnect(this);
    }
}
