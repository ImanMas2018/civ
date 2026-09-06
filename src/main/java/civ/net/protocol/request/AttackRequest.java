package civ.net.protocol.request;

import civ.net.protocol.Request;

public class AttackRequest extends Request {

    public static final String TYPE = "attack";

    private final int fromCol;
    private final int fromRow;
    private final int toCol;
    private final int toRow;

    public AttackRequest(int fromCol, int fromRow, int toCol, int toRow) {
        super(TYPE);
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getToCol() {
        return toCol;
    }

    public int getToRow() {
        return toRow;
    }
}
