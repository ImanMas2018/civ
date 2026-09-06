package civ.net.server.handler;

import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;

/**
 * Step 2 validates start rules on the server. Building the Game and broadcasting
 * fog-filtered state is Step 3.
 */
public class StartGameHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        StartGameRequest request = (StartGameRequest) message;
        Lobby lobby = session.getLobby();

        if (!lobby.isHost(client.getClientId())) {
            client.send(new ErrorResponse(Errors.HOST_ONLY_START));
            return;
        }
        if (!lobby.allReady()) {
            client.send(new ErrorResponse(Errors.NOT_ALL_READY));
            return;
        }
        if (lobby.getSeats().size() < 2) {
            client.send(new ErrorResponse("You need at least two players."));
            return;
        }

        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(new NoticePush(
                "All players ready — authoritative gameplay starts in Step 3."));
    }
}
