package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.diplomacy.DiplomaticState;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.BreakAllianceRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class BreakAllianceHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        BreakAllianceRequest request = (BreakAllianceRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player me = HandlerSupport.requireTurn(session, client, game);
        if (me == null) {
            return;
        }
        Player other = game.getPlayer(request.getTargetPlayerId());
        if (other == null || other.getId() == me.getId()) {
            client.send(new ErrorResponse("That player does not exist."));
            return;
        }
        if (game.getDiplomacy().between(me, other) != DiplomaticState.ALLIED) {
            client.send(new ErrorResponse("You are not allied with this player."));
            return;
        }

        game.getDiplomacy().set(me, other, DiplomaticState.NEUTRAL);
        session.getClients().broadcast(new NoticePush(
                me.getName() + " has broken the alliance with " + other.getName() + "."));
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
