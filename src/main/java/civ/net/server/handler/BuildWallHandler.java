package civ.net.server.handler;

import civ.model.Builder;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.request.BuildWallRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class BuildWallHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        BuildWallRequest request = (BuildWallRequest) message;
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
                    : "Only a Builder can build a wall."));
            return;
        }
        Hex other = game.getMap().get(request.getCol(), request.getRow());
        Builder builder = (Builder) unit;
        if (!game.canBuildWall(builder, other)) {
            String shortage = HandlerSupport.missingResource(
                    player.getEmpire().getStock(), 15, 20, 0);
            client.send(new ErrorResponse(shortage != null
                    ? shortage
                    : "You cannot build a wall there."));
            return;
        }
        game.buildWall(builder, other);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
