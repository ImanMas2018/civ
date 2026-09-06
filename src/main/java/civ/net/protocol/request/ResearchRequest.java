package civ.net.protocol.request;

import civ.net.protocol.Request;

public class ResearchRequest extends Request {

    public static final String TYPE = "research";

    private final String tech;

    public ResearchRequest(String tech) {
        super(TYPE);
        this.tech = tech;
    }

    public String getTech() {
        return tech;
    }
}
