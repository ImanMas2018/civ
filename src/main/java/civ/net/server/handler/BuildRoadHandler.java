package civ.net.server.handler;

import civ.model.Builder;
import civ.model.Game;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.BuildRoadRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class BuildRoadHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        BuildRoadRequest request = (BuildRoadRequest) message;
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
                    : "Only a Builder can build a road."));
            return;
        }
        Builder builder = (Builder) unit;
        if (!game.canBuildRoad(builder)) {
            String shortage = HandlerSupport.missingResource(
                    player.getEmpire().getStock(), 8, 0, 0);
            client.send(new ErrorResponse(shortage != null
                    ? shortage
                    : "You cannot build a road here."));
            return;
        }
        game.buildRoad(builder);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
