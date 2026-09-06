package civ.net.server.handler;

import civ.model.Builder;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.DemolishRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class DemolishHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        DemolishRequest request = (DemolishRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        Unit unit = HandlerSupport.requireOwnUnit(game, player, request.getBuilderId(), client);
        if (!(unit instanceof Builder)) {
            client.send(new ErrorResponse(unit == null
                    ? civ.net.protocol.Errors.NOT_YOUR_UNIT
                    : "Only a Builder can demolish a building."));
            return;
        }
        Hex hex = game.getMap().get(request.getCol(), request.getRow());
        if (!game.canDemolish((Builder) unit, hex)) {
            client.send(new ErrorResponse("You cannot demolish that."));
            return;
        }
        game.demolish((Builder) unit, hex);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
