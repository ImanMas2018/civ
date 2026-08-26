package civ.model;

public class BorderExpander extends Unit {

    public BorderExpander(int col, int row) {
        super("Border Expander", 3, 2, col, row);
    }

    @Override
    public String getLetter() {
        return "X";
    }
}
