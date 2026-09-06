package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.Stockpile;
import civ.model.trade.TradeOffer;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.TradeOfferRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class TradeOfferHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        TradeOfferRequest request = (TradeOfferRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player from = HandlerSupport.requireTurn(session, client, game);
        if (from == null) {
            return;
        }
        Player to = game.getPlayer(request.getTargetPlayerId());
        if (to == null || !to.isAlive() || to.getId() == from.getId()) {
            client.send(new ErrorResponse("That player is not in the game."));
            return;
        }

        TradeOffer offer = new TradeOffer(from.getId(), to.getId());
        put(offer.getOffered(), ResourceType.FOOD, request.getOfferFood());
        put(offer.getOffered(), ResourceType.WOOD, request.getOfferWood());
        put(offer.getOffered(), ResourceType.STONE, request.getOfferStone());
        put(offer.getOffered(), ResourceType.IRON, request.getOfferIron());
        put(offer.getRequested(), ResourceType.FOOD, request.getAskFood());
        put(offer.getRequested(), ResourceType.WOOD, request.getAskWood());
        put(offer.getRequested(), ResourceType.STONE, request.getAskStone());
        put(offer.getRequested(), ResourceType.IRON, request.getAskIron());

        if (!offer.hasAnyResources()) {
            client.send(new ErrorResponse("Offer something, or ask for something."));
            return;
        }

        Stockpile stock = from.getEmpire().getStock();
        for (ResourceType type : ResourceType.values()) {
            int amount = offer.offered(type);
            if (amount > 0 && !stock.canPay(type, amount)) {
                client.send(new ErrorResponse(Errors.notEnough(type.getLabel().toLowerCase())));
                return;
            }
        }

        for (ResourceType type : ResourceType.values()) {
            int amount = offer.offered(type);
            if (amount > 0) {
                stock.lock(type, amount);
            }
        }

        game.getTradeOffers().add(offer);

        ClientHandler targetClient = session.getClients().byPlayerId(to.getId());
        if (targetClient != null) {
            targetClient.send(new NoticePush("New trade offer from " + from.getName() + "."));
        }
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }

    private static void put(java.util.Map<ResourceType, Integer> map, ResourceType type, int amount) {
        map.put(type, Math.max(0, amount));
    }
}
