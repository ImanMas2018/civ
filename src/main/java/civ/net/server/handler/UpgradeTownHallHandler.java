package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.Message;
import civ.net.protocol.request.UpgradeTownHallRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class UpgradeTownHallHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        UpgradeTownHallRequest request = (UpgradeTownHallRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        if (!game.canUpgradeTownHall()) {
            client.send(new ErrorResponse("You cannot upgrade the Town Hall right now."));
            return;
        }
        game.upgradeTownHall();
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
