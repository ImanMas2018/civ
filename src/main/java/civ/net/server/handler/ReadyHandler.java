package civ.net.server.handler;

import civ.net.protocol.Message;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;

public class ReadyHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        ReadyRequest request = (ReadyRequest) message;
        Lobby.Seat seat = session.getLobby().getSeat(client.getClientId());
        if (seat == null) {
            client.send(new ErrorResponse("Join the lobby first."));
            return;
        }

        seat.ready = request.isReady();
        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(session.buildLobbyState());
    }
}
