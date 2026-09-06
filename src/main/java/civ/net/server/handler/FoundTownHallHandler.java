package civ.net.server.handler;

import civ.model.Builder;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.FoundTownHallRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class FoundTownHallHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        FoundTownHallRequest request = (FoundTownHallRequest) message;
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
                    : "Only a Builder can found a Town Hall."));
            return;
        }
        Hex hex = game.getMap().get(request.getCol(), request.getRow());
        Builder builder = (Builder) unit;
        if (!game.canFoundTownHall(builder, hex)) {
            String shortage = HandlerSupport.missingResource(
                    player.getEmpire().getStock(),
                    Game.FOUND_WOOD, Game.FOUND_STONE, Game.FOUND_IRON);
            if (shortage == null) {
                shortage = HandlerSupport.missingResource(
                        player.getEmpire().getStock(),
                        civ.model.ResourceType.FOOD, Game.FOUND_FOOD);
            }
            client.send(new ErrorResponse(shortage != null
                    ? shortage
                    : "You cannot found a Town Hall here."));
            return;
        }
        game.foundTownHall(builder, hex);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
