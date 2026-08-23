package civ.model;

import civ.model.command.ResearchTechCommand;
import civ.model.command.TrainUnitCommand;
import civ.model.command.UpgradeTownHallCommand;
import civ.model.combat.ArcherHandler;
import civ.model.combat.Battle;
import civ.model.combat.BattleReport;
import civ.model.combat.CavalryHandler;
import civ.model.combat.DamageHandler;
import civ.model.combat.Dice;
import civ.model.combat.HostileHandler;
import civ.model.combat.SwordsmanHandler;
import civ.model.event.EventBus;
import civ.model.event.GameEvent;
import civ.model.factory.BuildingFactory;
import civ.model.factory.UnitFactory;
import civ.model.trade.TradeService;
import civ.model.trade.TradeTracker;
import civ.model.tribe.Quest;
import civ.model.tribe.QuestStatus;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeCamp;
import civ.model.tribe.TribeGuard;
import civ.model.tribe.TribePlacer;
import civ.model.tribe.TribeTurnBehaviour;
import civ.model.tribe.TribeType;
import civ.model.world.DisasterEffect;
import civ.model.world.DisasterRoller;
import civ.model.world.Season;
import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private final Battle battle;
    private final TradeTracker tradeTracker = new TradeTracker();
    private final TradeService tradeService = new TradeService();
    private final TribeTurnBehaviour tribeBehaviour = new TribeTurnBehaviour();
    private final DisasterRoller disasterRoller;
    private final List<MilitaryUnit> hostiles = new ArrayList<>();
    private final List<Tribe> tribes = new ArrayList<>();
    private final int centreCol;
    private final int centreRow;
    private final Random random;
    private final long mapSeed;

    private int turn = 1;
    private Unit selected;
    private Hex inspected;
    private boolean starving = false;
    private boolean nextDockHalfPrice = false;
    private final List<String> log = new ArrayList<>();
    private Season lastSeason = Season.SPRING;
    private DisasterEffect lastDisaster;
    private int lastBearTurn = -999;
    private boolean processingTurn = false;
    private boolean disasterBusy = false;

    private List<MilitaryUnit> pendingAttackers;
    private List<MilitaryUnit> pendingDefenders;
    private BattleReport pendingReport;
    private Tribe pendingAttackedTribe;

    public Game(long seed) {
        this.mapSeed = seed;
        this.map = new GameMap(22, 18);
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;
        this.random = new Random(seed);
        this.battle = new Battle(new Dice(random));
        this.disasterRoller = new DisasterRoller(random);

        new MapGenerator(seed).fill(map, centreCol, centreRow);
        setUpStartingPosition();
        wireWorldSystems();
    }

    /**
     * Spec order after tribes (already run in TurnEngine): season → disaster → bears.
     * Autosave registers later in save/load.
     */
    private void wireWorldSystems() {
        bus.subscribe(GameEvent.BUILDING_PLACED, payload -> {
            Building building = (Building) payload;
            if (building.getType() == BuildingType.MONUMENT) {
                empire.getHappiness().add(2);
            } else if (building.getType() == BuildingType.SETTLEMENT) {
                empire.getHappiness().add(-1);
            }
            syncGarrisonHappiness();
        });
        bus.subscribe(GameEvent.BUILDING_DESTROYED, payload -> {
            Building building = (Building) payload;
            if (building.getType() == BuildingType.MONUMENT) {
                empire.getHappiness().add(-2);
            }
            syncGarrisonHappiness();
        });
        bus.subscribe(GameEvent.TURN_ENDED, payload -> {
            Season now = Season.forTurn(turn);
            if (now != lastSeason) {
                lastSeason = now;
                bus.publish(GameEvent.SEASON_CHANGED, now);
                addLog("Season changed to " + now.getLabel() + ".");
            }
            syncGarrisonHappiness();
            disasterRoller.maybeStrike(this);
            runBearBehaviour();
        });
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
        if (ring.size() > 4) {
            addUnit(new Swordsman(ring.get(4).getCol(), ring.get(4).getRow()));
            addUnit(new Archer(ring.get(4).getCol(), ring.get(4).getRow()));
        }
        spawnHostiles();
        placeTradingPost();
        tribes.addAll(new TribePlacer(random).place(map, centreCol, centreRow));

        empire.getStock().add(ResourceType.FOOD, 999);
        empire.getStock().add(ResourceType.WOOD, 999);
        empire.getStock().add(ResourceType.STONE, 999);
        empire.getStock().add(ResourceType.IRON, 999);
    }

    public void addUnit(Unit unit) {
        empire.getUnits().add(unit);
        revealAround(unit);
        if (unit.isMilitary() && empire.countMilitary() >= empire.getMilitaryCap()) {
            empire.getHappiness().noteMilitaryCapReached();
            addLog("Military unit cap reached: −1 happiness.");
        }
        syncGarrisonHappiness();
    }

    public void removeUnit(Unit unit) {
        empire.getUnits().remove(unit);
        if (selected == unit) {
            selected = null;
        }
    }

    public void addHostile(MilitaryUnit unit) {
        hostiles.add(unit);
    }

    public List<MilitaryUnit> getHostiles() {
        return hostiles;
    }

    public List<Tribe> getTribes() {
        return tribes;
    }

    public TradeTracker getTradeTracker() {
        return tradeTracker;
    }

    public TradeService getTradeService() {
        return tradeService;
    }

    public boolean isNextDockHalfPrice() {
        return nextDockHalfPrice;
    }

    public void setNextDockHalfPrice(boolean nextDockHalfPrice) {
        this.nextDockHalfPrice = nextDockHalfPrice;
    }

    public Random getRandom() {
        return random;
    }

    public long getMapSeed() {
        return mapSeed;
    }

    public Season getSeason() {
        return Season.forTurn(turn);
    }

    public DisasterEffect getLastDisaster() {
        return lastDisaster;
    }

    public void setLastDisaster(DisasterEffect lastDisaster) {
        this.lastDisaster = lastDisaster;
    }

    public int getLastBearTurn() {
        return lastBearTurn;
    }

    public void setLastBearTurn(int lastBearTurn) {
        this.lastBearTurn = lastBearTurn;
    }

    public boolean isProcessingTurn() {
        return processingTurn;
    }

    public void setProcessingTurn(boolean processingTurn) {
        this.processingTurn = processingTurn;
    }

    public boolean isDisasterBusy() {
        return disasterBusy;
    }

    public void setDisasterBusy(boolean disasterBusy) {
        this.disasterBusy = disasterBusy;
    }

    public boolean hasPendingBattle() {
        return pendingReport != null;
    }

    /** Save is locked during battle, end-of-turn, or disaster animation. */
    public boolean isSaveLocked() {
        return processingTurn || disasterBusy || pendingReport != null;
    }

    public String saveLockReason() {
        if (pendingReport != null) {
            return "A battle is in progress.";
        }
        if (processingTurn) {
            return "End-of-turn processing.";
        }
        if (disasterBusy) {
            return "A disaster is unfolding.";
        }
        return null;
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
        Building building = hex.getBuilding();
        if (building != null) {
            text.append(". ").append(building.getType().getLabel())
                    .append(" HP ").append(building.getHp())
                    .append("/").append(building.getMaxHp());
        }
        List<MilitaryUnit> enemies = hostilesAt(hex);
        if (!enemies.isEmpty()) {
            text.append(". Hostile: ");
            for (int i = 0; i < enemies.size(); i++) {
                if (i > 0) {
                    text.append(", ");
                }
                MilitaryUnit enemy = enemies.get(i);
                text.append(enemy.getTypeName())
                        .append(" (HP ").append(enemy.getCombatHp()).append(")");
            }
        }
        Tribe tribe = tribeAt(hex);
        if (tribe != null && tribe.isDiscovered() && !tribe.isDestroyed()) {
            text.append(". Tribe ").append(tribe.getName())
                    .append(" (").append(tribe.getState().getName())
                    .append(", ").append(tribe.getRelation()).append(")");
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
        if (to.isBlocked()) {
            return Integer.MAX_VALUE;
        }
        Season season = Season.forTurn(turn);
        if (to.getTerrain().isSea()) {
            return Math.max(1, unit.getAp()) + season.waterMovePenalty();
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
        cost += season.landMovePenalty();
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
        if (target.isBlocked()) {
            return false;
        }
        if (target.getTerrain().isSea()) {
            return empire.hasTech(Tech.SEAFARING) && unit.getAp() > 0;
        }
        if (!hostilesAt(target).isEmpty()) {
            return false;
        }
        if (!canStack(unit, target)) {
            return false;
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
        if (type == BuildingType.TRADING_POST || type == BuildingType.TRIBE_CAMP
                || type == BuildingType.OUTPOST || type == BuildingType.TOWN_HALL) {
            return false;
        }
        int woodCost = type.getWoodCost();
        if (type == BuildingType.DOCK && nextDockHalfPrice) {
            woodCost = woodCost / 2;
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
        return empire.getStock().canPay(woodCost, type.getStoneCost(), type.getIronCost());
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
            syncGarrisonHappiness();
            return;
        }
        unit.spend(moveCost(unit, target));
        unit.moveTo(target);
        revealAround(unit);
        syncGarrisonHappiness();
    }

    public void build(Builder builder, BuildingType type, Hex hex) {
        if (!canBuild(builder, type, hex)) {
            return;
        }

        int woodCost = type.getWoodCost();
        if (type == BuildingType.DOCK && nextDockHalfPrice) {
            woodCost = woodCost / 2;
        }
        empire.getStock().pay(woodCost, type.getStoneCost(), type.getIronCost());
        builder.spend(type.getApCost());

        Building building = buildingFactory.create(type, hex);
        empire.getBuildings().add(building);

        if (type == BuildingType.SETTLEMENT) {
            empire.raiseUnitCap(5);
        }
        if (type == BuildingType.DOCK && nextDockHalfPrice) {
            nextDockHalfPrice = false;
            addLog("Dock built at half wood cost (coastal quest reward).");
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
        if (building != null && (building.getType() == BuildingType.TRIBE_CAMP
                || building.getType() == BuildingType.TRADING_POST)) {
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
            removeBuildingFromMap(building);
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

    public boolean hasMilitaryStable() {
        for (Building building : empire.getBuildings()) {
            if (building.getType() == BuildingType.MILITARY_STABLE) {
                return true;
            }
        }
        return false;
    }

    public Hex spawnHexFor(UnitBlueprint blueprint) {
        Hex home = empire.getTownHall().getHex();
        Unit probe = blueprint.create(home.getCol(), home.getRow());
        for (Hex hex : map.withinRange(home, 2)) {
            if (!hex.isOwned() || !hex.getTerrain().isLand()) {
                continue;
            }
            if (!hostilesAt(hex).isEmpty()) {
                continue;
            }
            if (canStack(probe, hex)) {
                return hex;
            }
        }
        return home;
    }

    public boolean canStack(Unit unit, Hex target) {
        if (!(unit instanceof MilitaryUnit) || ((MilitaryUnit) unit).isHostile()) {
            return true;
        }
        int swords = 0;
        int archers = 0;
        int cavalry = 0;
        for (Unit other : unitsAt(target)) {
            if (other == unit) {
                continue;
            }
            if (other instanceof Swordsman) {
                swords++;
            } else if (other instanceof Archer) {
                archers++;
            } else if (other instanceof Cavalry) {
                cavalry++;
            }
        }
        if (unit instanceof Swordsman && swords >= 2) {
            return false;
        }
        if (unit instanceof Archer && archers >= 2) {
            return false;
        }
        if (unit instanceof Cavalry && cavalry >= 1) {
            return false;
        }
        return true;
    }

    public List<MilitaryUnit> hostilesAt(Hex hex) {
        List<MilitaryUnit> result = new ArrayList<>();
        for (MilitaryUnit unit : hostiles) {
            if (unit.isOn(hex)) {
                result.add(unit);
            }
        }
        for (Tribe tribe : tribes) {
            if (tribe.isDestroyed()) {
                continue;
            }
            for (TribeGuard guard : tribe.getGuards()) {
                if (guard.isOn(hex)) {
                    result.add(guard);
                }
            }
        }
        return result;
    }

    public List<MilitaryUnit> playerMilitaryNear(Hex centre, int radius) {
        List<MilitaryUnit> result = new ArrayList<>();
        for (Unit unit : empire.getUnits()) {
            if (!(unit instanceof MilitaryUnit)) {
                continue;
            }
            MilitaryUnit military = (MilitaryUnit) unit;
            if (military.isHostile()) {
                continue;
            }
            int d = HexGeometry.distance(
                    centre.getCol(), centre.getRow(), unit.getCol(), unit.getRow());
            if (d <= radius) {
                result.add(military);
            }
        }
        return result;
    }

    public Tribe tribeAt(Hex hex) {
        if (hex == null) {
            return null;
        }
        for (Tribe tribe : tribes) {
            if (!tribe.isDestroyed() && tribe.getCampHex() == hex) {
                return tribe;
            }
        }
        Building building = hex.getBuilding();
        if (building instanceof TribeCamp) {
            return ((TribeCamp) building).getTribe();
        }
        return null;
    }

    public Tribe tribeOfGuard(MilitaryUnit unit) {
        if (unit instanceof TribeGuard) {
            return ((TribeGuard) unit).getTribe();
        }
        return null;
    }

    public List<MilitaryUnit> attackersOn(Hex from, Hex to) {
        List<MilitaryUnit> result = new ArrayList<>();
        if (from == null || to == null) {
            return result;
        }
        int distance = HexGeometry.distance(
                from.getCol(), from.getRow(), to.getCol(), to.getRow());
        for (Unit unit : unitsAt(from)) {
            if (!(unit instanceof MilitaryUnit)) {
                continue;
            }
            MilitaryUnit military = (MilitaryUnit) unit;
            if (military.isHostile() || military.getAp() < 1) {
                continue;
            }
            if (distance == 2 && !(military instanceof Archer)) {
                continue;
            }
            result.add(military);
        }
        return result;
    }

    public boolean canAttack(Hex from, Hex to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        if (!to.isDiscovered()) {
            return false;
        }
        int distance = HexGeometry.distance(
                from.getCol(), from.getRow(), to.getCol(), to.getRow());
        if (attackersOn(from, to).isEmpty()) {
            return false;
        }
        if (distance == 1) {
            return !hostilesAt(to).isEmpty() || canStrikeStructure(to) || canCapture(to)
                    || canStrikeTribeCamp(to);
        }
        if (distance == 2) {
            return !hostilesAt(to).isEmpty() || canStrikeStructure(to) || canStrikeTribeCamp(to);
        }
        return false;
    }

    public boolean canAttackWall(Hex from, Hex to) {
        if (from == null || to == null) {
            return false;
        }
        if (HexGeometry.distance(from.getCol(), from.getRow(), to.getCol(), to.getRow()) != 1) {
            return false;
        }
        if (attackersOn(from, to).isEmpty()) {
            return false;
        }
        Edge edge = map.getEdges().find(from, to);
        return edge != null && edge.hasWall();
    }

    public boolean isDiceAttack(Hex from, Hex to) {
        return canAttack(from, to) && !hostilesAt(to).isEmpty();
    }

    public BattleReport beginDiceAttack(Hex from, Hex to) {
        if (!isDiceAttack(from, to)) {
            return null;
        }
        List<MilitaryUnit> attackers = attackersOn(from, to);
        List<MilitaryUnit> defenders = hostilesAt(to);
        for (MilitaryUnit unit : attackers) {
            unit.spend(1);
        }
        noteAggressionFromAttack(to, defenders);
        int distance = HexGeometry.distance(
                from.getCol(), from.getRow(), to.getCol(), to.getRow());
        pendingAttackers = attackers;
        pendingDefenders = defenders;
        pendingAttackedTribe = tribeAt(to);
        if (pendingAttackedTribe == null && !defenders.isEmpty()) {
            pendingAttackedTribe = tribeOfGuard(defenders.get(0));
        }
        pendingReport = battle.resolve(
                attackerDiceCount(attackers, distance),
                defenderDice(defenders),
                wallBonus(from, to));
        return pendingReport;
    }

    public void applyPendingDiceAttack() {
        if (pendingReport == null) {
            return;
        }
        DamageHandler chain = hitChain();
        chain.handle(pendingDefenders, pendingReport.getHitsOnDefender());
        chain.handle(pendingAttackers, pendingReport.getHitsOnAttacker());
        Hex killHex = pendingDefenders.isEmpty()
                ? null
                : map.get(pendingDefenders.get(0).getCol(), pendingDefenders.get(0).getRow());
        for (MilitaryUnit unit : new ArrayList<>(pendingDefenders)) {
            if (unit.isDead()) {
                if (pendingAttackedTribe != null && killHex != null) {
                    noteQuestKill(pendingAttackedTribe, killHex);
                }
                buryUnit(unit);
            }
        }
        for (MilitaryUnit unit : new ArrayList<>(pendingAttackers)) {
            buryIfDead(unit);
        }
        addLog("Battle: " + pendingReport.getHitsOnDefender() + " hit(s) on the defender, "
                + pendingReport.getHitsOnAttacker() + " hit(s) on the attacker.");
        pendingAttackers = null;
        pendingDefenders = null;
        pendingReport = null;
        pendingAttackedTribe = null;
    }

    public void performQuietAttack(Hex from, Hex to) {
        if (!canAttack(from, to) || isDiceAttack(from, to)) {
            return;
        }
        List<MilitaryUnit> attackers = attackersOn(from, to);
        for (MilitaryUnit unit : attackers) {
            unit.spend(1);
        }
        if (canCapture(to)) {
            to.setOwned(true);
            to.setDiscovered(true);
            addLog("The hex was captured without a fight.");
            return;
        }
        if (canStrikeTribeCamp(to)) {
            Tribe tribe = tribeAt(to);
            noteAggressionOnTribe(tribe);
            strikeBuilding(to.getBuilding(), battle.damageToStructure(attackers));
            return;
        }
        Building building = to.getBuilding();
        if (building != null && !empire.getBuildings().contains(building)) {
            strikeBuilding(building, battle.damageToStructure(attackers));
        }
    }

    public void attackWall(Hex from, Hex to) {
        if (!canAttackWall(from, to)) {
            return;
        }
        List<MilitaryUnit> attackers = attackersOn(from, to);
        for (MilitaryUnit unit : attackers) {
            unit.spend(1);
        }
        Edge edge = map.getEdges().find(from, to);
        Wall wall = edge.getWall();
        int damage = battle.damageToStructure(attackers);
        wall.damage(damage);
        addLog("The wall took " + damage + " damage ("
                + wall.getHp() + "/" + wall.getMaxHp() + ").");
        if (wall.isDestroyed()) {
            edge.setWall(null);
            addLog("The wall was destroyed.");
        }
    }

    private void spawnHostiles() {
        Hex barbHex = null;
        Hex animalHex = null;
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || hex.isOwned()) {
                    continue;
                }
                int distance = HexGeometry.distance(centreCol, centreRow, col, row);
                if (distance != 2) {
                    continue;
                }
                if (barbHex == null) {
                    barbHex = hex;
                } else if (animalHex == null) {
                    animalHex = hex;
                }
            }
        }
        if (barbHex != null) {
            barbHex.setDiscovered(true);
            addHostile(new Barbarian(barbHex.getCol(), barbHex.getRow()));
            addHostile(new Barbarian(barbHex.getCol(), barbHex.getRow()));
            addLog("Raiders were spotted nearby.");
        }
        if (animalHex != null) {
            animalHex.setDiscovered(true);
            addHostile(new WildAnimal(animalHex.getCol(), animalHex.getRow()));
        }
    }

    private boolean canCapture(Hex to) {
        if (to.isOwned() || to.getBuilding() != null) {
            return false;
        }
        if (!to.getTerrain().isLand()) {
            return false;
        }
        return hostilesAt(to).isEmpty();
    }

    private boolean canStrikeStructure(Hex to) {
        Building building = to.getBuilding();
        if (building == null || empire.getBuildings().contains(building)) {
            return false;
        }
        if (building instanceof TribeCamp) {
            return false;
        }
        return hostilesAt(to).isEmpty();
    }

    private boolean canStrikeTribeCamp(Hex to) {
        Tribe tribe = tribeAt(to);
        if (tribe == null || tribe.isDestroyed()) {
            return false;
        }
        return hostilesAt(to).isEmpty();
    }

    private int attackerDiceCount(List<MilitaryUnit> attackers, int distance) {
        if (distance == 2) {
            return attackers.isEmpty() ? 0 : 1;
        }
        boolean sword = false;
        boolean archer = false;
        boolean cavalry = false;
        for (MilitaryUnit unit : attackers) {
            if (unit instanceof Swordsman) {
                sword = true;
            } else if (unit instanceof Archer) {
                archer = true;
            } else if (unit instanceof Cavalry) {
                cavalry = true;
            }
        }
        int count = 0;
        if (sword) {
            count++;
        }
        if (archer) {
            count++;
        }
        if (cavalry) {
            count++;
        }
        return count;
    }

    private int defenderDice(List<MilitaryUnit> defenders) {
        boolean barbarian = false;
        boolean animal = false;
        boolean tribeGuard = false;
        for (MilitaryUnit unit : defenders) {
            if (unit instanceof Barbarian) {
                barbarian = true;
            }
            if (unit instanceof WildAnimal || unit instanceof Bear) {
                animal = true;
            }
            if (unit instanceof TribeGuard) {
                tribeGuard = true;
            }
        }
        if (barbarian || tribeGuard) {
            return 2;
        }
        if (animal) {
            return 1;
        }
        return 2;
    }

    private int wallBonus(Hex from, Hex to) {
        if (HexGeometry.distance(from.getCol(), from.getRow(), to.getCol(), to.getRow()) != 1) {
            return 0;
        }
        Edge edge = map.getEdges().find(from, to);
        return edge != null && edge.hasWall() ? 2 : 0;
    }

    private DamageHandler hitChain() {
        DamageHandler chain = new HostileHandler();
        chain.setNext(new SwordsmanHandler())
                .setNext(new ArcherHandler())
                .setNext(new CavalryHandler());
        return chain;
    }

    public void buryUnit(MilitaryUnit unit) {
        if (unit instanceof TribeGuard) {
            TribeGuard guard = (TribeGuard) unit;
            guard.getTribe().getGuards().remove(guard);
        } else if (hostiles.contains(unit) || unit.isHostile()) {
            hostiles.remove(unit);
        } else {
            removeUnit(unit);
        }
        bus.publish(GameEvent.UNIT_KILLED, unit);
        addLog(unit.getTypeName() + " was killed.");
    }

    private void buryIfDead(MilitaryUnit unit) {
        if (unit.isDead()) {
            buryUnit(unit);
        }
    }

    private void strikeBuilding(Building building, int damage) {
        building.damage(damage);
        addLog("The " + building.getType().getLabel() + " took " + damage + " damage ("
                + building.getHp() + "/" + building.getMaxHp() + ").");
        if (!building.isDestroyed()) {
            return;
        }
        if (building instanceof TownHall) {
            addLog("The Town Hall has fallen to 0 HP!");
            return;
        }
        if (building instanceof TribeCamp) {
            conquerTribe(((TribeCamp) building).getTribe());
            return;
        }
        removeBuildingFromMap(building);
        addLog("The " + building.getType().getLabel() + " was destroyed.");
    }

    private void removeBuildingFromMap(Building building) {
        if (building instanceof ProductionBuilding) {
            ((ProductionBuilding) building).releaseAllWorkers();
        }
        building.getHex().setBuilding(null);
        empire.getBuildings().remove(building);
        bus.publish(GameEvent.BUILDING_DESTROYED, building);
    }

    private void placeTradingPost() {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || hex.isOwned()) {
                    continue;
                }
                if (hex.getBuilding() != null) {
                    continue;
                }
                int distance = HexGeometry.distance(centreCol, centreRow, col, row);
                if (distance < 4 || distance > 8) {
                    continue;
                }
                if (hex.getTerrain() == Terrain.PLAINS || hex.getTerrain() == Terrain.GRASSLAND) {
                    TradingPost post = new TradingPost(hex);
                    hex.setBuilding(post);
                    return;
                }
            }
        }
    }

    private void noteAggressionFromAttack(Hex to, List<MilitaryUnit> defenders) {
        Tribe tribe = tribeAt(to);
        if (tribe == null && !defenders.isEmpty()) {
            tribe = tribeOfGuard(defenders.get(0));
        }
        if (tribe != null) {
            noteAggressionOnTribe(tribe);
        }
    }

    private void noteAggressionOnTribe(Tribe tribe) {
        if (tribe == null || tribe.isDestroyed()) {
            return;
        }
        boolean wasAllied = tribe.isAllied();
        boolean wasFriendly = tribe.getRelation() >= 20 && !wasAllied;
        if (wasAllied) {
            empire.addHappinessBonus(-15);
            addLog("Attacking an ally: −15 happiness.");
        } else if (wasFriendly) {
            empire.addHappinessBonus(-5);
            addLog("Attacking a friendly tribe: −5 happiness.");
        }
        if (tribe.getQuest() != null) {
            QuestStatus status = tribe.getQuest().getStatus();
            if (status == QuestStatus.ACTIVE || status == QuestStatus.READY
                    || status == QuestStatus.AVAILABLE) {
                tribe.getQuest().cancel();
            }
        }
        tribe.setAllied(false);
        tribe.setRelationAbsolute(-100, bus);
        addLog("War with " + tribe.getName() + "!");
    }

    private void noteQuestKill(Tribe nearTribe, Hex killHex) {
        for (Tribe tribe : tribes) {
            if (tribe.isDestroyed() || tribe.getQuest() == null) {
                continue;
            }
            tribe.getQuest().noteKillNearCamp(tribe, killHex);
            tribe.getQuest().refreshReady(this, tribe);
        }
    }

    private void conquerTribe(Tribe tribe) {
        Hex camp = tribe.getCampHex();
        grantLoot(tribe);
        for (TribeGuard guard : new ArrayList<>(tribe.getGuards())) {
            tribe.getGuards().remove(guard);
        }
        tribe.markDestroyed();
        camp.setBuilding(null);
        Outpost outpost = new Outpost(camp);
        camp.setBuilding(outpost);
        empire.getBuildings().add(outpost);
        camp.setOwned(true);
        camp.setDiscovered(true);
        for (Hex neighbour : map.neighbours(camp)) {
            if (neighbour.getTerrain().isPassable() && neighbour.getTerrain().isLand()) {
                neighbour.setOwned(true);
                neighbour.setDiscovered(true);
            }
        }
        bus.publish(GameEvent.BUILDING_DESTROYED, tribe.getCamp());
        addLog(tribe.getName() + " was defeated. The camp is now an Outpost.");
    }

    private void grantLoot(Tribe tribe) {
        switch (tribe.getType()) {
            case FARMER:
                empire.getStock().add(ResourceType.FOOD, 40);
                break;
            case MOUNTAIN:
                empire.getStock().add(ResourceType.STONE, 25);
                empire.getStock().add(ResourceType.IRON, 15);
                break;
            case TRADER:
                empire.getStock().add(ResourceType.WOOD, 20);
                empire.getStock().add(ResourceType.STONE, 20);
                empire.getStock().add(ResourceType.FOOD, 20);
                break;
            case WARRIOR:
                empire.getStock().add(ResourceType.IRON, 30);
                break;
            case COASTAL:
                empire.getStock().add(ResourceType.FOOD, 25);
                empire.getStock().add(ResourceType.WOOD, 25);
                break;
            default:
                break;
        }
        addLog("Loot claimed from " + tribe.getName() + ".");
    }

    public void destroyBuilding(Building building, String reason) {
        if (building == null || building instanceof TownHall) {
            return;
        }
        if (building instanceof TribeCamp) {
            conquerTribe(((TribeCamp) building).getTribe());
            return;
        }
        removeBuildingFromMap(building);
        addLog("The " + building.getType().getLabel() + " was " + reason + ".");
    }

    public void killUnit(Unit unit, String reason) {
        if (unit instanceof MilitaryUnit) {
            buryUnit((MilitaryUnit) unit);
            return;
        }
        if (unit instanceof Worker) {
            Worker worker = (Worker) unit;
            if (worker.getStation() != null) {
                worker.getStation().removeWorker(worker);
            }
        }
        removeUnit(unit);
        addLog(unit.getTypeName() + " was " + reason + ".");
        syncGarrisonHappiness();
    }

    /**
     * Season + happiness modifiers applied on top of a building's raw output.
     */
    public int adjustProduction(Building building, int raw) {
        if (building.isPaused(turn)) {
            return 0;
        }
        int amount = raw;
        if (building instanceof ProductionBuilding) {
            int penalty = empire.getHappiness().workerOutputPenalty();
            if (penalty > 0) {
                amount = Math.max(0, amount
                        - ((ProductionBuilding) building).getWorkers().size() * penalty);
            }
        }
        BuildingType type = building.getType();
        if (type == BuildingType.FARM || type == BuildingType.STABLE) {
            amount = Math.max(0, amount + Season.forTurn(turn).farmFoodBonus());
        }
        amount = (int) Math.floor(amount * empire.getHappiness().productionMultiplier());
        return amount;
    }

    public void syncGarrisonHappiness() {
        TownHall hall = empire.getTownHall();
        if (hall == null) {
            empire.getHappiness().setGarrisonBonus(0);
            return;
        }
        int garrison = 0;
        for (Unit unit : unitsAt(hall.getHex())) {
            if (unit.isMilitary()) {
                garrison++;
            }
        }
        empire.getHappiness().setGarrisonBonus(garrison);
    }

    /** Bears act: hunt civilians first, dice-fight military, leave when no prey within 3. */
    public void runBearBehaviour() {
        for (MilitaryUnit hostile : new ArrayList<>(hostiles)) {
            if (!(hostile instanceof Bear)) {
                continue;
            }
            Bear bear = (Bear) hostile;
            Hex here = hexOf(bear);
            if (here == null) {
                continue;
            }

            Unit civilian = nearestCivilian(here, 3);
            MilitaryUnit military = nearestPlayerMilitary(here, 3);
            if (civilian == null && military == null) {
                hostiles.remove(bear);
                addLog("The bears left the map.");
                continue;
            }

            Unit prey = civilian != null ? civilian : military;
            Hex preyHex = hexOf(prey);
            int distance = HexGeometry.distance(here.getCol(), here.getRow(),
                    preyHex.getCol(), preyHex.getRow());
            if (distance > 1) {
                Hex step = stepToward(here, preyHex);
                if (step != null && !step.isBlocked() && step.getTerrain().isLand()
                        && hostilesAt(step).isEmpty() && bear.canSpend(1)) {
                    bear.spend(1);
                    bear.moveTo(step);
                    step.setDiscovered(true);
                }
                continue;
            }

            if (prey instanceof MilitaryUnit) {
                List<MilitaryUnit> attackers = new ArrayList<>();
                attackers.add(bear);
                List<MilitaryUnit> defenders = new ArrayList<>();
                defenders.add((MilitaryUnit) prey);
                BattleReport report = battle.resolve(1, 2, 0);
                DamageHandler chain = hitChain();
                chain.handle(defenders, report.getHitsOnDefender());
                chain.handle(attackers, report.getHitsOnAttacker());
                for (MilitaryUnit unit : new ArrayList<>(defenders)) {
                    buryIfDead(unit);
                }
                for (MilitaryUnit unit : new ArrayList<>(attackers)) {
                    buryIfDead(unit);
                }
                addLog("A bear attacked your " + prey.getTypeName() + "!");
            } else {
                prey.damageBody(25);
                prey.emptyAp();
                if (prey.isBodyDead()) {
                    killUnit(prey, "mauled by a bear");
                } else {
                    addLog("A bear mauled your " + prey.getTypeName() + "!");
                }
            }
        }
    }

    private Unit nearestCivilian(Hex from, int radius) {
        Unit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Unit unit : empire.getUnits()) {
            if (unit.isMilitary() || unit.isBusy()) {
                continue;
            }
            int dist = HexGeometry.distance(from.getCol(), from.getRow(),
                    unit.getCol(), unit.getRow());
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                best = unit;
            }
        }
        return best;
    }

    private MilitaryUnit nearestPlayerMilitary(Hex from, int radius) {
        MilitaryUnit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Unit unit : empire.getUnits()) {
            if (!(unit instanceof MilitaryUnit)) {
                continue;
            }
            int dist = HexGeometry.distance(from.getCol(), from.getRow(),
                    unit.getCol(), unit.getRow());
            if (dist <= radius && dist < bestDist) {
                bestDist = dist;
                best = (MilitaryUnit) unit;
            }
        }
        return best;
    }

    private Hex stepToward(Hex from, Hex to) {
        Hex best = null;
        int bestDist = HexGeometry.distance(from.getCol(), from.getRow(), to.getCol(), to.getRow());
        for (Hex neighbour : map.neighbours(from)) {
            int dist = HexGeometry.distance(neighbour.getCol(), neighbour.getRow(),
                    to.getCol(), to.getRow());
            if (dist < bestDist) {
                bestDist = dist;
                best = neighbour;
            }
        }
        return best;
    }

    public void runTribeTurns() {
        for (Tribe tribe : tribes) {
            tribeBehaviour.act(this, tribe);
        }
    }

    public void gift(Tribe tribe, ResourceType type, int amount) {
        if (tribe == null || !tribe.isDiscovered() || tribe.isDestroyed()) {
            return;
        }
        if (!tribe.getState().allowsGift()) {
            return;
        }
        if (!empire.getStock().canPay(type, amount)) {
            return;
        }
        int gain = tribe.giftRelationGain(type, amount);
        if (gain <= 0) {
            return;
        }
        empire.getStock().add(type, -amount);
        tribe.changeRelation(gain, bus);
        addLog("Gifted " + amount + " " + type.getLabel() + " to " + tribe.getName()
                + " (+" + gain + " relation).");
    }

    public void declareWar(Tribe tribe) {
        if (tribe == null || !tribe.getState().allowsWarDeclaration()) {
            return;
        }
        noteAggressionOnTribe(tribe);
    }

    public boolean canAskPeace(Tribe tribe) {
        return tribe != null
                && tribe.getState().allowsPeaceRequest()
                && empire.getStock().canPay(ResourceType.FOOD, 30)
                && empire.getStock().canPay(ResourceType.WOOD, 30)
                && empire.getStock().canPay(ResourceType.IRON, 30);
    }

    public void askPeace(Tribe tribe) {
        if (!canAskPeace(tribe)) {
            return;
        }
        empire.getStock().add(ResourceType.FOOD, -30);
        empire.getStock().add(ResourceType.WOOD, -30);
        empire.getStock().add(ResourceType.IRON, -30);
        tribe.setRelationAbsolute(-10, bus);
        addLog("Peace with " + tribe.getName() + ". Relation is now −10.");
    }

    public void askAlliance(Tribe tribe) {
        if (!civ.model.tribe.AllianceRules.canAlly(tribe, tribes)) {
            return;
        }
        tribe.setAllied(true);
        if (tribe.getRelation() < 70) {
            tribe.setRelationAbsolute(70, bus);
        }
        addLog("Allied with " + tribe.getName() + ".");
        bus.publish(GameEvent.RELATION_CHANGED, tribe);
    }

    public void takeQuest(Tribe tribe) {
        if (tribe == null || !tribe.getState().allowsQuest() || tribe.isQuestBlocked()) {
            return;
        }
        Quest quest = tribe.getQuest();
        if (quest == null || quest.getStatus() != QuestStatus.AVAILABLE) {
            return;
        }
        quest.accept();
        addLog("Accepted quest from " + tribe.getName() + ": " + quest.getTitle());
    }

    public void deliverQuest(Tribe tribe) {
        if (tribe == null || tribe.getQuest() == null) {
            return;
        }
        tribe.getQuest().refreshReady(this, tribe);
        tribe.getQuest().deliver(this, tribe);
    }

    public boolean hasBazaar() {
        for (Building building : empire.getBuildings()) {
            if (building.getType() == BuildingType.BAZAAR) {
                return true;
            }
        }
        return false;
    }

    public TradingPost findOwnedTradingPost() {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null || !hex.isOwned()) {
                    continue;
                }
                if (hex.getBuilding() instanceof TradingPost) {
                    return (TradingPost) hex.getBuilding();
                }
            }
        }
        return null;
    }

    public void revealAround(Unit unit) {
        Hex centre = hexOf(unit);
        if (centre == null) {
            return;
        }
        for (Hex hex : map.withinRange(centre, unit.getVisionRadius())) {
            hex.setDiscovered(true);
            Tribe tribe = tribeAt(hex);
            if (tribe != null && !tribe.isDiscovered()) {
                tribe.discover();
                addLog("Discovered the " + tribe.getName()
                        + " (" + tribe.getType().getLabel() + ").");
            }
        }
    }
}
