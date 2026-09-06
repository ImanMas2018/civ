package civ.net.protocol.request;

import civ.net.protocol.Request;

public class AllianceRequest extends Request {

    public static final String TYPE = "alliance";

    private final long targetPlayerId;

    public AllianceRequest(long targetPlayerId) {
        super(TYPE);
        this.targetPlayerId = targetPlayerId;
    }

    public long getTargetPlayerId() {
        return targetPlayerId;
    }
}
