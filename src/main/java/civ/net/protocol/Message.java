package civ.net.protocol;

public abstract class Message {

    /** Discriminator written into the JSON, e.g. "move_unit". */
    private final String type;

    /** Set by the client so a reply can be matched to a request. Null for pushes. */
    private String requestId;

    private final long createdAt = System.currentTimeMillis();

    protected Message(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Message withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
