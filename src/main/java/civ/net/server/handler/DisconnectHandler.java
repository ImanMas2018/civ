package civ.net.server.handler;

import civ.net.protocol.push.NoticePush;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;

public class DisconnectHandler {

    public void handle(ServerSession session, ClientHandler client) {
        if (session.getGame() != null) {
            // Step 3: mark player disconnected, skip turn, etc.
            return;
        }

        String name = session.getLobby().nameOf(client.getClientId());
        boolean wasSeated = session.getLobby().getSeat(client.getClientId()) != null;
        session.getLobby().leave(client.getClientId());

        if (wasSeated) {
            session.getClients().broadcast(new NoticePush(name + " has disconnected."));
            session.getClients().broadcast(session.buildLobbyState());
        }
    }
}
