package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import civ.net.server.ClientHandler;
import civ.net.server.GameBuilder;
import civ.net.server.Lobby;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;
import java.io.IOException;

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
        if (session.getGame() != null) {
            client.send(new ErrorResponse("The game has already started."));
            return;
        }

        Game game;
        try {
            game = GameBuilder.build(lobby);
        } catch (IOException ex) {
            client.send(new ErrorResponse("Could not load the selected map."));
            return;
        }

        session.setGame(game);

        int index = 0;
        for (Lobby.Seat seat : lobby.getSeats().values()) {
            ClientHandler handler = session.getClients().byClientId(seat.clientId);
            if (handler != null && index < game.getPlayers().size()) {
                Player player = game.getPlayers().get(index);
                handler.setPlayerId(player.getId());
                player.setHost(seat.host);
            }
            index++;
        }

        client.send(new OkResponse().withRequestId(request.getRequestId()));
        session.getClients().broadcast(new NoticePush("The game has started."));
        StateFilter.broadcast(session);
    }
}
