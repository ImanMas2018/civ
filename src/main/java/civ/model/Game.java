package civ.model;

import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.List;

/**
 * Game state plus the rules the view and controller are allowed to ask.
 * Buildings, the stockpile and the turn cycle arrive in later steps.
 */
public class Game {

    private final GameMap map;
    private final int centreCol;
    private final int centreRow;
    private final List<Unit> units = new ArrayList<>();

    private Unit selected;

    public Game(long seed) {
        this.map = new GameMap(22, 18);
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;

        new MapGenerator(seed).fill(map, centreCol, centreRow);
        setUpStartingPosition();
    }

    private void setUpStartingPosition() {
        Hex centre = map.get(centreCol, centreRow);
        centre.setOwned(true);
        centre.setDiscovered(true);
        List<Hex> ring = map.neighbours(centre);
        for (Hex neighbour : ring) {
            neighbour.setOwned(true);
            neighbour.setDiscovered(true);
        }

        // Spread the five starting units so each one can be clicked.
        addUnit(new Explorer(centreCol, centreRow));
        addUnit(new Builder(ring.get(0).getCol(), ring.get(0).getRow()));
        addUnit(new Builder(ring.get(1).getCol(), ring.get(1).getRow()));
        addUnit(new Worker(ring.get(2).getCol(), ring.get(2).getRow()));
        addUnit(new Worker(ring.get(3).getCol(), ring.get(3).getRow()));
    }

    public void addUnit(Unit unit) {
        units.add(unit);
        revealAround(unit);
    }

    public void removeUnit(Unit unit) {
        units.remove(unit);
        if (selected == unit) {
            selected = null;
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

    public List<Unit> getUnits() {
        return units;
    }

    public Unit getSelected() {
        return selected;
    }

    public void select(Unit unit) {
        selected = unit;
    }

    public Unit unitAt(Hex hex) {
        for (Unit unit : units) {
            if (unit.isOn(hex)) {
                return unit;
            }
        }
        return null;
    }

    public Hex hexOf(Unit unit) {
        return map.get(unit.getCol(), unit.getRow());
    }

    /**
     * One step onto an empty neighbour, if the unit can afford the terrain cost.
     * Cost lives on {@link Terrain}, not on the unit.
     */
    public boolean canMove(Unit unit, Hex target) {
        if (unit == null || target == null) {
            return false;
        }
        if (unit.isBusy()) {
            return false;
        }
        if (unitAt(target) != null) {
            return false;
        }

        int distance = HexGeometry.distance(
                unit.getCol(), unit.getRow(),
                target.getCol(), target.getRow());
        if (distance != 1) {
            return false;
        }

        return unit.canSpend(target.getTerrain().getMoveCost());
    }

    public void moveUnit(Unit unit, Hex target) {
        if (!canMove(unit, target)) {
            return;
        }
        unit.spend(target.getTerrain().getMoveCost());
        unit.moveTo(target);
        revealAround(unit);
    }

    /** Fog is removed forever — nothing in the program ever sets discovered back to false. */
    public void revealAround(Unit unit) {
        Hex centre = hexOf(unit);
        if (centre == null) {
            return;
        }
        for (Hex hex : map.withinRange(centre, unit.getVisionRadius())) {
            hex.setDiscovered(true);
        }
    }
}
