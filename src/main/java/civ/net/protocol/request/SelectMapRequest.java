package civ.net.protocol.request;

import civ.net.protocol.Request;

public class SelectMapRequest extends Request {

    public static final String TYPE = "select_map";

    private final String mapName;

    public SelectMapRequest(String mapName) {
        super(TYPE);
        this.mapName = mapName;
    }

    public String getMapName() {
        return mapName;
    }
}
