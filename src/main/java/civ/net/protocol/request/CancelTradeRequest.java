package civ.net.protocol.request;

import civ.net.protocol.Request;

public class CancelTradeRequest extends Request {

    public static final String TYPE = "cancel_trade";

    private final long offerId;

    public CancelTradeRequest(long offerId) {
        super(TYPE);
        this.offerId = offerId;
    }

    public long getOfferId() {
        return offerId;
    }
}
