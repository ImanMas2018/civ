package civ.net.protocol.request;

import civ.net.protocol.Request;

public class DeclareWarRequest extends Request {

    public static final String TYPE = "declare_war";

    private final long targetPlayerId;

    public DeclareWarRequest(long targetPlayerId) {
        super(TYPE);
        this.targetPlayerId = targetPlayerId;
    }

    public long getTargetPlayerId() {
        return targetPlayerId;
    }
}
