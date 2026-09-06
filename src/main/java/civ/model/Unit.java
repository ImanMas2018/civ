package civ.model;

public abstract class Unit extends Entity {

    private static final int DEFAULT_BODY_HP = 50;

    private final String typeName;
    private final int maxAp;
    private final int visionRadius;

    private long ownerId;
    private int ap;
    private int col;
    private int row;
    private int bodyHp = DEFAULT_BODY_HP;
    private int maxBodyHp = DEFAULT_BODY_HP;

    protected Unit(String typeName, int maxAp, int visionRadius, int col, int row) {
        this.typeName = typeName;
        this.maxAp = maxAp;
        this.visionRadius = visionRadius;
        this.ap = maxAp;
        this.col = col;
        this.row = row;
    }

    protected Unit(long id, long createdAt, String typeName, int maxAp, int visionRadius,
                   int col, int row) {
        super(id, createdAt);
        this.typeName = typeName;
        this.maxAp = maxAp;
        this.visionRadius = visionRadius;
        this.ap = maxAp;
        this.col = col;
        this.row = row;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
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

    public void setAp(int ap) {
        this.ap = Math.max(0, ap);
    }

    public void emptyAp() {
        ap = 0;
    }

    /** Start of a new turn. During starvation the units are weak, so they get less. */
    public void refresh(int starvationPenalty) {
        ap = Math.max(0, maxAp - starvationPenalty);
    }

    public int getBodyHp() {
        return bodyHp;
    }

    public int getMaxBodyHp() {
        return maxBodyHp;
    }

    protected void setBodyHp(int current, int max) {
        this.bodyHp = current;
        this.maxBodyHp = max;
    }

    public void restoreBodyHp(int current, int max) {
        setBodyHp(current, max);
    }

    public void setBodyHpAbsolute(int current) {
        this.bodyHp = Math.max(0, Math.min(maxBodyHp, current));
    }

    public void damageBody(int amount) {
        bodyHp = Math.max(0, bodyHp - amount);
    }

    public boolean isBodyDead() {
        return bodyHp <= 0;
    }

    /** A stationed worker is busy and cannot act. Other units are never busy. */
    public boolean isBusy() {
        return false;
    }

    public boolean isMilitary() {
        return false;
    }

    /** One short line for the side panel. Subclasses add their own details. */
    public String describe() {
        return typeName + "  AP " + ap + "/" + maxAp;
    }
}
