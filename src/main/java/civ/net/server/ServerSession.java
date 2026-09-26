package civ.net.server;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.Message;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.GameOverBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.handler.DisconnectHandler;
import civ.net.server.handler.EndTurnHandler;
import civ.net.server.handler.RequestHandler;
import civ.net.ws.WebSocketChatServer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ServerSession {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_CHAT_LENGTH = 500;

    /** Everything that touches the model holds this. */
    private final Object lock = new Object();

    private final ClientRegistry clients;
    private final Lobby lobby = new Lobby();
    private final RequestRouter router;

    private Game game;
    private HeartbeatServer heartbeatServer;
    private WebSocketChatServer webSocketChat;
    private boolean gameOver;

    public ServerSession(ClientRegistry clients) {
        this.clients = clients;
        this.router = new RequestRouter(this);
    }

    public Object getLock() {
        return lock;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
        this.gameOver = false;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void markGameOver(String winnerName) {
        this.gameOver = true;
        clients.broadcast(new GameOverBroadcast(winnerName));
    }

    public ClientRegistry getClients() {
        return clients;
    }

    public void setHeartbeatServer(HeartbeatServer heartbeatServer) {
        this.heartbeatServer = heartbeatServer;
    }

    public void setWebSocketChat(WebSocketChatServer webSocketChat) {
        this.webSocketChat = webSocketChat;
    }

    /**
     * Single door for chat: Swing clients and browser tabs all land here so both
     * sides see the same line.
     */
    public void publishChat(String sender, String text) {
        String cleanSender = sender == null || sender.isBlank() ? "Unknown" : sender.trim();
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isEmpty()) {
            return;
        }
        if (cleanText.length() > MAX_CHAT_LENGTH) {
            cleanText = cleanText.substring(0, MAX_CHAT_LENGTH);
        }

        String time = LocalTime.now().format(CLOCK);
        ChatBroadcast broadcast = new ChatBroadcast(cleanSender, cleanText, time);
        clients.broadcast(broadcast);
        if (webSocketChat != null) {
            webSocketChat.broadcastLine("[" + time + "] " + cleanSender + ": " + cleanText);
        }
    }

    /** Browser messages arrive as {@code "Name: body"} or plain text. */
    public void publishBrowserChat(String raw) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        int colon = trimmed.indexOf(':');
        if (colon > 0 && colon < trimmed.length() - 1) {
            String name = trimmed.substring(0, colon).trim();
            String body = trimmed.substring(colon + 1).trim();
            if (!name.isEmpty() && !body.isEmpty()) {
                publishChat(name, body);
                return;
            }
        }
        publishChat("Browser", trimmed);
    }

    public void noteHeartbeat(long playerId) {
        if (heartbeatServer != null) {
            heartbeatServer.note(playerId);
        }
    }

    /**
     * Called by the UDP sweeper when a player has been silent for ~10 seconds.
     * Marks them disconnected and auto-ends their turn if needed.
     */
    public void markSilent(long playerId) {
        synchronized (lock) {
            if (game == null) {
                return;
            }
            Player player = game.getPlayer(playerId);
            if (player == null || !player.isConnected()) {
                return;
            }
            player.setConnected(false);
            clients.broadcast(new NoticePush(player.getName() + " has disconnected."));
            if (game.isTurnOf(player)) {
                clients.broadcast(new NoticePush(
                        "Ending " + player.getName() + "'s turn automatically."));
                EndTurnHandler.endTurnFor(this, game, player);
            } else {
                StateFilter.broadcast(this);
            }
        }
    }

    public void handle(ClientHandler client, Message message) {
        RequestHandler handler = router.handlerFor(message.getType());

        if (handler == null) {
            client.send(new ErrorResponse("Unsupported request: " + message.getType()));
            return;
        }

        synchronized (lock) {
            try {
                handler.handle(this, client, message);
            } catch (RuntimeException ex) {
                client.send(new ErrorResponse("The server could not process that request."));
                ex.printStackTrace();
            }
        }
    }

    public Player playerOf(ClientHandler client) {
        return game == null ? null : game.getPlayer(client.getPlayerId());
    }

    public String nameOf(ClientHandler client) {
        if (game != null) {
            Player player = playerOf(client);
            if (player != null) {
                return player.getName();
            }
        }
        return lobby.nameOf(client.getClientId());
    }

    public LobbyStateBroadcast buildLobbyState() {
        List<LobbyStateBroadcast.SeatDto> seats = new ArrayList<>();
        for (Lobby.Seat seat : lobby.getSeats().values()) {
            seats.add(new LobbyStateBroadcast.SeatDto(seat.name, seat.ready, seat.host));
        }
        return new LobbyStateBroadcast(seats, lobby.getSelectedMap(), lobby.isCheatsEnabled());
    }

    public void onDisconnect(ClientHandler client) {
        synchronized (lock) {
            if (heartbeatServer != null && client.getPlayerId() > 0) {
                heartbeatServer.forget(client.getPlayerId());
            }
            new DisconnectHandler().handle(this, client);
        }
    }
}
