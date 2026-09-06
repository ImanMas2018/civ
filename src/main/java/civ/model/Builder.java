package civ.model;

public class Builder extends Unit {

    private int charges;

    public Builder(int col, int row) {
        super("Builder", 4, 1, col, row);
        this.charges = 3;
    }

    public Builder(long id, long createdAt, int col, int row) {
        super(id, createdAt, "Builder", 4, 1, col, row);
        this.charges = 3;
    }

    public void setCharges(int charges) {
        this.charges = Math.max(0, charges);
    }

    public int getCharges() {
        return charges;
    }

    public boolean hasCharge() {
        return charges > 0;
    }

    public void useCharge() {
        charges--;
    }

    @Override
    public String describe() {
        return super.describe() + "  builds left " + charges;
    }
}
