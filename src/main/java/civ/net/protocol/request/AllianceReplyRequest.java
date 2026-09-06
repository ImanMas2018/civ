package civ.net.protocol.request;

import civ.net.protocol.Request;

public class AllianceReplyRequest extends Request {

    public static final String TYPE = "alliance_reply";

    private final long proposerPlayerId;
    private final boolean accept;

    public AllianceReplyRequest(long proposerPlayerId, boolean accept) {
        super(TYPE);
        this.proposerPlayerId = proposerPlayerId;
        this.accept = accept;
    }

    public long getProposerPlayerId() {
        return proposerPlayerId;
    }

    public boolean isAccept() {
        return accept;
    }
}
