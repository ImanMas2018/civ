package civ.model;

/** Hostile beast. A defending animal hex rolls 1 die. */
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
