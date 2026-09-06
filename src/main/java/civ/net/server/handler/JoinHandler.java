package civ.net.server.handler;

import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;

public class JoinHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        JoinRequest request = (JoinRequest) message;

        if (session.getGame() != null) {
            client.send(new ErrorResponse("The game has already started."));
            return;
        }

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            client.send(new ErrorResponse("Please choose a username."));
            return;
        }
        if (name.length() > 24) {
            name = name.substring(0, 24);
        }

        Lobby lobby = session.getLobby();
        if (lobby.getSeat(client.getClientId()) != null) {
            client.send(new ErrorResponse("You have already joined the lobby."));
            return;
        }
        if (lobby.nameTaken(name)) {
            client.send(new ErrorResponse(Errors.NAME_TAKEN));
            return;
        }

        lobby.join(client.getClientId(), name);
        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(session.buildLobbyState());
    }
}
