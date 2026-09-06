package civ.net.protocol.request;

import civ.net.protocol.Request;

public class EndTurnRequest extends Request {

    public static final String TYPE = "end_turn";

    public EndTurnRequest() {
        super(TYPE);
    }
}
