package civ.net.protocol.request;

import civ.net.protocol.Request;

public class CraftItemRequest extends Request {

    public static final String TYPE = "craft_item";

    private final long apothecaryId;
    private final String itemType;

    public CraftItemRequest(long apothecaryId, String itemType) {
        super(TYPE);
        this.apothecaryId = apothecaryId;
        this.itemType = itemType;
    }

    public long getApothecaryId() {
        return apothecaryId;
    }

    public String getItemType() {
        return itemType;
    }
}
