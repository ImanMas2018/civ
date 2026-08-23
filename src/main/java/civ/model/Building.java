package civ.model;

/** Shared building state. Subclasses decide how much they produce. */
public abstract class Building {

    private static final int DEFAULT_HP = 40;

    private final BuildingType type;
    private final Hex hex;
    private int unpaidTurns = 0;
    private int hp = DEFAULT_HP;
    private int maxHp = DEFAULT_HP;
    /** Inclusive turn number through which production is paused (flood). 0 = not paused. */
    private int pausedUntilTurn = 0;

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

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    protected void setHealth(int current, int max) {
        this.hp = current;
        this.maxHp = max;
    }

    protected void setMaxHp(int max) {
        this.maxHp = max;
    }

    protected void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void pauseUntil(int turn) {
        pausedUntilTurn = Math.max(pausedUntilTurn, turn);
    }

    public int getPausedUntilTurn() {
        return pausedUntilTurn;
    }

    public void setPausedUntilTurn(int turn) {
        pausedUntilTurn = turn;
    }

    public boolean isPaused(int currentTurn) {
        return pausedUntilTurn > 0 && currentTurn <= pausedUntilTurn;
    }

    /** How much of {@code getType().getProduces()} this building makes this turn. */
    public abstract int outputPerTurn(Empire empire, GameMap map);
}
