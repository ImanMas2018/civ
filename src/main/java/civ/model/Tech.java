package civ.model;

/**
 * Town Hall upgrades. Costs and prerequisites live here so balancing is one table.
 * Researching them is Step 5; Step 4 only uses them to lock mine/settlement buttons.
 */
public enum Tech {
    STORAGE_1("Storage Upgrade 1", 2, 30, 20, 0, null),
    STORAGE_2("Storage Upgrade 2", 3, 50, 50, 0, STORAGE_1),
    STONE_MINING("Stone Mining", 2, 30, 0, 0, null),
    IRON_MINING("Iron Mining", 3, 30, 30, 0, STONE_MINING),
    PRO_TOOLS("Professional Tools", 4, 0, 40, 20, IRON_MINING),
    TOWN_BUILDING("Settlement Building", 3, 40, 40, 0, STONE_MINING);

    private final String label;
    private final int turns;
    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final Tech required;

    Tech(String label, int turns, int woodCost, int stoneCost, int ironCost, Tech required) {
        this.label = label;
        this.turns = turns;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
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

    public Tech getRequired() {
        return required;
    }
}
