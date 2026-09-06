package civ.model;

public class Hex {

    private final int col;
    private final int row;
    private final Terrain terrain;
    private final ResourceType deposit;
    private int depositAmount;

    /**
     * Territory claimed by a non-player (tribe camp ring, etc.).
     * Player ownership lives on {@link Fog}, not here.
     */
    private boolean reserved = false;
    private boolean road = false;
    private boolean blocked = false;
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

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public boolean hasRoad() {
        return road;
    }

    public void setRoad(boolean road) {
        this.road = road;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
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

    public void setDepositAmount(int depositAmount) {
        this.depositAmount = Math.max(0, depositAmount);
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }
}
