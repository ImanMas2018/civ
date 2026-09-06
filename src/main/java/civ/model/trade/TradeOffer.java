package civ.model.trade;

import civ.model.Entity;
import civ.model.ResourceType;
import java.util.EnumMap;
import java.util.Map;

public class TradeOffer extends Entity {

    public enum Status {
        PENDING, ACCEPTED, REJECTED, CANCELLED
    }

    private final long fromPlayerId;
    private final long toPlayerId;
    private final Map<ResourceType, Integer> offered = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> requested = new EnumMap<>(ResourceType.class);
    private Status status = Status.PENDING;

    public TradeOffer(long fromPlayerId, long toPlayerId) {
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
        for (ResourceType type : ResourceType.values()) {
            offered.put(type, 0);
            requested.put(type, 0);
        }
    }

    /** Rebuild from a network snapshot, preserving the server id. */
    public TradeOffer(long id, long createdAt, long fromPlayerId, long toPlayerId) {
        super(id, createdAt);
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
        for (ResourceType type : ResourceType.values()) {
            offered.put(type, 0);
            requested.put(type, 0);
        }
    }

    public long getFromPlayerId() {
        return fromPlayerId;
    }

    public long getToPlayerId() {
        return toPlayerId;
    }

    public Map<ResourceType, Integer> getOffered() {
        return offered;
    }

    public Map<ResourceType, Integer> getRequested() {
        return requested;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int offered(ResourceType type) {
        return offered.getOrDefault(type, 0);
    }

    public int requested(ResourceType type) {
        return requested.getOrDefault(type, 0);
    }

    public boolean hasAnyResources() {
        for (ResourceType type : ResourceType.values()) {
            if (offered(type) > 0 || requested(type) > 0) {
                return true;
            }
        }
        return false;
    }
}
