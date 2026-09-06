package civ.net.protocol.request;

import civ.net.protocol.Request;

public class CheatRequest extends Request {

    public static final String TYPE = "cheat";

    private final String command;

    public CheatRequest(String command) {
        super(TYPE);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
