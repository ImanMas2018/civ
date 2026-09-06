package civ.net.protocol.request;

import civ.net.protocol.Request;

public class DemolishRequest extends Request {

    public static final String TYPE = "demolish";

    private final long builderId;
    private final int col;
    private final int row;

    public DemolishRequest(long builderId, int col, int row) {
        super(TYPE);
        this.builderId = builderId;
        this.col = col;
        this.row = row;
    }

    public long getBuilderId() { return builderId; }
    public int getCol() { return col; }
    public int getRow() { return row; }
}
