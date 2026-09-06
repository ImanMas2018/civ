package civ.net.server.handler;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.MoveUnitRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class MoveUnitHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        MoveUnitRequest request = (MoveUnitRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        Unit unit = HandlerSupport.requireOwnUnit(game, player, request.getUnitId(), client);
        if (unit == null) {
            return;
        }
        Hex target = game.getMap().get(request.getCol(), request.getRow());
        if (target == null || !game.canMove(unit, target)) {
            client.send(new ErrorResponse("This unit cannot reach that hex."));
            return;
        }
        game.moveUnit(unit, target);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
