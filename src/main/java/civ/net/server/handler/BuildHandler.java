package civ.net.server.handler;

import civ.model.Builder;
import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.BuildRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class BuildHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        BuildRequest request = (BuildRequest) message;
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
                    : "Only a Builder can construct buildings."));
            return;
        }
        BuildingType type;
        try {
            type = BuildingType.valueOf(request.getBuildingType());
        } catch (RuntimeException ex) {
            client.send(new ErrorResponse("Unknown building type."));
            return;
        }
        Hex hex = game.getMap().get(request.getCol(), request.getRow());
        Builder builder = (Builder) unit;
        int woodCost = type.getWoodCost();
        if (type == BuildingType.DOCK && game.isNextDockHalfPrice()) {
            woodCost = woodCost / 2;
        }
        if (!game.canBuild(builder, type, hex)) {
            String shortage = HandlerSupport.missingForBuilding(
                    player.getEmpire().getStock(), type, woodCost);
            if (shortage != null) {
                client.send(new ErrorResponse(shortage));
            } else {
                client.send(new ErrorResponse("You cannot build that here."));
            }
            return;
        }
        game.build(builder, type, hex);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
