package civ.model;

public class Barbarian extends MilitaryUnit {

    public Barbarian(int col, int row) {
        super("Barbarian", 2, 2, col, row, 1, 8, 1);
    }

    public Barbarian(long id, long createdAt, int col, int row) {
        super(id, createdAt, "Barbarian", 2, 2, col, row, 1, 8, 1);
    }

    @Override
    public boolean isHostile() {
        return true;
    }

    @Override
    public String getLetter() {
        return "R";
    }
}
