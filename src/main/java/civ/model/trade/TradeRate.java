package civ.model.trade;

/** Strategy: how much you get back when selling a resource. */
public interface TradeRate {

    String getLabel();

    /** Fixed amount to sell, or 0 when the player types any amount. */
    int getFixedAmount();

    /** Percentage returned, e.g. 50 means you get half. */
    int getPercent();

    /** Fractions always round down (integer division). */
    default int convert(int sold) {
        return sold * getPercent() / 100;
    }

    /** Stable key for the once-per-turn tracker. */
    String trackerKey();
}
