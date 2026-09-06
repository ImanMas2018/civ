package civ.net.protocol.request;

import civ.net.protocol.Request;

public class ReadyRequest extends Request {

    public static final String TYPE = "ready";

    private final boolean ready;

    public ReadyRequest(boolean ready) {
        super(TYPE);
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
