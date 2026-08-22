package civ.model.trade;

import civ.model.tribe.Tribe;
import civ.model.tribe.TribeType;

public class TribeTrade implements TradeRate {

    private final Tribe tribe;

    public TribeTrade(Tribe tribe) {
        this.tribe = tribe;
    }

    public Tribe getTribe() {
        return tribe;
    }

    @Override
    public String getLabel() {
        return "Trade with " + tribe.getName();
    }

    @Override
    public int getFixedAmount() {
        return 0;
    }

    @Override
    public int getPercent() {
        int base = tribe.getType() == TribeType.TRADER ? 80 : 75;
        return base + tribe.getTradeBonusPercent();
    }

    @Override
    public String trackerKey() {
        return "tribe-" + tribe.getId();
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
