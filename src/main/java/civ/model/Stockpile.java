package civ.model;

import java.util.EnumMap;
import java.util.Map;

public class Stockpile {

    private final Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> locked = new EnumMap<>(ResourceType.class);
    private int capacity;

    public Stockpile(int capacity) {
        this.capacity = capacity;
        for (ResourceType type : ResourceType.values()) {
            amounts.put(type, 0);
            locked.put(type, 0);
        }
    }

    public int get(ResourceType type) {
        return amounts.get(type);
    }

    public int getLocked(ResourceType type) {
        return locked.getOrDefault(type, 0);
    }

    /** What you may actually spend right now. */
    public int available(ResourceType type) {
        return get(type) - getLocked(type);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        for (ResourceType type : ResourceType.values()) {
            if (amounts.get(type) > capacity) {
                amounts.put(type, capacity);
            }
        }
    }

    /** Adds (or, with a negative amount, removes). Never goes above the capacity. */
    public void add(ResourceType type, int amount) {
        int now = amounts.get(type) + amount;
        if (now > capacity) {
            now = capacity;
        }
        if (now < 0) {
            now = 0;
        }
        amounts.put(type, now);
        if (getLocked(type) > now) {
            locked.put(type, now);
        }
    }

    public void set(ResourceType type, int amount) {
        amounts.put(type, amount);
        if (getLocked(type) > amount) {
            locked.put(type, amount);
        }
    }

    public void setLocked(ResourceType type, int amount) {
        locked.put(type, Math.max(0, amount));
    }

    /** Every cost check in the game goes through this — locking applies automatically. */
    public boolean canPay(ResourceType type, int amount) {
        return available(type) >= amount;
    }

    public boolean canPay(int wood, int stone, int iron) {
        return canPay(ResourceType.WOOD, wood)
                && canPay(ResourceType.STONE, stone)
                && canPay(ResourceType.IRON, iron);
    }

    public void lock(ResourceType type, int amount) {
        if (amount <= 0) {
            return;
        }
        locked.merge(type, amount, Integer::sum);
    }

    public void unlock(ResourceType type, int amount) {
        if (amount <= 0) {
            return;
        }
        int next = getLocked(type) - amount;
        locked.put(type, Math.max(0, next));
    }

    public void pay(int wood, int stone, int iron) {
        add(ResourceType.WOOD, -wood);
        add(ResourceType.STONE, -stone);
        add(ResourceType.IRON, -iron);
    }
}
