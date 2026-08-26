package civ.model;

public class WildAnimal extends MilitaryUnit {

    public WildAnimal(int col, int row) {
        super("Wild Animal", 2, 1, col, row, 1, 4, 1);
    }

    @Override
    public boolean isHostile() {
        return true;
    }

    @Override
    public String getLetter() {
        return "N";
    }
}
