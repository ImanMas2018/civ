package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.push.NoticePush;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class DisconnectHandler {

    public void handle(ServerSession session, ClientHandler client) {
        Game game = session.getGame();

        if (game == null) {
            String name = session.getLobby().nameOf(client.getClientId());
            boolean wasSeated = session.getLobby().getSeat(client.getClientId()) != null;
            session.getLobby().leave(client.getClientId());

            if (wasSeated) {
                session.getClients().broadcast(new NoticePush(name + " has disconnected."));
                session.getClients().broadcast(session.buildLobbyState());
            }
            return;
        }

        Player player = session.playerOf(client);
        if (player == null) {
            return;
        }

        player.setConnected(false);
        session.getClients().broadcast(new NoticePush(player.getName() + " has disconnected."));

        if (game.isTurnOf(player)) {
            session.getClients().broadcast(new NoticePush(
                    "Ending " + player.getName() + "'s turn automatically."));
            EndTurnHandler.endTurnFor(session, game, player);
        } else {
            StateFilter.broadcast(session);
        }
    }
}
