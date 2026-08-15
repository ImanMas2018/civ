package civ.model;

/**
 * Shared state for every unit: position, action points and vision.
 * Concrete types only change the numbers and add their own extra ability.
 */
public abstract class Unit {

    private final String typeName;
    private final int maxAp;
    private final int visionRadius;

    private int ap;
    private int col;
    private int row;

    protected Unit(String typeName, int maxAp, int visionRadius, int col, int row) {
        this.typeName = typeName;
        this.maxAp = maxAp;
        this.visionRadius = visionRadius;
        this.ap = maxAp;
        this.col = col;
        this.row = row;
    }

    public String getTypeName() {
        return typeName;
    }

    /** Letter drawn on the map (E, B, W, X). */
    public String getLetter() {
        return typeName.substring(0, 1);
    }

    public int getMaxAp() {
        return maxAp;
    }

    public int getAp() {
        return ap;
    }

    public int getVisionRadius() {
        return visionRadius;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public boolean isOn(Hex hex) {
        return hex.getCol() == col && hex.getRow() == row;
    }

    public void moveTo(Hex hex) {
        this.col = hex.getCol();
        this.row = hex.getRow();
    }

    public boolean canSpend(int cost) {
        return !isBusy() && ap >= cost;
    }

    public void spend(int cost) {
        ap -= cost;
    }

    /** Start of a new turn. During starvation the units are weak, so they get less. */
    public void refresh(int starvationPenalty) {
        ap = Math.max(0, maxAp - starvationPenalty);
    }

    /** A stationed worker is busy and cannot act. Other units are never busy. */
    public boolean isBusy() {
        return false;
    }

    /** One short line for the side panel. Subclasses add their own details. */
    public String describe() {
        return typeName + "  AP " + ap + "/" + maxAp;
    }
}
