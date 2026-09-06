package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.diplomacy.DiplomaticState;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.DeclareWarRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class DeclareWarHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        DeclareWarRequest request = (DeclareWarRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player declarer = HandlerSupport.requireTurn(session, client, game);
        if (declarer == null) {
            return;
        }
        Player target = game.getPlayer(request.getTargetPlayerId());
        if (target == null || target.getId() == declarer.getId() || !target.isAlive()) {
            client.send(new ErrorResponse("That player does not exist."));
            return;
        }

        // War is unilateral and instant. No acceptance step, by design.
        game.getDiplomacy().set(declarer, target, DiplomaticState.ENEMY);

        session.getClients().broadcast(new NoticePush(
                declarer.getName() + " has declared war on " + target.getName() + "!"));
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
