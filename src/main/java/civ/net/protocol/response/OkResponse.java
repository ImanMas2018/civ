package civ.net.protocol.response;

import civ.net.protocol.Response;

public class OkResponse extends Response {

    public static final String TYPE = "ok";

    public OkResponse() {
        super(TYPE);
    }
}
