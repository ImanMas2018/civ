package civ.net.protocol.request;

import civ.net.protocol.Request;

public class UseItemRequest extends Request {

    public static final String TYPE = "use_item";

    private final String itemType;
    private final long unitId;
    private final Integer targetCol;
    private final Integer targetRow;

    public UseItemRequest(String itemType, long unitId, Integer targetCol, Integer targetRow) {
        super(TYPE);
        this.itemType = itemType;
        this.unitId = unitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    public String getItemType() {
        return itemType;
    }

    public long getUnitId() {
        return unitId;
    }

    public Integer getTargetCol() {
        return targetCol;
    }

    public Integer getTargetRow() {
        return targetRow;
    }
}
