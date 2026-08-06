package civ.model;

/**
 * Minimal game state for Step 2: a generated map with the starting ring visible.
 * Units, buildings and turns arrive in later steps.
 */
public class Game {

    private final GameMap map;
    private final int centreCol;
    private final int centreRow;

    public Game(long seed) {
        this.map = new GameMap(22, 18);
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;

        new MapGenerator(seed).fill(map, centreCol, centreRow);
        revealStartingArea();
    }

    private void revealStartingArea() {
        Hex centre = map.get(centreCol, centreRow);
        centre.setOwned(true);
        centre.setDiscovered(true);
        for (Hex neighbour : map.neighbours(centre)) {
            neighbour.setOwned(true);
            neighbour.setDiscovered(true);
        }
    }

    public GameMap getMap() {
        return map;
    }

    public int getCentreCol() {
        return centreCol;
    }

    public int getCentreRow() {
        return centreRow;
    }
}
