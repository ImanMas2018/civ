package civ.model;

/** Shared building state. Subclasses decide how much they produce. */
public abstract class Building {

    private final BuildingType type;
    private final Hex hex;
    private int unpaidTurns = 0;

    protected Building(BuildingType type, Hex hex) {
        this.type = type;
        this.hex = hex;
    }

    public BuildingType getType() {
        return type;
    }

    public Hex getHex() {
        return hex;
    }

    public int getUnpaidTurns() {
        return unpaidTurns;
    }

    public void markPaid() {
        unpaidTurns = 0;
    }

    public void markUnpaid() {
        unpaidTurns++;
    }

    public boolean isCollapsed() {
        return unpaidTurns >= 3;
    }

    /** How much of {@code getType().getProduces()} this building makes this turn. */
    public abstract int outputPerTurn(Empire empire, GameMap map);
}
