package civ.model;

public class Archer extends MilitaryUnit {

    public Archer(int col, int row) {
        super("Archer", 2, 2, col, row, 1, 6, 2);
    }

    @Override
    public String getLetter() {
        return "A";
    }
}
