package civ.net.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MessageCodec {

    /** Compact on purpose: pretty printing would insert newlines and break line framing. */
    private final Gson gson = new Gson();

    public String encode(Message message) {
        return gson.toJson(message);
    }

    public Message decode(String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        if (!object.has("type")) {
            throw new IllegalArgumentException("Message has no type field: " + json);
        }

        String type = object.get("type").getAsString();
        Class<? extends Message> clazz = MessageTypes.classFor(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown message type: " + type);
        }
        return gson.fromJson(object, clazz);
    }
}
