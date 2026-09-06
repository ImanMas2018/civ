package civ.net.protocol.response;

import civ.net.protocol.Response;

public class ErrorResponse extends Response {

    public static final String TYPE = "error";

    private final String reason;

    public ErrorResponse(String reason) {
        super(TYPE);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
