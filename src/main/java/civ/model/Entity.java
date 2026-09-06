package civ.model;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Entity {

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    private final long id;
    private final long createdAt;

    protected Entity() {
        this.id = NEXT_ID.getAndIncrement();
        this.createdAt = System.currentTimeMillis();
    }

    /** Used when rebuilding from a save file, so ids survive a reload. */
    protected Entity(long id, long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        NEXT_ID.updateAndGet(current -> Math.max(current, id + 1));
    }

    public long getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
