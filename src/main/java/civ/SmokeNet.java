package civ;

import civ.net.protocol.Message;
import civ.net.protocol.MessageCodec;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.GameServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Headless smoke test for Step 2 lobby + chat (no Swing). */
public final class SmokeNet {

    public static void main(String[] args) throws Exception {
        int port = 19555;
        GameServer server = new GameServer(port);
        server.start();

        try {
            ClientProbe a = ClientProbe.connect("127.0.0.1", port, "Alice");
            ClientProbe b = ClientProbe.connect("127.0.0.1", port, "Bob");

            a.awaitLobbySeats(2, 3);
            b.awaitLobbySeats(2, 3);
            assertTrue(a.lastLobby().getSeats().stream().anyMatch(s -> s.host && "Alice".equals(s.name)),
                    "Alice should be host");
            assertTrue(b.lastLobby().getSeats().stream().anyMatch(s -> s.host && "Alice".equals(s.name)),
                    "Bob should see Alice as host");

            a.send(new ReadyRequest(true));
            b.send(new ReadyRequest(true));
            a.awaitAllReady(3);
            b.awaitAllReady(3);

            a.send(new ChatRequest("hello \"world\" \\ emoji 🙂"));
            ChatBroadcast chat = b.await(ChatBroadcast.class, 3);
            assertTrue("Alice".equals(chat.getSender()), "chat sender");
            assertTrue(chat.getText().contains("hello"), "chat text");
            assertTrue(chat.getTime() != null && chat.getTime().matches("\\d{2}:\\d{2}"), "server time");

            ClientProbe c = ClientProbe.connectRaw("127.0.0.1", port);
            c.send(new JoinRequest("Alice"));
            ErrorResponse dup = c.await(ErrorResponse.class, 3);
            assertTrue(dup.getReason().toLowerCase().contains("taken"), "duplicate name rejected");

            b.close();
            NoticePush notice = a.await(NoticePush.class, 3);
            assertTrue(notice.getText().contains("Bob") && notice.getText().contains("disconnected"),
                    "disconnect notice: " + notice.getText());
            a.awaitLobbySeats(1, 3);

            a.close();
            c.close();
            System.out.println("SmokeNet OK");
        } finally {
            server.stop();
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class ClientProbe {
        private final MessageCodec codec = new MessageCodec();
        private final Socket socket;
        private final BufferedWriter out;
        private final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
        private volatile LobbyStateBroadcast lobby;
        private final Thread reader;

        static ClientProbe connect(String host, int port, String name) throws Exception {
            ClientProbe probe = connectRaw(host, port);
            probe.send(new JoinRequest(name));
            probe.awaitJoined(name, 3);
            return probe;
        }

        static ClientProbe connectRaw(String host, int port) throws Exception {
            return new ClientProbe(host, port);
        }

        private ClientProbe(String host, int port) throws Exception {
            socket = new Socket(host, port);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Message message = codec.decode(line);
                        if (message instanceof LobbyStateBroadcast) {
                            lobby = (LobbyStateBroadcast) message;
                        }
                        inbox.offer(message);
                    }
                } catch (Exception ignored) {
                }
            }, "smoke-reader");
            reader.setDaemon(true);
            reader.start();
        }

        void send(Message message) throws Exception {
            message.withRequestId(UUID.randomUUID().toString());
            out.write(codec.encode(message));
            out.write('\n');
            out.flush();
        }

        LobbyStateBroadcast lastLobby() {
            return lobby;
        }

        void awaitJoined(String name, int seconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (System.nanoTime() < deadline) {
                LobbyStateBroadcast state = lobby;
                if (state != null && state.getSeats().stream().anyMatch(s -> name.equals(s.name))) {
                    return;
                }
                Thread.sleep(40);
            }
            throw new AssertionError(name + " did not appear in lobby");
        }

        void awaitLobbySeats(int count, int seconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (System.nanoTime() < deadline) {
                LobbyStateBroadcast state = lobby;
                if (state != null && state.getSeats().size() == count) {
                    return;
                }
                Thread.sleep(40);
            }
            throw new AssertionError("Timed out waiting for " + count + " seats; last="
                    + (lobby == null ? null : lobby.getSeats().size()));
        }

        void awaitAllReady(int seconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            while (System.nanoTime() < deadline) {
                LobbyStateBroadcast state = lobby;
                if (state != null && !state.getSeats().isEmpty()
                        && state.getSeats().stream().allMatch(s -> s.ready)) {
                    return;
                }
                Thread.sleep(40);
            }
            throw new AssertionError("Timed out waiting for all ready");
        }

        <T extends Message> T await(Class<T> type, int seconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            List<Message> deferred = new ArrayList<>();
            try {
                while (System.nanoTime() < deadline) {
                    Message message = inbox.poll(40, TimeUnit.MILLISECONDS);
                    if (message == null) {
                        continue;
                    }
                    if (type.isInstance(message)) {
                        return type.cast(message);
                    }
                    deferred.add(message);
                }
            } finally {
                inbox.addAll(deferred);
            }
            throw new AssertionError("Timed out waiting for " + type.getSimpleName()
                    + "; saw " + snapshotTypes());
        }

        List<String> snapshotTypes() {
            List<String> types = new ArrayList<>();
            for (Message message : inbox) {
                types.add(message.getType());
            }
            return types;
        }

        void close() throws Exception {
            try {
                out.flush();
                socket.shutdownOutput();
            } catch (Exception ignored) {
            }
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private SmokeNet() {
    }
}
