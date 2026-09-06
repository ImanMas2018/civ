package civ.model.item;

import java.util.EnumMap;
import java.util.Map;

/** What a player owns. Items are never carried by units. */
public class Inventory {

    private final Map<ItemType, Integer> counts = new EnumMap<>(ItemType.class);

    public Inventory() {
        for (ItemType type : ItemType.values()) {
            counts.put(type, 0);
        }
    }

    public int count(ItemType type) {
        return counts.getOrDefault(type, 0);
    }

    public boolean has(ItemType type) {
        return count(type) > 0;
    }

    public void add(ItemType type) {
        counts.put(type, count(type) + 1);
    }

    public void add(ItemType type, int amount) {
        if (amount <= 0) {
            return;
        }
        counts.put(type, count(type) + amount);
    }

    public void remove(ItemType type) {
        int now = count(type);
        if (now <= 0) {
            return;
        }
        counts.put(type, now - 1);
    }

    public void set(ItemType type, int amount) {
        counts.put(type, Math.max(0, amount));
    }

    public Map<ItemType, Integer> snapshot() {
        return new EnumMap<>(counts);
    }
}
