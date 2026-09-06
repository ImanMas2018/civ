package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.UnitBlueprint;
import civ.net.protocol.Message;
import civ.net.protocol.request.TrainRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class TrainHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        TrainRequest request = (TrainRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        UnitBlueprint blueprint;
        try {
            blueprint = UnitBlueprint.valueOf(request.getUnitType());
        } catch (RuntimeException ex) {
            client.send(new ErrorResponse("Unknown unit type."));
            return;
        }
        if (!game.canTrain(blueprint)) {
            String shortage = HandlerSupport.missingResource(
                    player.getEmpire().getStock(), ResourceType.FOOD, blueprint.getFoodCost());
            if (shortage == null) {
                shortage = HandlerSupport.missingResource(
                        player.getEmpire().getStock(), ResourceType.WOOD, blueprint.getWoodCost());
            }
            client.send(new ErrorResponse(shortage != null
                    ? shortage
                    : "You cannot train that unit right now."));
            return;
        }
        game.train(blueprint);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
