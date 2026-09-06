package civ.net.protocol.request;

import civ.net.protocol.Request;

public class StationRequest extends Request {

    public static final String TYPE = "station";

    private final long workerId;

    public StationRequest(long workerId) {
        super(TYPE);
        this.workerId = workerId;
    }

    public long getWorkerId() {
        return workerId;
    }
}
