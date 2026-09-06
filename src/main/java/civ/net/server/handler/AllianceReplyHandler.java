package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.diplomacy.DiplomaticState;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.AllianceReplyRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class AllianceReplyHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        AllianceReplyRequest reply = (AllianceReplyRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player me = session.playerOf(client);
        if (me == null || !me.isAlive()) {
            client.send(new ErrorResponse("You are not in this game."));
            return;
        }
        Player proposer = game.getPlayer(reply.getProposerPlayerId());
        if (proposer == null || !proposer.isAlive()) {
            client.send(new ErrorResponse("That player does not exist."));
            return;
        }
        if (!game.getDiplomacy().hasProposal(proposer.getId(), me.getId())) {
            client.send(new ErrorResponse("There is no pending alliance proposal from that player."));
            return;
        }

        game.getDiplomacy().clearProposal(proposer.getId(), me.getId());

        if (reply.isAccept()) {
            game.getDiplomacy().set(me, proposer, DiplomaticState.ALLIED);
            session.getClients().broadcast(new NoticePush(
                    me.getName() + " and " + proposer.getName() + " are now allied."));
            HandlerSupport.ok(client, reply);
            StateFilter.broadcast(session);
        } else {
            ClientHandler proposerClient = session.getClients().byPlayerId(proposer.getId());
            if (proposerClient != null) {
                proposerClient.send(new NoticePush(me.getName() + " declined your alliance."));
            }
            HandlerSupport.ok(client, reply);
        }
    }
}
