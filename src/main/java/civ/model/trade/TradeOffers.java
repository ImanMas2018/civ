package civ.model.trade;

import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.Stockpile;
import java.util.ArrayList;
import java.util.List;

/** All player-to-player trade offers for one match. */
public class TradeOffers {

    private final List<TradeOffer> offers = new ArrayList<>();

    public void add(TradeOffer offer) {
        offers.add(offer);
    }

    public void clear() {
        offers.clear();
    }

    public TradeOffer byId(long id) {
        for (TradeOffer offer : offers) {
            if (offer.getId() == id) {
                return offer;
            }
        }
        return null;
    }

    public List<TradeOffer> pendingFor(Player player) {
        List<TradeOffer> result = new ArrayList<>();
        if (player == null) {
            return result;
        }
        for (TradeOffer offer : offers) {
            if (offer.getStatus() == TradeOffer.Status.PENDING
                    && offer.getToPlayerId() == player.getId()) {
                result.add(offer);
            }
        }
        return result;
    }

    public List<TradeOffer> pendingFrom(Player player) {
        List<TradeOffer> result = new ArrayList<>();
        if (player == null) {
            return result;
        }
        for (TradeOffer offer : offers) {
            if (offer.getStatus() == TradeOffer.Status.PENDING
                    && offer.getFromPlayerId() == player.getId()) {
                result.add(offer);
            }
        }
        return result;
    }

    /** Unlock and cancel every pending offer this player created (e.g. on disconnect). */
    public void cancelAllFrom(Game game, Player player) {
        if (game == null || player == null) {
            return;
        }
        for (TradeOffer offer : offers) {
            if (offer.getStatus() != TradeOffer.Status.PENDING
                    || offer.getFromPlayerId() != player.getId()) {
                continue;
            }
            releaseLocks(game, offer);
            offer.setStatus(TradeOffer.Status.CANCELLED);
        }
    }

    public static void releaseLocks(Game game, TradeOffer offer) {
        Player sender = game.getPlayer(offer.getFromPlayerId());
        if (sender == null) {
            return;
        }
        Stockpile stock = sender.getEmpire().getStock();
        for (MapEntry entry : entries(offer.getOffered())) {
            stock.unlock(entry.type, entry.amount);
        }
    }

    private static List<MapEntry> entries(java.util.Map<ResourceType, Integer> map) {
        List<MapEntry> list = new ArrayList<>();
        for (java.util.Map.Entry<ResourceType, Integer> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                list.add(new MapEntry(entry.getKey(), entry.getValue()));
            }
        }
        return list;
    }

    private static final class MapEntry {
        final ResourceType type;
        final int amount;

        MapEntry(ResourceType type, int amount) {
            this.type = type;
            this.amount = amount;
        }
    }
}
