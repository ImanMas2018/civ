package civ.net.protocol.request;

import civ.net.protocol.Request;

public class ChatRequest extends Request {

    public static final String TYPE = "chat";

    private final String text;

    public ChatRequest(String text) {
        super(TYPE);
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
