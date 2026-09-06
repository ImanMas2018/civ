package civ.net.protocol.push;

import civ.net.protocol.Broadcast;

public class NoticePush extends Broadcast {

    public static final String TYPE = "notice";

    private final String text;

    public NoticePush(String text) {
        super(TYPE);
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
