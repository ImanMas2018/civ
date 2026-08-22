package civ.model.trade;

import civ.model.Game;
import civ.model.ResourceType;

/** The only place that performs a resource exchange. */
public class TradeService {

    public boolean canTrade(Game game, TradeRate rate, ResourceType sell, int amount) {
        if (amount <= 0 || sell == null) {
            return false;
        }
        if (rate.getFixedAmount() > 0 && amount != rate.getFixedAmount()) {
            return false;
        }
        if (game.getTradeTracker().alreadyTradedThisTurn(rate)) {
            return false;
        }
        return game.getEmpire().getStock().canPay(sell, amount);
    }

    public void trade(Game game, TradeRate rate, ResourceType sell, ResourceType receive, int amount) {
        if (sell == receive || receive == null) {
            return;
        }
        if (!canTrade(game, rate, sell, amount)) {
            return;
        }
        int gained = rate.convert(amount);
        if (gained <= 0) {
            return;
        }
        game.getEmpire().getStock().add(sell, -amount);
        game.getEmpire().getStock().add(receive, gained);
        game.getTradeTracker().markTraded(rate);
        game.addLog(rate.getLabel() + ": sold " + amount + " " + sell.getLabel()
                + " for " + gained + " " + receive.getLabel() + ".");
    }
}
