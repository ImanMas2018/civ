package civ.model.trade;

import java.util.HashSet;
import java.util.Set;

/** One bazaar / trading-post / tribe trade per turn. */
public class TradeTracker {

    private final Set<String> used = new HashSet<>();

    public boolean alreadyTradedThisTurn(TradeRate rate) {
        return used.contains(rate.trackerKey());
    }

    public void markTraded(TradeRate rate) {
        used.add(rate.trackerKey());
    }

    public void clearTurn() {
        used.clear();
    }
}
