package civ.net.protocol.push;

import civ.net.protocol.Broadcast;

public class ChatBroadcast extends Broadcast {

    public static final String TYPE = "chat_broadcast";

    private final String sender;
    private final String text;
    private final String time;

    public ChatBroadcast(String sender, String text, String time) {
        super(TYPE);
        this.sender = sender;
        this.text = text;
        this.time = time;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }
}
