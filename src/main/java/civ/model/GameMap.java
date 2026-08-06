package civ.model;

import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.List;

/** The grid of hexes. Uses odd-r offset coordinates stored in a 2-D array. */
public class GameMap {

    private final int cols;
    private final int rows;
    private final Hex[][] hexes;

    public GameMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.hexes = new Hex[cols][rows];
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public boolean inside(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    public Hex get(int col, int row) {
        if (!inside(col, row)) {
            return null;
        }
        return hexes[col][row];
    }

    public void set(Hex hex) {
        hexes[hex.getCol()][hex.getRow()] = hex;
    }

    public List<Hex> neighbours(Hex hex) {
        List<Hex> result = new ArrayList<>();
        for (int direction = 0; direction < 6; direction++) {
            int[] pos = HexGeometry.neighbour(hex.getCol(), hex.getRow(), direction);
            Hex neighbour = get(pos[0], pos[1]);
            if (neighbour != null) {
                result.add(neighbour);
            }
        }
        return result;
    }

    public List<Hex> withinRange(Hex centre, int radius) {
        List<Hex> result = new ArrayList<>();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                int d = HexGeometry.distance(centre.getCol(), centre.getRow(), col, row);
                if (d <= radius) {
                    result.add(hexes[col][row]);
                }
            }
        }
        return result;
    }
}
