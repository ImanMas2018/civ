package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.Tech;
import civ.net.protocol.Message;
import civ.net.protocol.request.ResearchRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class ResearchHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        ResearchRequest request = (ResearchRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        Tech tech;
        try {
            tech = Tech.valueOf(request.getTech());
        } catch (RuntimeException ex) {
            client.send(new ErrorResponse("Unknown technology."));
            return;
        }
        if (!game.canResearch(tech)) {
            String shortage = HandlerSupport.missingResource(
                    player.getEmpire().getStock(),
                    tech.getWoodCost(), tech.getStoneCost(), tech.getIronCost());
            client.send(new ErrorResponse(shortage != null
                    ? shortage
                    : "You cannot research that right now."));
            return;
        }
        game.research(tech);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
