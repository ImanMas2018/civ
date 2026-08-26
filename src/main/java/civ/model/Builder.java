package civ.model;

public class Builder extends Unit {

    private int charges;

    public Builder(int col, int row) {
        super("Builder", 4, 1, col, row);
        this.charges = 3;
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
