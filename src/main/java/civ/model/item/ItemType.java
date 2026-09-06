package civ.model.item;

import civ.model.ResourceType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Data table for craftable consumables. Costs are free to choose; effects are fixed by the spec. */
public enum ItemType {

    TELEPORT("Teleport Scroll", cost(20, 30, 10, 20)),
    MOBILITY("Swift Draught", cost(15, 10, 0, 0)),
    COMBAT("Battle Tonic", cost(10, 0, 0, 15));

    private final String label;
    private final Map<ResourceType, Integer> cost;

    ItemType(String label, Map<ResourceType, Integer> cost) {
        this.label = label;
        this.cost = cost;
    }

    public String getLabel() {
        return label;
    }

    public Map<ResourceType, Integer> getCost() {
        return cost;
    }

    public int costOf(ResourceType type) {
        return cost.getOrDefault(type, 0);
    }

    public String describeCost() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(entry.getValue()).append(shortLabel(entry.getKey()));
        }
        return sb.toString();
    }

    private static String shortLabel(ResourceType type) {
        switch (type) {
            case FOOD:
                return "f";
            case WOOD:
                return "w";
            case STONE:
                return "s";
            case IRON:
                return "i";
            default:
                return "";
        }
    }

    private static Map<ResourceType, Integer> cost(int food, int wood, int stone, int iron) {
        Map<ResourceType, Integer> map = new EnumMap<>(ResourceType.class);
        map.put(ResourceType.FOOD, food);
        map.put(ResourceType.WOOD, wood);
        map.put(ResourceType.STONE, stone);
        map.put(ResourceType.IRON, iron);
        return Collections.unmodifiableMap(map);
    }
}
