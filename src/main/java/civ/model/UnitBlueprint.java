package civ.model;

/**
 * Training table. {@link #create} is a tiny Factory Method: the rest of the
 * program asks for "a unit" and never writes {@code new Worker(...)} itself.
 */
public enum UnitBlueprint {

    WORKER("Worker", 10, 0, 2),
    BUILDER("Builder", 10, 10, 2),
    EXPLORER("Explorer", 15, 0, 3),
    BORDER_EXPANDER("Border Expander", 20, 20, 4);

    private final String label;
    private final int foodCost;
    private final int woodCost;
    private final int turns;

    UnitBlueprint(String label, int foodCost, int woodCost, int turns) {
        this.label = label;
        this.foodCost = foodCost;
        this.woodCost = woodCost;
        this.turns = turns;
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
            default:
                throw new IllegalStateException("unknown blueprint " + this);
        }
    }
}
