package civ.model;

/**
 * Town Hall ranks. Costs and storage are the values for <em>reaching</em> that rank.
 */
public enum TownHallLevel {
    BASE_CAMP(1, "Base Camp", 100, 0, 0, 0, 0, 0),
    SETTLEMENT(2, "Settlement", 200, 50, 50, 0, 3, 50),
    CAPITAL(3, "Capital", 300, 0, 100, 50, 5, 0);

    private final int number;
    private final String label;
    private final int storage;
    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final int turns;
    private final int heal;

    TownHallLevel(int number, String label, int storage,
                  int woodCost, int stoneCost, int ironCost, int turns, int heal) {
        this.number = number;
        this.label = label;
        this.storage = storage;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
        this.turns = turns;
        this.heal = heal;
    }

    public int getNumber() {
        return number;
    }

    public String getLabel() {
        return label;
    }

    public int getStorage() {
        return storage;
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

    public int getTurns() {
        return turns;
    }

    public int getHeal() {
        return heal;
    }

    public TownHallLevel next() {
        if (this == BASE_CAMP) {
            return SETTLEMENT;
        }
        if (this == SETTLEMENT) {
            return CAPITAL;
        }
        return null;
    }

    public static TownHallLevel of(int number) {
        for (TownHallLevel value : values()) {
            if (value.number == number) {
                return value;
            }
        }
        return BASE_CAMP;
    }
}
