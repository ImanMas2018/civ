package civ.model;

/**
 * Training table. {@link #create} is a tiny Factory Method: the rest of the
 * program asks for "a unit" and never writes {@code new Worker(...)} itself.
 */
public enum UnitBlueprint {

    WORKER("Worker", 10, 0, 2, false, 1),
    BUILDER("Builder", 10, 10, 2, false, 1),
    EXPLORER("Explorer", 15, 0, 3, false, 1),
    BORDER_EXPANDER("Border Expander", 20, 20, 4, false, 1),
    SWORDSMAN("Swordsman", 20, 10, 3, true, 1),
    ARCHER("Archer", 15, 10, 3, true, 2),
    CAVALRY("Cavalry", 25, 15, 4, true, 2);

    private final String label;
    private final int foodCost;
    private final int woodCost;
    private final int turns;
    private final boolean military;
    private final int requiredLevel;

    UnitBlueprint(String label, int foodCost, int woodCost, int turns,
                  boolean military, int requiredLevel) {
        this.label = label;
        this.foodCost = foodCost;
        this.woodCost = woodCost;
        this.turns = turns;
        this.military = military;
        this.requiredLevel = requiredLevel;
    }

    public String getLabel() {
        return label;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getTurns() {
        return turns;
    }

    public boolean isMilitary() {
        return military;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public Unit create(int col, int row) {
        switch (this) {
            case WORKER:
                return new Worker(col, row);
            case BUILDER:
                return new Builder(col, row);
            case EXPLORER:
                return new Explorer(col, row);
            case BORDER_EXPANDER:
                return new BorderExpander(col, row);
            case SWORDSMAN:
                return new Swordsman(col, row);
            case ARCHER:
                return new Archer(col, row);
            case CAVALRY:
                return new Cavalry(col, row);
            default:
                throw new IllegalStateException("unknown blueprint " + this);
        }
    }
}
