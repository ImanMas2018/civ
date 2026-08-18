package civ.model;

import civ.model.command.ResearchTechCommand;
import civ.model.command.TrainUnitCommand;
import civ.model.command.UpgradeTownHallCommand;
import civ.model.event.EventBus;
import civ.model.event.GameEvent;
import civ.model.factory.BuildingFactory;
import civ.model.factory.UnitFactory;
import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.List;

/**
 * Game state plus the rules the view and controller are allowed to ask.
 * End Turn itself lives in {@link TurnEngine}.
 */
public class Game {

    private final GameMap map;
    private final Empire empire = new Empire();
    private final EventBus bus = new EventBus();
    private final UnitFactory unitFactory = new UnitFactory(this);
    private final BuildingFactory buildingFactory = new BuildingFactory(bus);
    private final int centreCol;
    private final int centreRow;

    private int turn = 1;
    private Unit selected;
    private Hex inspected;
    private boolean starving = false;
    private final List<String> log = new ArrayList<>();

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

        empire.getStock().add(ResourceType.FOOD, 999);
        empire.getStock().add(ResourceType.WOOD, 999);
        empire.getStock().add(ResourceType.STONE, 999);
        empire.getStock().add(ResourceType.IRON, 999);
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

    public EventBus getBus() {
        return bus;
    }

    public UnitFactory getUnitFactory() {
        return unitFactory;
    }

    public BuildingFactory getBuildingFactory() {
        return buildingFactory;
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

    public void nextTurn() {
        turn++;
    }

    public boolean isStarving() {
        return starving;
    }

    public void setStarving(boolean starving) {
        this.starving = starving;
    }

    public List<String> getLog() {
        return log;
    }

    public void addLog(String message) {
        log.add("Turn " + turn + ": " + message);
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

    public Hex getInspected() {
        return inspected;
    }

    public void inspect(Hex hex) {
        inspected = hex;
    }

    public String describeInspected() {
        Hex hex = inspected;
        if (hex == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        text.append(hex.getTerrain().getLabel());
        if (!hex.getTerrain().isPassable()) {
            text.append(" — impassable");
        } else if (hex.getTerrain().isSea()) {
            text.append(empire.hasTech(Tech.SEAFARING)
                    ? " — enter with Seafaring (AP to 0)"
                    : " — needs Seafaring");
        } else {
            text.append(" — move cost ").append(hex.getTerrain().getMoveCost());
        }
        if (hex.hasRoad()) {
            text.append(" — road");
        }
        if (isCoastal(hex)) {
            text.append(" — coast");
        }
        if (hex.hasResource()) {
            String deposit = hex.getTerrain().isSea() ? "Fish" : hex.getDeposit().getLabel();
            text.append(". ").append(deposit).append(" ").append(hex.getDepositAmount());
        } else if (hex.isExhausted()) {
            text.append(". deposit empty");
        }
        boolean buildable = hex.getTerrain().isLand() && hex.isOwned() && hex.getBuilding() == null;
        text.append(buildable ? ". Can build here." : ". Cannot build here.");
        return text.toString();
    }

    public boolean isCoastal(Hex hex) {
        if (hex == null || !hex.getTerrain().isLand()) {
            return false;
        }
        for (Hex neighbour : map.neighbours(hex)) {
            if (neighbour.getTerrain().isSea()) {
                return true;
            }
        }
        return false;
    }

    /**
     * AP to step from the unit's hex onto {@code to}. Sea entry spends whatever AP
     * is left (the unit becomes a boat). Mountain Range is impassable.
     */
    public int moveCost(Unit unit, Hex to) {
        Hex from = hexOf(unit);
        if (from == null || to == null) {
            return Integer.MAX_VALUE;
        }
        if (!to.getTerrain().isPassable()) {
            return Integer.MAX_VALUE;
        }
        if (to.getTerrain().isSea()) {
            return Math.max(1, unit.getAp());
        }

        int cost = to.getTerrain().getMoveCost();
        if (from.hasRoad() && to.hasRoad()) {
            cost -= 1;
        }
        Edge edge = map.getEdges().find(from, to);
        if (edge != null && edge.hasRiver()) {
            boolean bridged = from.hasRoad() && to.hasRoad();
            cost += bridged ? 0 : 2;
        }
        return Math.max(1, cost);
    }

    public Unit unitAt(Hex hex) {
        List<Unit> here = unitsAt(hex);
        return here.isEmpty() ? null : here.get(0);
    }

    public List<Unit> unitsAt(Hex hex) {
        List<Unit> result = new ArrayList<>();
        for (Unit unit : empire.getUnits()) {
            if (unit.isOn(hex)) {
                result.add(unit);
            }
        }
        return result;
    }

    /** Next unit on this hex, wrapping around. Used to click-cycle a stack. */
    public Unit nextUnitOn(Hex hex, Unit current) {
        List<Unit> here = unitsAt(hex);
        if (here.isEmpty()) {
            return null;
        }
        int index = here.indexOf(current);
        if (index < 0) {
            return here.get(0);
        }
        return here.get((index + 1) % here.size());
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

        int distance = HexGeometry.distance(
                unit.getCol(), unit.getRow(),
                target.getCol(), target.getRow());
        if (distance != 1) {
            return false;
        }
        if (!target.getTerrain().isPassable()) {
            return false;
        }
        if (target.getTerrain().isSea()) {
            return empire.hasTech(Tech.SEAFARING) && unit.getAp() > 0;
        }
        return unit.canSpend(moveCost(unit, target));
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
        if (!hex.getTerrain().isLand()) {
            return false;
        }
        if (hex.getBuilding() != null) {
            return false;
        }
        if (type == BuildingType.DOCK && !isCoastal(hex)) {
            return false;
        }
        if (!builder.canSpend(type.getApCost())) {
            return false;
        }
        if (type.getRequiredTech() != null && !empire.hasTech(type.getRequiredTech())) {
            return false;
        }
        if (type.getRequiredLevel() > empire.getTownHall().getLevel()) {
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

    public boolean canExpandBorder(BorderExpander expander, Hex hex) {
        if (expander == null || hex == null) {
            return false;
        }
        return hex.isDiscovered();
    }

    public boolean canTrain(UnitBlueprint blueprint) {
        if (empire.getTownHall().isBusy()) {
            return false;
        }
        return unitFactory.canCreate(blueprint);
    }

    public boolean canResearch(Tech tech) {
        return !empire.getTownHall().isBusy() && empire.canResearch(tech);
    }

    public boolean canUpgradeTownHall() {
        if (empire.getTownHall().isBusy()) {
            return false;
        }
        TownHallLevel next = empire.getTownHall().getRank().next();
        if (next == null) {
            return false;
        }
        return empire.getStock().canPay(next.getWoodCost(), next.getStoneCost(), next.getIronCost());
    }

    public boolean hasIdleUnitWithAp() {
        for (Unit unit : empire.getUnits()) {
            if (!unit.isBusy() && unit.getAp() > 0) {
                return true;
            }
        }
        return false;
    }

    public void moveUnit(Unit unit, Hex target) {
        if (!canMove(unit, target)) {
            return;
        }
        if (target.getTerrain().isSea()) {
            unit.moveTo(target);
            unit.emptyAp();
            revealAround(unit);
            return;
        }
        unit.spend(moveCost(unit, target));
        unit.moveTo(target);
        revealAround(unit);
    }

    public void build(Builder builder, BuildingType type, Hex hex) {
        if (!canBuild(builder, type, hex)) {
            return;
        }

        empire.getStock().pay(type.getWoodCost(), type.getStoneCost(), type.getIronCost());
        builder.spend(type.getApCost());

        Building building = buildingFactory.create(type, hex);
        empire.getBuildings().add(building);

        if (type == BuildingType.SETTLEMENT) {
            empire.raiseUnitCap(5);
        }

        builder.useCharge();
        if (!builder.hasCharge()) {
            removeUnit(builder);
        }
    }

    public boolean canBuildRoad(Builder builder) {
        Hex hex = hexOf(builder);
        if (builder == null || hex == null) {
            return false;
        }
        if (!hex.isOwned() || !hex.getTerrain().isLand() || hex.hasRoad()) {
            return false;
        }
        return builder.canSpend(1) && empire.getStock().canPay(ResourceType.WOOD, 8);
    }

    public void buildRoad(Builder builder) {
        if (!canBuildRoad(builder)) {
            return;
        }
        empire.getStock().add(ResourceType.WOOD, -8);
        builder.spend(1);
        hexOf(builder).setRoad(true);
        addLog("A road was built.");
    }

    public boolean canBuildWall(Builder builder, Hex other) {
        Hex here = hexOf(builder);
        if (builder == null || here == null || other == null) {
            return false;
        }
        if (HexGeometry.distance(here.getCol(), here.getRow(), other.getCol(), other.getRow()) != 1) {
            return false;
        }
        if (!here.isDiscovered() || !other.isDiscovered()) {
            return false;
        }
        if (!here.isOwned() && !other.isOwned()) {
            return false;
        }
        if (here.getTerrain().isSea() || here.getTerrain().isMountainRange()
                || other.getTerrain().isSea() || other.getTerrain().isMountainRange()) {
            return false;
        }
        Edge edge = map.getEdges().getOrCreate(here, other);
        if (edge.hasWall()) {
            return false;
        }
        return builder.canSpend(1)
                && empire.getStock().canPay(15, 20, 0);
    }

    public void buildWall(Builder builder, Hex other) {
        if (!canBuildWall(builder, other)) {
            return;
        }
        empire.getStock().pay(15, 20, 0);
        builder.spend(1);
        map.getEdges().getOrCreate(hexOf(builder), other).setWall(new Wall());
        addLog("A wall was raised.");
    }

    public boolean canDemolish(Builder builder, Hex hex) {
        if (builder == null || hex == null) {
            return false;
        }
        Hex here = hexOf(builder);
        int distance = HexGeometry.distance(here.getCol(), here.getRow(), hex.getCol(), hex.getRow());
        if (distance > 1) {
            return false;
        }
        if (!builder.canSpend(1)) {
            return false;
        }
        Building building = hex.getBuilding();
        if (building instanceof TownHall) {
            return false;
        }
        boolean hasBuilding = building != null && hex.isOwned();
        return hasBuilding || hex.hasRoad();
    }

    public void demolish(Builder builder, Hex hex) {
        if (!canDemolish(builder, hex)) {
            return;
        }
        builder.spend(1);
        Building building = hex.getBuilding();
        if (building != null && !(building instanceof TownHall)) {
            if (building instanceof ProductionBuilding) {
                ((ProductionBuilding) building).releaseAllWorkers();
            }
            hex.setBuilding(null);
            empire.getBuildings().remove(building);
            bus.publish(GameEvent.BUILDING_DESTROYED, building);
            addLog("The " + building.getType().getLabel() + " was demolished. Resources are lost.");
        }
        if (hex.hasRoad()) {
            hex.setRoad(false);
            addLog("The road was demolished.");
        }
    }

    public boolean canDemolishWall(Builder builder, Hex other) {
        Hex here = hexOf(builder);
        if (here == null || other == null) {
            return false;
        }
        if (HexGeometry.distance(here.getCol(), here.getRow(), other.getCol(), other.getRow()) != 1) {
            return false;
        }
        Edge edge = map.getEdges().find(here, other);
        return edge != null && edge.hasWall() && builder.canSpend(1);
    }

    public void demolishWall(Builder builder, Hex other) {
        if (!canDemolishWall(builder, other)) {
            return;
        }
        builder.spend(1);
        Edge edge = map.getEdges().find(hexOf(builder), other);
        if (edge != null) {
            edge.setWall(null);
            addLog("The wall was demolished.");
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

    public void expandBorder(BorderExpander expander, Hex hex) {
        if (!canExpandBorder(expander, hex)) {
            return;
        }
        hex.setOwned(true);
        hex.setDiscovered(true);
        for (Hex neighbour : map.neighbours(hex)) {
            neighbour.setOwned(true);
            neighbour.setDiscovered(true);
        }
        removeUnit(expander);
        addLog("The border was expanded and the Border Expander was consumed.");
    }

    public void train(UnitBlueprint blueprint) {
        if (!canTrain(blueprint)) {
            return;
        }
        empire.getTownHall().start(new TrainUnitCommand(blueprint), this);
    }

    public void research(Tech tech) {
        if (!canResearch(tech)) {
            return;
        }
        empire.getTownHall().start(new ResearchTechCommand(tech), this);
    }

    public void upgradeTownHall() {
        if (!canUpgradeTownHall()) {
            return;
        }
        TownHallLevel next = empire.getTownHall().getRank().next();
        empire.getTownHall().start(new UpgradeTownHallCommand(next), this);
    }

    public void cancelTownHallOrder() {
        empire.getTownHall().cancelCommand(this);
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
