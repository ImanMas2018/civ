package civ.model;

/**
 * Town Hall upgrades. Costs and prerequisites live here so balancing is one table.
 */
public enum Tech {
    STORAGE_1("Storage Upgrade 1", 2, 30, 20, 0, 1, null),
    STORAGE_2("Storage Upgrade 2", 3, 50, 50, 0, 1, STORAGE_1),
    STONE_MINING("Stone Mining", 2, 30, 0, 0, 1, null),
    IRON_MINING("Iron Mining", 3, 30, 30, 0, 1, STONE_MINING),
    PRO_TOOLS("Professional Tools", 4, 0, 40, 20, 1, IRON_MINING),
    TOWN_BUILDING("Settlement Building", 3, 40, 40, 0, 1, STONE_MINING),
    SEAFARING("Seafaring", 4, 80, 0, 0, 2, null),
    STEEL_TOOLS("Steel Tools", 3, 0, 0, 40, 2, null),
    DEFENSIVE_ARCHITECTURE("Defensive Architecture", 4, 0, 100, 0, 3, null);

    private final String label;
    private final int turns;
    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final int requiredLevel;
    private final Tech required;

    Tech(String label, int turns, int woodCost, int stoneCost, int ironCost,
         int requiredLevel, Tech required) {
        this.label = label;
        this.turns = turns;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
        this.requiredLevel = requiredLevel;
        this.required = required;
    }

    public String getLabel() {
        return label;
    }

    public int getTurns() {
        return turns;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    public int getIronCost() {
        return ironCost;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public Tech getRequired() {
        return required;
    }
}
