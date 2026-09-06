package civ.net.protocol.request;

import civ.net.protocol.Request;

public class CancelTownHallOrderRequest extends Request {

    public static final String TYPE = "cancel_town_hall_order";

    public CancelTownHallOrderRequest() {
        super(TYPE);
    }
}
