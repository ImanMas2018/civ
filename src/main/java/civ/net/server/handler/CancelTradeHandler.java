package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.trade.TradeOffer;
import civ.model.trade.TradeOffers;
import civ.net.protocol.Message;
import civ.net.protocol.request.CancelTradeRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class CancelTradeHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        CancelTradeRequest request = (CancelTradeRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player me = HandlerSupport.requireTurn(session, client, game);
        if (me == null) {
            return;
        }

        TradeOffer offer = game.getTradeOffers().byId(request.getOfferId());
        if (offer == null || offer.getStatus() != TradeOffer.Status.PENDING) {
            client.send(new ErrorResponse("That offer is no longer available."));
            return;
        }
        if (offer.getFromPlayerId() != me.getId()) {
            client.send(new ErrorResponse("Only the sender can cancel this offer."));
            return;
        }

        TradeOffers.releaseLocks(game, offer);
        offer.setStatus(TradeOffer.Status.CANCELLED);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
