package civ.net.protocol.push;

import civ.net.protocol.Broadcast;

/** Private alliance proposal — only the target receives this. */
public class AlliancePrompt extends Broadcast {

    public static final String TYPE = "alliance_prompt";

    private final long fromPlayerId;
    private final String fromPlayerName;

    public AlliancePrompt(long fromPlayerId, String fromPlayerName) {
        super(TYPE);
        this.fromPlayerId = fromPlayerId;
        this.fromPlayerName = fromPlayerName;
    }

    public long getFromPlayerId() {
        return fromPlayerId;
    }

    public String getFromPlayerName() {
        return fromPlayerName;
    }
}
