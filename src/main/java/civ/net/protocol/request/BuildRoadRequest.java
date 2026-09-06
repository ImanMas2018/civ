package civ.net.protocol.request;

import civ.net.protocol.Request;

public class BuildRoadRequest extends Request {

    public static final String TYPE = "build_road";

    private final long builderId;

    public BuildRoadRequest(long builderId) {
        super(TYPE);
        this.builderId = builderId;
    }

    public long getBuilderId() {
        return builderId;
    }
}
