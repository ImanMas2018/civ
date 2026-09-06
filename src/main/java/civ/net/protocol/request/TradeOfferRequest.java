package civ.net.protocol.request;

import civ.net.protocol.Request;

public class TradeOfferRequest extends Request {

    public static final String TYPE = "trade_offer";

    private final long targetPlayerId;
    private final int offerFood;
    private final int offerWood;
    private final int offerStone;
    private final int offerIron;
    private final int askFood;
    private final int askWood;
    private final int askStone;
    private final int askIron;

    public TradeOfferRequest(long targetPlayerId,
                             int offerFood, int offerWood, int offerStone, int offerIron,
                             int askFood, int askWood, int askStone, int askIron) {
        super(TYPE);
        this.targetPlayerId = targetPlayerId;
        this.offerFood = offerFood;
        this.offerWood = offerWood;
        this.offerStone = offerStone;
        this.offerIron = offerIron;
        this.askFood = askFood;
        this.askWood = askWood;
        this.askStone = askStone;
        this.askIron = askIron;
    }

    public long getTargetPlayerId() {
        return targetPlayerId;
    }

    public int getOfferFood() {
        return offerFood;
    }

    public int getOfferWood() {
        return offerWood;
    }

    public int getOfferStone() {
        return offerStone;
    }

    public int getOfferIron() {
        return offerIron;
    }

    public int getAskFood() {
        return askFood;
    }

    public int getAskWood() {
        return askWood;
    }

    public int getAskStone() {
        return askStone;
    }

    public int getAskIron() {
        return askIron;
    }
}
