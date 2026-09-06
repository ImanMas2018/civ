package civ.model;

/**
 * One player's private view of the world: what they have discovered, and what they own.
 * Uses boolean grids rather than coordinate sets so lookups stay obvious and cheap.
 */
public class Fog {

    private final boolean[][] discovered;
    private final boolean[][] owned;

    public Fog(int cols, int rows) {
        this.discovered = new boolean[cols][rows];
        this.owned = new boolean[cols][rows];
    }

    public boolean isDiscovered(Hex hex) {
        return discovered[hex.getCol()][hex.getRow()];
    }

    public boolean isOwned(Hex hex) {
        return owned[hex.getCol()][hex.getRow()];
    }

    public boolean isDiscovered(int col, int row) {
        return discovered[col][row];
    }

    public boolean isOwned(int col, int row) {
        return owned[col][row];
    }

    public void discover(Hex hex) {
        discovered[hex.getCol()][hex.getRow()] = true;
    }

    public void claim(Hex hex) {
        owned[hex.getCol()][hex.getRow()] = true;
        discovered[hex.getCol()][hex.getRow()] = true;
    }

    public void release(Hex hex) {
        owned[hex.getCol()][hex.getRow()] = false;
    }

    public void setDiscovered(Hex hex, boolean value) {
        discovered[hex.getCol()][hex.getRow()] = value;
    }

    public void setOwned(Hex hex, boolean value) {
        owned[hex.getCol()][hex.getRow()] = value;
        if (value) {
            discovered[hex.getCol()][hex.getRow()] = true;
        }
    }

    public boolean[][] copyDiscovered() {
        return copyGrid(discovered);
    }

    public boolean[][] copyOwned() {
        return copyGrid(owned);
    }

    public void loadDiscovered(boolean[][] source) {
        loadGrid(discovered, source);
    }

    public void loadOwned(boolean[][] source) {
        loadGrid(owned, source);
    }

    private static boolean[][] copyGrid(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private static void loadGrid(boolean[][] target, boolean[][] source) {
        if (source == null) {
            return;
        }
        int cols = Math.min(target.length, source.length);
        for (int c = 0; c < cols; c++) {
            int rows = Math.min(target[c].length, source[c].length);
            System.arraycopy(source[c], 0, target[c], 0, rows);
        }
    }
}
