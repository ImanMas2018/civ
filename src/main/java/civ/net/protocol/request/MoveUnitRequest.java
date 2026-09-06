package civ.net.protocol.request;

import civ.net.protocol.Request;

public class MoveUnitRequest extends Request {

    public static final String TYPE = "move_unit";

    private final long unitId;
    private final int col;
    private final int row;

    public MoveUnitRequest(long unitId, int col, int row) {
        super(TYPE);
        this.unitId = unitId;
        this.col = col;
        this.row = row;
    }

    public long getUnitId() {
        return unitId;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
}
