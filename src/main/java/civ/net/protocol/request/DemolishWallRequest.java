package civ.net.protocol.request;

import civ.net.protocol.Request;

public class DemolishWallRequest extends Request {

    public static final String TYPE = "demolish_wall";

    private final long builderId;
    private final int col;
    private final int row;

    public DemolishWallRequest(long builderId, int col, int row) {
        super(TYPE);
        this.builderId = builderId;
        this.col = col;
        this.row = row;
    }

    public long getBuilderId() { return builderId; }
    public int getCol() { return col; }
    public int getRow() { return row; }
}
