package civ.net.protocol.request;

import civ.net.protocol.Request;

public class UpgradeTownHallRequest extends Request {

    public static final String TYPE = "upgrade_town_hall";

    public UpgradeTownHallRequest() {
        super(TYPE);
    }
}
