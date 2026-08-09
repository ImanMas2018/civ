package civ.model;

/** Claims a hex and its neighbours, then is consumed. Used from Step 5. */
public class BorderExpander extends Unit {

    public BorderExpander(int col, int row) {
        super("Border Expander", 3, 2, col, row);
    }
}
