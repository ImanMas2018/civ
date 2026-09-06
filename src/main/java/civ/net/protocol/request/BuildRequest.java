package civ.net.protocol.request;

import civ.net.protocol.Request;

public class BuildRequest extends Request {

    public static final String TYPE = "build";

    private final long builderId;
    private final String buildingType;
    private final int col;
    private final int row;

    public BuildRequest(long builderId, String buildingType, int col, int row) {
        super(TYPE);
        this.builderId = builderId;
        this.buildingType = buildingType;
        this.col = col;
        this.row = row;
    }

    public long getBuilderId() {
        return builderId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
}
