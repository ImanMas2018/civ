package civ;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Unit;
import civ.net.client.SnapshotApplier;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.MessageCodec;
import civ.net.protocol.dto.GameStateDto;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.GameStateBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.EndTurnRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.MoveUnitRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.request.StartGameRequest;
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

/** Headless smoke test for lobby, chat, and authoritative gameplay. */
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

            a.send(new ReadyRequest(true));
            b.send(new ReadyRequest(true));
            a.awaitAllReady(3);
            b.awaitAllReady(3);

            a.send(new ChatRequest("hello \"world\" \\ emoji 🙂"));
            ChatBroadcast chat = b.await(ChatBroadcast.class, 3);
            assertTrue("Alice".equals(chat.getSender()), "chat sender");
            assertTrue(chat.getText().contains("hello"), "chat text");

            a.send(new StartGameRequest());
            GameStateBroadcast aliceStart = a.await(GameStateBroadcast.class, 5);
            GameStateBroadcast bobStart = b.await(GameStateBroadcast.class, 5);
            GameStateDto aliceDto = aliceStart.getState();
            GameStateDto bobDto = bobStart.getState();

            assertTrue(aliceDto.yourPlayerId != bobDto.yourPlayerId, "distinct player ids");
            assertTrue(!aliceDto.hexes.isEmpty(), "Alice fog hexes");
            assertTrue(!bobDto.hexes.isEmpty(), "Bob fog hexes");
            assertTrue(!aliceDto.units.isEmpty(), "Alice starting units");
            assertTrue(aliceDto.turn == 1, "turn 1");

            // Anti-cheat: Alice's snapshot must not contain Bob's units by default
            // (spawns are far apart on Crossroads).
            long bobId = bobDto.yourPlayerId;
            boolean aliceSeesBobUnit = aliceDto.units.stream().anyMatch(u -> u.ownerId == bobId);
            assertTrue(!aliceSeesBobUnit, "Alice must not see Bob's units at spawn");

            Game aliceGame = SnapshotApplier.createShell(aliceDto);
            Unit mover = aliceGame.getViewpointPlayer().getEmpire().getUnits().get(0);
            Hex from = aliceGame.hexOf(mover);
            Hex found = null;
            for (Hex neighbour : aliceGame.getMap().neighbours(from)) {
                if (aliceGame.canMove(mover, neighbour)) {
                    found = neighbour;
                    break;
                }
            }
            assertTrue(found != null, "Alice should have a legal move");
            final Hex target = found;
            a.send(new MoveUnitRequest(mover.getId(), target.getCol(), target.getRow()));
            GameStateBroadcast afterMove = a.await(GameStateBroadcast.class, 5);
            assertTrue(afterMove.getState().units.stream()
                            .anyMatch(u -> u.id == mover.getId()
                                    && u.col == target.getCol()
                                    && u.row == target.getRow()),
                    "unit moved in snapshot");

            Game bobGame = SnapshotApplier.createShell(bobDto);
            Unit bobUnit = bobGame.getViewpointPlayer().getEmpire().getUnits().get(0);
            b.send(new MoveUnitRequest(bobUnit.getId(), bobUnit.getCol(), bobUnit.getRow()));
            ErrorResponse notTurn = b.await(ErrorResponse.class, 3);
            assertTrue(Errors.NOT_YOUR_TURN.equals(notTurn.getReason()),
                    "out-of-turn: " + notTurn.getReason());

            a.send(new EndTurnRequest());
            GameStateBroadcast afterEnd = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                GameStateBroadcast broadcast = b.await(GameStateBroadcast.class, 2);
                if (broadcast.getState().currentPlayerId == bobDto.yourPlayerId) {
                    afterEnd = broadcast;
                    break;
                }
            }
            assertTrue(afterEnd != null, "turn passed to Bob");

            ClientProbe c = ClientProbe.connectRaw("127.0.0.1", port);
            c.send(new JoinRequest("Late"));
            ErrorResponse late = c.await(ErrorResponse.class, 3);
            assertTrue(late.getReason().toLowerCase().contains("started"),
                    "late join: " + late.getReason());

            b.close();
            NoticePush notice = null;
            long disconnectDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < disconnectDeadline) {
                NoticePush push = a.await(NoticePush.class, 2);
                if (push.getText().toLowerCase().contains("disconnect")) {
                    notice = push;
                    break;
                }
            }
            assertTrue(notice != null && notice.getText().toLowerCase().contains("bob"),
                    "disconnect notice: " + (notice == null ? "null" : notice.getText()));

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
