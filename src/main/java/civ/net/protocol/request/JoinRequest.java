package civ.net.protocol.request;

import civ.net.protocol.Request;

public class JoinRequest extends Request {

    public static final String TYPE = "join";

    private final String name;

    public JoinRequest(String name) {
        super(TYPE);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
