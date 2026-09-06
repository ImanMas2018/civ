package civ.net.protocol.request;

import civ.net.protocol.Request;

public class BreakAllianceRequest extends Request {

    public static final String TYPE = "break_alliance";

    private final long targetPlayerId;

    public BreakAllianceRequest(long targetPlayerId) {
        super(TYPE);
        this.targetPlayerId = targetPlayerId;
    }

    public long getTargetPlayerId() {
        return targetPlayerId;
    }
}
