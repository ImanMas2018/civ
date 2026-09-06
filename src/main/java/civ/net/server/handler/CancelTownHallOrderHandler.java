package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.net.protocol.Message;
import civ.net.protocol.request.CancelTownHallOrderRequest;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class CancelTownHallOrderHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        CancelTownHallOrderRequest request = (CancelTownHallOrderRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        game.cancelTownHallOrder();
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
