package civ.net.protocol.request;

import civ.net.protocol.Request;

public class TrainRequest extends Request {

    public static final String TYPE = "train";

    private final String unitType;

    public TrainRequest(String unitType) {
        super(TYPE);
        this.unitType = unitType;
    }

    public String getUnitType() {
        return unitType;
    }
}
