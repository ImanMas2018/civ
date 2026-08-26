package civ.model.trade;

public class BazaarTrade implements TradeRate {

    private final int tier;

    public BazaarTrade(int tier) {
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public String getLabel() {
        return "Bazaar tier " + tier;
    }

    @Override
    public int getFixedAmount() {
        if (tier == 1) {
            return 10;
        }
        if (tier == 2) {
            return 100;
        }
        return 500;
    }

    @Override
    public int getPercent() {
        if (tier == 1) {
            return 50;
        }
        if (tier == 2) {
            return 60;
        }
        return 70;
    }

    @Override
    public String trackerKey() {
        return "bazaar";
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
