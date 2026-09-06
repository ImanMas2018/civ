package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.Stockpile;
import civ.model.trade.TradeOffer;
import civ.model.trade.TradeOffers;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.TradeReplyRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class TradeReplyHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        TradeReplyRequest reply = (TradeReplyRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player me = HandlerSupport.requireTurn(session, client, game);
        if (me == null) {
            return;
        }

        TradeOffer offer = game.getTradeOffers().byId(reply.getOfferId());
        if (offer == null || offer.getStatus() != TradeOffer.Status.PENDING) {
            client.send(new ErrorResponse("That offer is no longer available."));
            return;
        }
        if (offer.getToPlayerId() != me.getId()) {
            client.send(new ErrorResponse("That offer was not sent to you."));
            return;
        }

        Player sender = game.getPlayer(offer.getFromPlayerId());
        if (sender == null || !sender.isAlive()) {
            TradeOffers.releaseLocks(game, offer);
            offer.setStatus(TradeOffer.Status.CANCELLED);
            client.send(new ErrorResponse("That offer is no longer available."));
            StateFilter.broadcast(session);
            return;
        }

        if (!reply.isAccept()) {
            TradeOffers.releaseLocks(game, offer);
            offer.setStatus(TradeOffer.Status.REJECTED);
            notify(session, sender, me.getName() + " rejected your trade offer.");
            HandlerSupport.ok(client, reply);
            StateFilter.broadcast(session);
            return;
        }

        Stockpile myStock = me.getEmpire().getStock();
        for (ResourceType type : ResourceType.values()) {
            int amount = offer.requested(type);
            if (amount > 0 && !myStock.canPay(type, amount)) {
                client.send(new ErrorResponse(Errors.notEnough(type.getLabel().toLowerCase())));
                return;
            }
        }

        Stockpile senderStock = sender.getEmpire().getStock();
        for (ResourceType type : ResourceType.values()) {
            int amount = offer.offered(type);
            if (amount > 0) {
                senderStock.unlock(type, amount);
                senderStock.add(type, -amount);
                myStock.add(type, amount);
            }
        }
        for (ResourceType type : ResourceType.values()) {
            int amount = offer.requested(type);
            if (amount > 0) {
                myStock.add(type, -amount);
                senderStock.add(type, amount);
            }
        }

        offer.setStatus(TradeOffer.Status.ACCEPTED);
        notify(session, sender, me.getName() + " accepted your trade offer.");
        HandlerSupport.ok(client, reply);
        StateFilter.broadcast(session);
    }

    private static void notify(ServerSession session, Player player, String text) {
        ClientHandler target = session.getClients().byPlayerId(player.getId());
        if (target != null) {
            target.send(new NoticePush(text));
        }
    }
}
