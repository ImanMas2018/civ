package civ.net.protocol.request;

import civ.net.protocol.Request;

public class UnstationRequest extends Request {

    public static final String TYPE = "unstation";

    private final long workerId;

    public UnstationRequest(long workerId) {
        super(TYPE);
        this.workerId = workerId;
    }

    public long getWorkerId() {
        return workerId;
    }
}
