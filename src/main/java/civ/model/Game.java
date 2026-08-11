package civ.model;

import civ.util.HexGeometry;
import java.util.List;

/**
 * Game state plus the rules the view and controller are allowed to ask.
 * The turn cycle (End Turn, queue, starvation) arrives in Step 5.
 */
public class Game {

    private final GameMap map;
    private final Empire empire = new Empire();
    private final int centreCol;
    private final int centreRow;

    private int turn = 1;
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

        TownHall townHall = new TownHall(centre);
        centre.setBuilding(townHall);
        empire.setTownHall(townHall);

        centre.setOwned(true);
        centre.setDiscovered(true);
        List<Hex> ring = map.neighbours(centre);
        for (Hex neighbour : ring) {
            neighbour.setOwned(true);
            neighbour.setDiscovered(true);
        }

        addUnit(new Explorer(centreCol, centreRow));
        addUnit(new Builder(ring.get(0).getCol(), ring.get(0).getRow()));
        addUnit(new Builder(ring.get(1).getCol(), ring.get(1).getRow()));
        addUnit(new Worker(ring.get(2).getCol(), ring.get(2).getRow()));
        addUnit(new Worker(ring.get(3).getCol(), ring.get(3).getRow()));

        empire.getStock().add(ResourceType.FOOD, 40);
        empire.getStock().add(ResourceType.WOOD, 40);
        empire.getStock().add(ResourceType.STONE, 20);
    }

    public void addUnit(Unit unit) {
        empire.getUnits().add(unit);
        revealAround(unit);
    }

    public void removeUnit(Unit unit) {
        empire.getUnits().remove(unit);
        if (selected == unit) {
            selected = null;
        }
    }

    public GameMap getMap() {
        return map;
    }

    public Empire getEmpire() {
        return empire;
    }

    public int getCentreCol() {
        return centreCol;
    }

    public int getCentreRow() {
        return centreRow;
    }

    public int getTurn() {
        return turn;
    }

    public List<Unit> getUnits() {
        return empire.getUnits();
    }

    public Unit getSelected() {
        return selected;
    }

    public void select(Unit unit) {
        selected = unit;
    }

    public Unit unitAt(Hex hex) {
        for (Unit unit : empire.getUnits()) {
            if (unit.isOn(hex)) {
                return unit;
            }
        }
        return null;
    }

    public Hex hexOf(Unit unit) {
        return map.get(unit.getCol(), unit.getRow());
    }

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

    public boolean canBuild(Builder builder, BuildingType type, Hex hex) {
        if (builder == null || hex == null) {
            return false;
        }
        if (!builder.hasCharge()) {
            return false;
        }
        if (!builder.isOn(hex)) {
            return false;
        }
        if (!hex.isOwned()) {
            return false;
        }
        if (hex.getBuilding() != null) {
            return false;
        }
        if (!builder.canSpend(type.getApCost())) {
            return false;
        }
        if (type.getRequiredTech() != null && !empire.hasTech(type.getRequiredTech())) {
            return false;
        }
        if (type.getRequiredTerrain() != null && hex.getTerrain() != type.getRequiredTerrain()) {
            return false;
        }
        if (type == BuildingType.SETTLEMENT) {
            if (hex.hasResource()) {
                return false;
            }
        } else if (type.getRequiredDeposit() != null) {
            if (hex.getDeposit() != type.getRequiredDeposit() || !hex.hasResource()) {
                return false;
            }
        }
        return empire.getStock().canPay(type.getWoodCost(), type.getStoneCost(), type.getIronCost());
    }

    public boolean canStation(Worker worker) {
        if (worker == null || worker.isBusy()) {
            return false;
        }
        Hex hex = hexOf(worker);
        Building building = hex.getBuilding();
        if (!(building instanceof ProductionBuilding)) {
            return false;
        }
        ProductionBuilding production = (ProductionBuilding) building;
        if (production.getType().getProduces() == null) {
            return false;
        }
        if (!production.hasRoom()) {
            return false;
        }
        return worker.canSpend(1);
    }

    public void moveUnit(Unit unit, Hex target) {
        if (!canMove(unit, target)) {
            return;
        }
        unit.spend(target.getTerrain().getMoveCost());
        unit.moveTo(target);
        revealAround(unit);
    }

    public void build(Builder builder, BuildingType type, Hex hex) {
        if (!canBuild(builder, type, hex)) {
            return;
        }

        empire.getStock().pay(type.getWoodCost(), type.getStoneCost(), type.getIronCost());
        builder.spend(type.getApCost());

        ProductionBuilding building = new ProductionBuilding(type, hex);
        hex.setBuilding(building);
        empire.getBuildings().add(building);

        if (type == BuildingType.SETTLEMENT) {
            empire.raiseUnitCap(5);
        }

        builder.useCharge();
        if (!builder.hasCharge()) {
            removeUnit(builder);
        }
    }

    public void station(Worker worker) {
        if (!canStation(worker)) {
            return;
        }
        ProductionBuilding building = (ProductionBuilding) hexOf(worker).getBuilding();
        worker.spend(1);
        building.addWorker(worker);
    }

    public void unstation(Worker worker) {
        if (worker.getStation() == null) {
            return;
        }
        worker.getStation().removeWorker(worker);
    }

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
