package civ.model.trade;

public class TradingPostTrade implements TradeRate {

    @Override
    public String getLabel() {
        return "Trading Post";
    }

    @Override
    public int getFixedAmount() {
        return 0;
    }

    @Override
    public int getPercent() {
        return 80;
    }

    @Override
    public String trackerKey() {
        return "trading-post";
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
