package civ.net.server.handler;

import civ.model.BorderExpander;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.ExpandBorderRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class ExpandBorderHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        ExpandBorderRequest request = (ExpandBorderRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        Unit unit = HandlerSupport.requireOwnUnit(game, player, request.getUnitId(), client);
        if (!(unit instanceof BorderExpander)) {
            client.send(new ErrorResponse(unit == null
                    ? civ.net.protocol.Errors.NOT_YOUR_UNIT
                    : "Only a Border Expander can claim territory."));
            return;
        }
        Hex hex = game.getMap().get(request.getCol(), request.getRow());
        if (!game.canExpandBorder((BorderExpander) unit, hex)) {
            client.send(new ErrorResponse("You cannot claim that hex."));
            return;
        }
        game.expandBorder((BorderExpander) unit, hex);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
