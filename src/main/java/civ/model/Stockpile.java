package civ.model;

import java.util.EnumMap;
import java.util.Map;

/** The warehouse: four resource piles and one shared capacity. */
public class Stockpile {

    private final Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
    private int capacity;

    public Stockpile(int capacity) {
        this.capacity = capacity;
        for (ResourceType type : ResourceType.values()) {
            amounts.put(type, 0);
        }
    }

    public int get(ResourceType type) {
        return amounts.get(type);
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
        amounts.put(type, now);
    }

    public boolean canPay(ResourceType type, int amount) {
        return amounts.get(type) >= amount;
    }

    public boolean canPay(int wood, int stone, int iron) {
        return canPay(ResourceType.WOOD, wood)
                && canPay(ResourceType.STONE, stone)
                && canPay(ResourceType.IRON, iron);
    }

    public void pay(int wood, int stone, int iron) {
        add(ResourceType.WOOD, -wood);
        add(ResourceType.STONE, -stone);
        add(ResourceType.IRON, -iron);
    }
}
