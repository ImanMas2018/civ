package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.diplomacy.DiplomaticState;
import civ.net.protocol.Message;
import civ.net.protocol.push.AlliancePrompt;
import civ.net.protocol.request.AllianceRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;

public class AllianceHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        AllianceRequest request = (AllianceRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player from = HandlerSupport.requireTurn(session, client, game);
        if (from == null) {
            return;
        }
        Player to = game.getPlayer(request.getTargetPlayerId());
        if (to == null || to.getId() == from.getId() || !to.isAlive()) {
            client.send(new ErrorResponse("That player does not exist."));
            return;
        }
        DiplomaticState state = game.getDiplomacy().between(from, to);
        if (state == DiplomaticState.ALLIED) {
            client.send(new ErrorResponse("You are already allied with this player."));
            return;
        }

        game.getDiplomacy().proposeAlliance(from.getId(), to.getId());

        ClientHandler targetClient = session.getClients().byPlayerId(to.getId());
        if (targetClient != null) {
            targetClient.send(new AlliancePrompt(from.getId(), from.getName()));
        }
        HandlerSupport.ok(client, request);
    }
}
