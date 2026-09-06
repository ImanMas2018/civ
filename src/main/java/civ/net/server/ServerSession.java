package civ.net.server;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.Message;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.handler.DisconnectHandler;
import civ.net.server.handler.EndTurnHandler;
import civ.net.server.handler.RequestHandler;
import java.util.ArrayList;
import java.util.List;

public class ServerSession {

    /** Everything that touches the model holds this. */
    private final Object lock = new Object();

    private final ClientRegistry clients;
    private final Lobby lobby = new Lobby();
    private final RequestRouter router;

    private Game game;
    private HeartbeatServer heartbeatServer;

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
    }

    public ClientRegistry getClients() {
        return clients;
    }

    public void setHeartbeatServer(HeartbeatServer heartbeatServer) {
        this.heartbeatServer = heartbeatServer;
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
