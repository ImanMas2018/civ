package civ.model;

/** One tile of the map. Coordinates and terrain never change; fog, ownership and deposit amount do. */
public class Hex {

    private final int col;
    private final int row;
    private final Terrain terrain;
    private final ResourceType deposit;
    private int depositAmount;

    private boolean discovered = false;
    private boolean owned = false;
    private Building building;

    public Hex(int col, int row, Terrain terrain, ResourceType deposit, int depositAmount) {
        this.col = col;
        this.row = row;
        this.terrain = terrain;
        this.deposit = deposit;
        this.depositAmount = depositAmount;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public ResourceType getDeposit() {
        return deposit;
    }

    public int getDepositAmount() {
        return depositAmount;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public boolean isOwned() {
        return owned;
    }

    public boolean hasResource() {
        return deposit != null && depositAmount > 0;
    }

    public boolean isExhausted() {
        return deposit != null && depositAmount <= 0;
    }

    public void takeResource(int amount) {
        depositAmount = Math.max(0, depositAmount - amount);
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }
}
