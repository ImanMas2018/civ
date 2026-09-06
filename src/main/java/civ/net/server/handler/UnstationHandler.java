package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.Unit;
import civ.model.Worker;
import civ.net.protocol.Message;
import civ.net.protocol.request.UnstationRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class UnstationHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        UnstationRequest request = (UnstationRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        Unit unit = HandlerSupport.requireOwnUnit(game, player, request.getWorkerId(), client);
        if (!(unit instanceof Worker)) {
            client.send(new ErrorResponse(unit == null
                    ? civ.net.protocol.Errors.NOT_YOUR_UNIT
                    : "Only a Worker can be unstationed."));
            return;
        }
        game.unstation((Worker) unit);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
