package civ.model;

public class Swordsman extends MilitaryUnit {

    public Swordsman(int col, int row) {
        super("Swordsman", 2, 2, col, row, 1, 10, 1);
    }

    public Swordsman(long id, long createdAt, int col, int row) {
        super(id, createdAt, "Swordsman", 2, 2, col, row, 1, 10, 1);
    }

    @Override
    public String getLetter() {
        return "S";
    }
}
