package civ.net.protocol.request;

import civ.net.protocol.Request;

public class TradeReplyRequest extends Request {

    public static final String TYPE = "trade_reply";

    private final long offerId;
    private final boolean accept;

    public TradeReplyRequest(long offerId, boolean accept) {
        super(TYPE);
        this.offerId = offerId;
        this.accept = accept;
    }

    public long getOfferId() {
        return offerId;
    }

    public boolean isAccept() {
        return accept;
    }
}
