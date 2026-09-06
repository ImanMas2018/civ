package civ.net.protocol.request;

import civ.net.protocol.Request;

public class SetCheatsRequest extends Request {

    public static final String TYPE = "set_cheats";

    private final boolean enabled;

    public SetCheatsRequest(boolean enabled) {
        super(TYPE);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
