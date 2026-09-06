package civ.net.server.handler;

import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.request.SetCheatsRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;

public class SetCheatsHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        SetCheatsRequest request = (SetCheatsRequest) message;
        Lobby lobby = session.getLobby();

        if (session.getGame() != null) {
            client.send(new ErrorResponse("Cheats can only be toggled in the lobby."));
            return;
        }
        if (!lobby.isHost(client.getClientId())) {
            client.send(new ErrorResponse(Errors.HOST_ONLY));
            return;
        }

        lobby.setCheatsEnabled(request.isEnabled());
        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(session.buildLobbyState());
    }
}
