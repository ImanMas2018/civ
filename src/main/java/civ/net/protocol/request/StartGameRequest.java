package civ.net.protocol.request;

import civ.net.protocol.Request;

public class StartGameRequest extends Request {

    public static final String TYPE = "start_game";

    public StartGameRequest() {
        super(TYPE);
    }
}
