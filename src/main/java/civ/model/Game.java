package civ.model;

import civ.model.command.CraftItemCommand;
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
import civ.model.diplomacy.DiplomacyTable;
import civ.model.event.EventBus;
import civ.model.event.GameEvent;
import civ.model.factory.BuildingFactory;
import civ.model.factory.UnitFactory;
import civ.model.item.Item;
import civ.model.item.ItemFactory;
import civ.model.item.ItemType;
import civ.model.trade.TradeOffers;
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
import civ.model.map.MapPreset;
import civ.model.world.Season;
import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game extends Entity {

    private final GameMap map;
    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private final EventBus bus = new EventBus();
    private final UnitFactory unitFactory = new UnitFactory(this);
    private final BuildingFactory buildingFactory = new BuildingFactory(bus);
    private Battle battle;
    private final TradeTracker tradeTracker = new TradeTracker();
    private final TradeService tradeService = new TradeService();
    private final DiplomacyTable diplomacy = new DiplomacyTable();
    private final TradeOffers tradeOffers = new TradeOffers();
    private final Map<Long, List<BattleReport>> pendingReports = new HashMap<>();
    private final TribeTurnBehaviour tribeBehaviour = new TribeTurnBehaviour();
    private DisasterRoller disasterRoller;
    private final List<MilitaryUnit> hostiles = new ArrayList<>();
    private final List<Tribe> tribes = new ArrayList<>();
    private final int centreCol;
    private final int centreRow;
    private Random random;
    private final long mapSeed;

    private int turn = 1;
    private Unit selected;
    private Hex inspected;
    private boolean nextDockHalfPrice = false;
    private final List<String> log = new ArrayList<>();
    private Season lastSeason = Season.SPRING;
    private DisasterEffect lastDisaster;
    private int lastBearTurn = -999;
    private boolean processingTurn = false;
    private boolean disasterBusy = false;
    /** When set (network client), fog/HUD use this player instead of the current actor. */
    private Long viewpointPlayerId;

    private List<MilitaryUnit> pendingAttackers;
    private List<MilitaryUnit> pendingDefenders;
    private BattleReport pendingReport;
    private Tribe pendingAttackedTribe;

    /** Late-game cost to found a Town Hall outside your borders. */
    public static final int FOUND_WOOD = 200;
    public static final int FOUND_STONE = 150;
    public static final int FOUND_IRON = 80;
    public static final int FOUND_FOOD = 100;

    public Game(long seed) {
        this(seed, false);
    }

    /** Single-player: procedural map, one BLUE "Player 1". {@code blank} for save loads. */
    public Game(long seed, boolean blank) {
        this.mapSeed = seed;
        this.map = new GameMap(22, 18);
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;
        this.random = new Random(seed);
        this.battle = new Battle(new Dice(random));
        this.disasterRoller = new DisasterRoller(random);

        Player player = new Player("Player 1", PlayerColour.BLUE, map);
        players.add(player);

        new MapGenerator(seed).fill(map, centreCol, centreRow);
        if (!blank) {
            setUpStartingPosition(player, centreCol, centreRow);
            spawnHostilesNear(centreCol, centreRow);
            placeTradingPostNear(centreCol, centreRow);
            tribes.addAll(new TribePlacer(random).place(this, centreCol, centreRow));
        }
        wireWorldSystems();
    }

    /** Multiplayer hot-seat / designed-map game. */
    public Game(long seed, List<String> playerNames, MapPreset preset) {
        this.mapSeed = seed;
        this.map = preset.build();
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;
        this.random = new Random(seed);
        this.battle = new Battle(new Dice(random));
        this.disasterRoller = new DisasterRoller(random);

        int count = Math.min(playerNames.size(), preset.getMaxPlayers());
        for (int i = 0; i < count; i++) {
            Player player = new Player(playerNames.get(i), PlayerColour.forIndex(i), map);
            players.add(player);
            int[] spawn = preset.spawnFor(i);
            setUpStartingPosition(player, spawn[0], spawn[1]);
        }

        int[] first = preset.spawnFor(0);
        spawnHostilesNear(first[0], first[1]);
        placeTradingPostNear(centreCol, centreRow);
        tribes.addAll(new TribePlacer(random).place(this, centreCol, centreRow));
        wireWorldSystems();
    }

    /**
     * Empty designed-map shell for a network client. Players keep server ids;
     * units/buildings/fog come exclusively from {@code GameStateDto} snapshots.
     */
    public Game(MapPreset preset, List<Player> seatedPlayers) {
        this.mapSeed = 0L;
        this.map = preset.build();
        this.centreCol = map.getCols() / 2;
        this.centreRow = map.getRows() / 2;
        this.random = new Random(0L);
        this.battle = new Battle(new Dice(random));
        this.disasterRoller = new DisasterRoller(random);
        players.addAll(seatedPlayers);
    }

    public void setTurn(int turn) {
        this.turn = Math.max(1, turn);
        this.lastSeason = Season.forTurn(this.turn);
    }

    public void restoreRandomFromBase64(String base64) throws java.io.IOException {
        if (base64 == null || base64.isEmpty()) {
            return;
        }
        byte[] raw = java.util.Base64.getDecoder().decode(base64);
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(raw))) {
            Random loaded = (Random) in.readObject();
            this.random = loaded;
            this.battle = new Battle(new Dice(random));
            this.disasterRoller = new DisasterRoller(random);
        } catch (ClassNotFoundException ex) {
            throw new java.io.IOException("Could not restore random state.", ex);
        }
    }

    public void clearForLoad() {
        for (Player player : players) {
            player.getEmpire().getUnits().clear();
            player.getEmpire().getBuildings().clear();
            player.getEmpire().setTownHall(null);
        }
        hostiles.clear();
        tribes.clear();
        selected = null;
        inspected = null;
        pendingAttackers = null;
        pendingDefenders = null;
        pendingReport = null;
        pendingAttackedTribe = null;
        lastDisaster = null;
        log.clear();
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex != null) {
                    hex.setBuilding(null);
                    hex.setReserved(false);
                }
            }
        }
    }

    /**
     * Spec order after tribes (already run in TurnEngine): season → disaster → bears.
     * Autosave registers later in save/load.
     */
    private void wireWorldSystems() {
        bus.subscribe(GameEvent.BUILDING_PLACED, payload -> {
            Building building = (Building) payload;
            Empire ownerEmpire = getEmpire(building.getOwnerId());
            if (ownerEmpire == null) {
                ownerEmpire = getEmpire();
            }
            if (building.getType() == BuildingType.MONUMENT) {
                ownerEmpire.getHappiness().add(2);
            } else if (building.getType() == BuildingType.SETTLEMENT) {
                ownerEmpire.getHappiness().add(-1);
            }
            syncGarrisonHappiness(ownerEmpire);
        });
        bus.subscribe(GameEvent.BUILDING_DESTROYED, payload -> {
            Building building = (Building) payload;
            Empire ownerEmpire = getEmpire(building.getOwnerId());
            if (ownerEmpire == null) {
                ownerEmpire = getEmpire();
            }
            if (building.getType() == BuildingType.MONUMENT) {
                ownerEmpire.getHappiness().add(-2);
            }
            syncGarrisonHappiness(ownerEmpire);
        });
        bus.subscribe(GameEvent.TURN_ENDED, payload -> {
            Season now = Season.forTurn(turn);
            if (now != lastSeason) {
                lastSeason = now;
                bus.publish(GameEvent.SEASON_CHANGED, now);
                addLog("Season changed to " + now.getLabel() + ".");
            }
            for (Player player : players) {
                if (player.isAlive()) {
                    syncGarrisonHappiness(player.getEmpire());
                }
            }
            disasterRoller.maybeStrike(this);
            runBearBehaviour();
        });
    }

    private void setUpStartingPosition(Player player, int spawnCol, int spawnRow) {
        Hex centre = map.get(spawnCol, spawnRow);
        Empire empire = player.getEmpire();

        TownHall townHall = new TownHall(centre);
        townHall.setOwnerId(player.getId());
        centre.setBuilding(townHall);
        empire.setTownHall(townHall);

        claimFor(player, centre);
        List<Hex> ring = map.neighbours(centre);
        for (Hex neighbour : ring) {
            claimFor(player, neighbour);
        }

        addUnitFor(player, new Explorer(spawnCol, spawnRow));
        addUnitFor(player, new Builder(ring.get(0).getCol(), ring.get(0).getRow()));
        addUnitFor(player, new Builder(ring.get(1).getCol(), ring.get(1).getRow()));
        addUnitFor(player, new Worker(ring.get(2).getCol(), ring.get(2).getRow()));
        addUnitFor(player, new Worker(ring.get(3).getCol(), ring.get(3).getRow()));
        if (ring.size() > 4) {
            addUnitFor(player, new Swordsman(ring.get(4).getCol(), ring.get(4).getRow()));
            addUnitFor(player, new Archer(ring.get(4).getCol(), ring.get(4).getRow()));
        }

        empire.getStock().add(ResourceType.FOOD, 80);
        empire.getStock().add(ResourceType.WOOD, 60);
        empire.getStock().add(ResourceType.STONE, 30);
        empire.getStock().add(ResourceType.IRON, 10);
    }

    public void addUnit(Unit unit) {
        addUnitFor(getCurrentPlayer(), unit);
    }

    public void addUnitFor(Player player, Unit unit) {
        unit.setOwnerId(player.getId());
        Empire empire = player.getEmpire();
        empire.getUnits().add(unit);
        revealAround(player, unit);
        if (unit.isMilitary() && empire.countMilitary() >= empire.getMilitaryCap()) {
            empire.getHappiness().noteMilitaryCapReached();
            addLog("Military unit cap reached: −1 happiness.");
        }
        syncGarrisonHappiness(empire);
    }

    public void removeUnit(Unit unit) {
        Player owner = getPlayer(unit.getOwnerId());
        if (owner != null) {
            owner.getEmpire().getUnits().remove(unit);
        } else {
            for (Player player : players) {
                player.getEmpire().getUnits().remove(unit);
            }
        }
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

    /** Compat: current player's empire. */
    public Empire getEmpire() {
        return getCurrentPlayer().getEmpire();
    }

    public Empire getEmpire(Player player) {
        return player == null ? null : player.getEmpire();
    }

    public Empire getEmpire(long playerId) {
        Player player = getPlayer(playerId);
        return player == null ? null : player.getEmpire();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int index) {
        currentPlayerIndex = Math.max(0, Math.min(index, players.size() - 1));
    }

    public Player getPlayer(long id) {
        for (Player player : players) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    public DiplomacyTable getDiplomacy() {
        return diplomacy;
    }

    public TradeOffers getTradeOffers() {
        return tradeOffers;
    }

    public void queueReportFor(Player player, BattleReport report) {
        if (player == null || report == null) {
            return;
        }
        pendingReports.computeIfAbsent(player.getId(), key -> new ArrayList<>()).add(report);
    }

    public List<BattleReport> takeReportsFor(Player player) {
        if (player == null) {
            return List.of();
        }
        List<BattleReport> reports = pendingReports.remove(player.getId());
        return reports == null ? List.of() : reports;
    }

    public boolean isTurnOf(Player player) {
        return player != null && getCurrentPlayer().getId() == player.getId();
    }

    public void setViewpointPlayerId(Long viewpointPlayerId) {
        this.viewpointPlayerId = viewpointPlayerId;
    }

    public Long getViewpointPlayerId() {
        return viewpointPlayerId;
    }

    /** Offline/hot-seat: same as current player. Network client: always "you". */
    public Player getViewpointPlayer() {
        if (viewpointPlayerId != null) {
            Player viewer = getPlayer(viewpointPlayerId);
            if (viewer != null) {
                return viewer;
            }
        }
        return getCurrentPlayer();
    }

    public Unit findUnit(long unitId) {
        for (Unit unit : getAllUnits()) {
            if (unit.getId() == unitId) {
                return unit;
            }
        }
        for (MilitaryUnit hostile : hostiles) {
            if (hostile.getId() == unitId) {
                return hostile;
            }
        }
        return null;
    }

    public Building findBuilding(long buildingId) {
        for (Player player : players) {
            for (Building building : player.getEmpire().getBuildings()) {
                if (building.getId() == buildingId) {
                    return building;
                }
            }
        }
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex != null && hex.getBuilding() != null
                        && hex.getBuilding().getId() == buildingId) {
                    return hex.getBuilding();
                }
            }
        }
        return null;
    }

    /** Skips eliminated and disconnected players so the game never stalls. */
    public void advanceTurn() {
        int previous = currentPlayerIndex;
        for (int step = 1; step <= players.size(); step++) {
            int next = (previous + step) % players.size();
            Player candidate = players.get(next);
            if (candidate.isAlive() && candidate.isConnected()) {
                currentPlayerIndex = next;
                if (next <= previous) {
                    turn++;
                }
                return;
            }
        }
    }

    public boolean owns(Player player, Unit unit) {
        return player != null && unit != null && unit.getOwnerId() == player.getId();
    }

    public boolean owns(Player player, Building building) {
        return player != null && building != null && building.getOwnerId() == player.getId();
    }

    public boolean isDiscovered(Player player, Hex hex) {
        return player != null && hex != null && player.getFog().isDiscovered(hex);
    }

    public boolean isOwned(Player player, Hex hex) {
        return player != null && hex != null && player.getFog().isOwned(hex);
    }

    public Player ownerOf(Hex hex) {
        if (hex == null) {
            return null;
        }
        for (Player player : players) {
            if (player.isAlive() && player.getFog().isOwned(hex)) {
                return player;
            }
        }
        return null;
    }

    public boolean isClaimed(Hex hex) {
        return hex != null && (hex.isReserved() || ownerOf(hex) != null);
    }

    public void claimFor(Player player, Hex hex) {
        if (player == null || hex == null) {
            return;
        }
        Player previous = ownerOf(hex);
        if (previous != null && previous.getId() != player.getId()) {
            previous.getFog().release(hex);
        }
        hex.setReserved(false);
        player.getFog().claim(hex);
    }

    public void revealAround(Player player, Unit unit) {
        if (player == null || unit == null) {
            return;
        }
        Hex centre = hexOf(unit);
        if (centre == null) {
            return;
        }
        for (Hex hex : map.withinRange(centre, unit.getVisionRadius())) {
            player.getFog().discover(hex);
            Tribe tribe = tribeAt(hex);
            if (tribe != null && !tribe.isDiscovered()) {
                tribe.discover();
                addLog("Discovered the " + tribe.getName()
                        + " (" + tribe.getType().getLabel() + ").");
            }
        }
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
        return getCurrentPlayer().isStarving();
    }

    public void setStarving(boolean starving) {
        getCurrentPlayer().setStarving(starving);
    }

    public boolean isStarving(Player player) {
        return player != null && player.isStarving();
    }

    public void setStarving(Player player, boolean starving) {
        if (player != null) {
            player.setStarving(starving);
        }
    }

    public List<String> getLog() {
        return log;
    }

    public void addLog(String message) {
        log.add("Turn " + turn + ": " + message);
    }

    /** Current player's living units. */
    public List<Unit> getUnits() {
        return getEmpire().getUnits();
    }

    public List<Unit> getAllUnits() {
        List<Unit> all = new ArrayList<>();
        for (Player player : players) {
            if (player.isAlive()) {
                all.addAll(player.getEmpire().getUnits());
            }
        }
        return all;
    }

    public Unit getSelected() {
        return selected;
    }

    public void select(Unit unit) {
        if (unit != null && !owns(getViewpointPlayer(), unit)) {
            return;
        }
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
            text.append(getEmpire().hasTech(Tech.SEAFARING)
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
        boolean buildable = hex.getTerrain().isLand() && isOwned(getCurrentPlayer(), hex) && hex.getBuilding() == null;
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
        for (Unit unit : getAllUnits()) {
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
        if (!owns(getCurrentPlayer(), unit)) {
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
            return getEmpire().hasTech(Tech.SEAFARING) && unit.getAp() > 0;
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
        if (!owns(getCurrentPlayer(), builder)) {
            return false;
        }
        if (!builder.hasCharge()) {
            return false;
        }
        if (!builder.isOn(hex)) {
            return false;
        }
        if (!isOwned(getCurrentPlayer(), hex)) {
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
        if (type.getRequiredTech() != null && !getEmpire().hasTech(type.getRequiredTech())) {
            return false;
        }
        if (getEmpire().getTownHall() == null
                || type.getRequiredLevel() > getEmpire().getTownHall().getLevel()) {
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
        return getEmpire().getStock().canPay(woodCost, type.getStoneCost(), type.getIronCost());
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
        return isDiscovered(getCurrentPlayer(), hex);
    }

    public boolean canTrain(UnitBlueprint blueprint) {
        TownHall hall = getEmpire().getTownHall();
        if (hall == null || hall.isBusy()) {
            return false;
        }
        return unitFactory.canCreate(blueprint);
    }

    public boolean canResearch(Tech tech) {
        TownHall hall = getEmpire().getTownHall();
        return hall != null && !hall.isBusy() && getEmpire().canResearch(tech);
    }

    public boolean canUpgradeTownHall() {
        TownHall hall = getEmpire().getTownHall();
        if (hall == null || hall.isBusy()) {
            return false;
        }
        TownHallLevel next = hall.getRank().next();
        if (next == null) {
            return false;
        }
        return getEmpire().getStock().canPay(next.getWoodCost(), next.getStoneCost(), next.getIronCost());
    }

    public boolean hasIdleUnitWithAp() {
        for (Unit unit : getEmpire().getUnits()) {
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
        Player owner = getPlayer(unit.getOwnerId());
        if (owner == null) {
            owner = getCurrentPlayer();
        }
        if (target.getTerrain().isSea()) {
            unit.moveTo(target);
            unit.emptyAp();
            revealAround(owner, unit);
            syncGarrisonHappiness(owner.getEmpire());
            return;
        }
        unit.spend(moveCost(unit, target));
        unit.moveTo(target);
        revealAround(owner, unit);
        syncGarrisonHappiness(owner.getEmpire());
    }

    public void build(Builder builder, BuildingType type, Hex hex) {
        if (!canBuild(builder, type, hex)) {
            return;
        }

        int woodCost = type.getWoodCost();
        if (type == BuildingType.DOCK && nextDockHalfPrice) {
            woodCost = woodCost / 2;
        }
        getEmpire().getStock().pay(woodCost, type.getStoneCost(), type.getIronCost());
        builder.spend(type.getApCost());

        Building building = buildingFactory.create(type, hex);
        building.setOwnerId(getCurrentPlayer().getId());
        getEmpire().getBuildings().add(building);

        if (type == BuildingType.SETTLEMENT) {
            getEmpire().raiseUnitCap(5);
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
        if (!isOwned(getCurrentPlayer(), hex) || !hex.getTerrain().isLand() || hex.hasRoad()) {
            return false;
        }
        return builder.canSpend(1) && getEmpire().getStock().canPay(ResourceType.WOOD, 8);
    }

    public void buildRoad(Builder builder) {
        if (!canBuildRoad(builder)) {
            return;
        }
        getEmpire().getStock().add(ResourceType.WOOD, -8);
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
        Player me = getCurrentPlayer();
        if (!isDiscovered(me, here) || !isDiscovered(me, other)) {
            return false;
        }
        if (!isOwned(me, here) && !isOwned(me, other)) {
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
                && getEmpire().getStock().canPay(15, 20, 0);
    }

    public void buildWall(Builder builder, Hex other) {
        if (!canBuildWall(builder, other)) {
            return;
        }
        getEmpire().getStock().pay(15, 20, 0);
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
        boolean hasBuilding = building != null && isOwned(getCurrentPlayer(), hex);
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
        Player me = getCurrentPlayer();
        claimFor(me, hex);
        for (Hex neighbour : map.neighbours(hex)) {
            claimFor(me, neighbour);
        }
        removeUnit(expander);
        addLog("The border was expanded and the Border Expander was consumed.");
    }

    public void train(UnitBlueprint blueprint) {
        if (!canTrain(blueprint)) {
            return;
        }
        getEmpire().getTownHall().start(new TrainUnitCommand(blueprint), this);
    }

    public void research(Tech tech) {
        if (!canResearch(tech)) {
            return;
        }
        getEmpire().getTownHall().start(new ResearchTechCommand(tech), this);
    }

    public void upgradeTownHall() {
        if (!canUpgradeTownHall()) {
            return;
        }
        TownHallLevel next = getEmpire().getTownHall().getRank().next();
        getEmpire().getTownHall().start(new UpgradeTownHallCommand(next), this);
    }

    public void cancelTownHallOrder() {
        getEmpire().getTownHall().cancelCommand(this);
    }

    public boolean hasMilitaryStable() {
        for (Building building : getEmpire().getBuildings()) {
            if (building.getType() == BuildingType.MILITARY_STABLE) {
                return true;
            }
        }
        return false;
    }

    public Hex spawnHexFor(UnitBlueprint blueprint) {
        TownHall hall = getEmpire().getTownHall();
        if (hall == null) {
            return null;
        }
        Hex home = hall.getHex();
        Unit probe = blueprint.create(home.getCol(), home.getRow());
        for (Hex hex : map.withinRange(home, 2)) {
            if (!isOwned(getCurrentPlayer(), hex) || !hex.getTerrain().isLand()) {
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
        for (Unit unit : getAllUnits()) {
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
        Player me = getCurrentPlayer();
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
            if (!owns(me, military)) {
                continue;
            }
            if (distance == 2 && !(military instanceof Archer)) {
                continue;
            }
            result.add(military);
        }
        return result;
    }

    private int attackerCombatBonus(List<MilitaryUnit> attackers) {
        for (MilitaryUnit unit : attackers) {
            if (unit.isCombatBuffed()) {
                return 1;
            }
        }
        return 0;
    }

    public boolean anyUnitAt(Hex hex) {
        return hex != null && !unitsAt(hex).isEmpty();
    }

    /**
     * Whether {@code unit} could stand on {@code hex} (terrain / seafaring / blocked),
     * ignoring AP, adjacency and stacking. Used by teleport validation.
     */
    public boolean isPassableFor(Unit unit, Hex hex) {
        if (unit == null || hex == null) {
            return false;
        }
        if (!hex.getTerrain().isPassable() || hex.isBlocked()) {
            return false;
        }
        if (hex.getTerrain().isSea()) {
            Player owner = getPlayer(unit.getOwnerId());
            Empire empire = owner != null ? owner.getEmpire() : getEmpire();
            return empire.hasTech(Tech.SEAFARING);
        }
        return true;
    }

    public void expireItemEffects(Player player) {
        if (player == null) {
            return;
        }
        for (Unit unit : player.getEmpire().getUnits()) {
            if (unit instanceof MilitaryUnit) {
                ((MilitaryUnit) unit).setCombatBuffed(false);
            }
        }
        player.clearItemUsedThisTurn();
    }

    public boolean canCraftItem(Apothecary apothecary, ItemType type) {
        if (apothecary == null || type == null) {
            return false;
        }
        Player me = getCurrentPlayer();
        if (!owns(me, apothecary) || apothecary.isBusy()) {
            return false;
        }
        Stockpile stock = me.getEmpire().getStock();
        for (Map.Entry<ResourceType, Integer> entry : type.getCost().entrySet()) {
            if (entry.getValue() > 0 && !stock.canPay(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public String craftItemRejection(Apothecary apothecary, ItemType type) {
        if (apothecary == null) {
            return "No Apothecary selected.";
        }
        Player me = getCurrentPlayer();
        if (!owns(me, apothecary)) {
            return "That Apothecary does not belong to you.";
        }
        if (apothecary.isBusy()) {
            return "The Apothecary is already crafting.";
        }
        Stockpile stock = me.getEmpire().getStock();
        for (Map.Entry<ResourceType, Integer> entry : type.getCost().entrySet()) {
            if (entry.getValue() > 0 && !stock.canPay(entry.getKey(), entry.getValue())) {
                return "You do not have enough " + entry.getKey().getLabel().toLowerCase()
                        + " for this.";
            }
        }
        return null;
    }

    public void craftItem(Apothecary apothecary, ItemType type) {
        if (!canCraftItem(apothecary, type)) {
            return;
        }
        apothecary.start(new CraftItemCommand(type, getCurrentPlayer()), this);
    }

    public String useItemRejection(Player player, ItemType type, Unit target, Hex destination) {
        if (player == null || type == null) {
            return "Invalid item use.";
        }
        if (target == null) {
            return "Select one of your units.";
        }
        if (!owns(player, target)) {
            return "You can only use items on your own units.";
        }
        if (target instanceof MilitaryUnit && ((MilitaryUnit) target).isHostile()) {
            return "You can only use items on your own units.";
        }
        if (player.hasUsedItem(target)) {
            return "This unit has already used an item this turn.";
        }
        if (!player.getEmpire().getInventory().has(type)) {
            return "You do not have that item.";
        }
        Item item = ItemFactory.create(type);
        return item.rejectionReason(this, player, target, destination);
    }

    public boolean canUseItem(Player player, ItemType type, Unit target, Hex destination) {
        return useItemRejection(player, type, target, destination) == null;
    }

    public void useItem(Player player, ItemType type, Unit target, Hex destination) {
        if (!canUseItem(player, type, target, destination)) {
            return;
        }
        Item item = ItemFactory.create(type);
        item.apply(this, player, target, destination);
        player.getEmpire().getInventory().remove(type);
        player.markItemUsed(target);
    }

    public boolean ownsAllUnitsAt(Player player, Hex hex) {
        if (player == null || hex == null) {
            return false;
        }
        List<Unit> here = unitsAt(hex);
        if (here.isEmpty()) {
            return false;
        }
        for (Unit unit : here) {
            if (!owns(player, unit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Owner of opposing player units or buildings on this hex.
     * AI hostiles and tribes return null (no diplomatic state).
     */
    public Player ownerOfUnitsAt(Hex hex) {
        if (hex == null) {
            return null;
        }
        for (Unit unit : unitsAt(hex)) {
            if (unit instanceof MilitaryUnit && ((MilitaryUnit) unit).isHostile()) {
                continue;
            }
            Player owner = getPlayer(unit.getOwnerId());
            if (owner != null && owner.getId() != getCurrentPlayer().getId()) {
                return owner;
            }
        }
        Building building = hex.getBuilding();
        if (building != null && !owns(getCurrentPlayer(), building)
                && !(building instanceof TribeCamp)) {
            return getPlayer(building.getOwnerId());
        }
        return null;
    }

    /** Other players' military on this hex. */
    public List<MilitaryUnit> enemyPlayerMilitaryAt(Hex hex) {
        List<MilitaryUnit> result = new ArrayList<>();
        if (hex == null) {
            return result;
        }
        Player me = getCurrentPlayer();
        for (Unit unit : unitsAt(hex)) {
            if (!(unit instanceof MilitaryUnit)) {
                continue;
            }
            MilitaryUnit military = (MilitaryUnit) unit;
            if (military.isHostile()) {
                continue;
            }
            if (owns(me, military)) {
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
        if (!isDiscovered(getCurrentPlayer(), to)) {
            return false;
        }
        int distance = HexGeometry.distance(
                from.getCol(), from.getRow(), to.getCol(), to.getRow());
        if (attackersOn(from, to).isEmpty()) {
            return false;
        }
        Player defender = ownerOfUnitsAt(to);
        boolean warOk = defender == null || diplomacy.canAttack(getCurrentPlayer(), defender);
        boolean enemyUnits = !enemyPlayerMilitaryAt(to).isEmpty();
        if (distance == 1) {
            return !hostilesAt(to).isEmpty()
                    || (enemyUnits && warOk)
                    || canStrikeStructure(to)
                    || canCapture(to)
                    || canStrikeTribeCamp(to);
        }
        if (distance == 2) {
            return !hostilesAt(to).isEmpty()
                    || (enemyUnits && warOk)
                    || canStrikeStructure(to)
                    || canStrikeTribeCamp(to);
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
        if (!canAttack(from, to)) {
            return false;
        }
        return !hostilesAt(to).isEmpty() || !enemyPlayerMilitaryAt(to).isEmpty();
    }

    public BattleReport beginDiceAttack(Hex from, Hex to) {
        if (!isDiceAttack(from, to)) {
            return null;
        }
        List<MilitaryUnit> attackers = attackersOn(from, to);
        List<MilitaryUnit> defenders = new ArrayList<>(hostilesAt(to));
        if (defenders.isEmpty()) {
            defenders.addAll(enemyPlayerMilitaryAt(to));
        }
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
                attackerCombatBonus(attackers),
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
                pendingReport.noteUnitLost(unit.getTypeName());
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
            claimFor(getCurrentPlayer(), to);
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
        if (building != null && !owns(getCurrentPlayer(), building)) {
            strikeBuilding(building, battle.damageToStructure(attackers));
        }
    }

    /** Quiet attack that also returns a report for the defender's inbox. */
    public BattleReport performQuietAttackWithReport(Hex from, Hex to) {
        if (!canAttack(from, to) || isDiceAttack(from, to)) {
            return null;
        }
        List<MilitaryUnit> attackers = attackersOn(from, to);
        for (MilitaryUnit unit : attackers) {
            unit.spend(1);
        }
        if (canCapture(to)) {
            claimFor(getCurrentPlayer(), to);
            addLog("The hex was captured without a fight.");
            return null;
        }
        Building building = to.getBuilding();
        if (canStrikeTribeCamp(to)) {
            Tribe tribe = tribeAt(to);
            noteAggressionOnTribe(tribe);
            int damage = battle.damageToStructure(attackers);
            strikeBuilding(building, damage);
            return BattleReport.structureOnly(damage, "tribe camp");
        }
        if (building != null && !owns(getCurrentPlayer(), building)) {
            int damage = battle.damageToStructure(attackers);
            String label = building.getType().name().toLowerCase().replace('_', ' ');
            strikeBuilding(building, damage);
            return BattleReport.structureOnly(damage, label);
        }
        return null;
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

    private void spawnHostilesNear(int originCol, int originRow) {
        Hex barbHex = null;
        Hex animalHex = null;
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || isClaimed(hex)) {
                    continue;
                }
                int distance = HexGeometry.distance(originCol, originRow, col, row);
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
            getCurrentPlayer().getFog().discover(barbHex);
            addHostile(new Barbarian(barbHex.getCol(), barbHex.getRow()));
            addHostile(new Barbarian(barbHex.getCol(), barbHex.getRow()));
            addLog("Raiders were spotted nearby.");
        }
        if (animalHex != null) {
            getCurrentPlayer().getFog().discover(animalHex);
            addHostile(new WildAnimal(animalHex.getCol(), animalHex.getRow()));
        }
    }

    private boolean canStrikeStructure(Hex to) {
        Building building = to.getBuilding();
        if (building == null || owns(getCurrentPlayer(), building)) {
            return false;
        }
        if (building instanceof TribeCamp) {
            return false;
        }
        if (!hostilesAt(to).isEmpty() || !enemyPlayerMilitaryAt(to).isEmpty()) {
            return false;
        }
        Player owner = getPlayer(building.getOwnerId());
        if (owner != null && !diplomacy.canAttack(getCurrentPlayer(), owner)) {
            return false;
        }
        return true;
    }

    private boolean canStrikeTribeCamp(Hex to) {
        Tribe tribe = tribeAt(to);
        if (tribe == null || tribe.isDestroyed()) {
            return false;
        }
        return hostilesAt(to).isEmpty() && enemyPlayerMilitaryAt(to).isEmpty();
    }

    private boolean canCapture(Hex to) {
        if (isClaimed(to) || to.getBuilding() != null) {
            return false;
        }
        if (!to.getTerrain().isLand()) {
            return false;
        }
        return hostilesAt(to).isEmpty() && enemyPlayerMilitaryAt(to).isEmpty();
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
        boolean playerUnit = false;
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
            if (!unit.isHostile() && !(unit instanceof TribeGuard)) {
                playerUnit = true;
            }
        }
        if (playerUnit) {
            return attackerDiceCount(defenders, 1);
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
            Player owner = getPlayer(building.getOwnerId());
            Empire empire = owner != null ? owner.getEmpire() : null;
            if (empire != null) {
                empire.forgetTownHall((TownHall) building);
            }
            building.getHex().setBuilding(null);
            if (owner != null) {
                checkElimination(owner);
            }
            bus.publish(GameEvent.BUILDING_DESTROYED, building);
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
        Player owner = getPlayer(building.getOwnerId());
        if (owner != null) {
            Empire empire = owner.getEmpire();
            if (building instanceof TownHall) {
                empire.forgetTownHall((TownHall) building);
            } else {
                empire.getBuildings().remove(building);
            }
        } else {
            for (Player player : players) {
                player.getEmpire().getBuildings().remove(building);
            }
        }
        bus.publish(GameEvent.BUILDING_DESTROYED, building);
    }

    private void placeTradingPostNear(int originCol, int originRow) {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || isClaimed(hex)) {
                    continue;
                }
                if (hex.getBuilding() != null) {
                    continue;
                }
                int distance = HexGeometry.distance(originCol, originRow, col, row);
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
            getEmpire().addHappinessBonus(-15);
            addLog("Attacking an ally: −15 happiness.");
        } else if (wasFriendly) {
            getEmpire().addHappinessBonus(-5);
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
        outpost.setOwnerId(getCurrentPlayer().getId());
        camp.setBuilding(outpost);
        getEmpire().getBuildings().add(outpost);
        camp.setReserved(false);
        claimFor(getCurrentPlayer(), camp);
        for (Hex neighbour : map.neighbours(camp)) {
            if (neighbour.getTerrain().isPassable() && neighbour.getTerrain().isLand()) {
                claimFor(getCurrentPlayer(), neighbour);
            }
        }
        bus.publish(GameEvent.BUILDING_DESTROYED, tribe.getCamp());
        addLog(tribe.getName() + " was defeated. The camp is now an Outpost.");
    }

    private void grantLoot(Tribe tribe) {
        switch (tribe.getType()) {
            case FARMER:
                getEmpire().getStock().add(ResourceType.FOOD, 40);
                break;
            case MOUNTAIN:
                getEmpire().getStock().add(ResourceType.STONE, 25);
                getEmpire().getStock().add(ResourceType.IRON, 15);
                break;
            case TRADER:
                getEmpire().getStock().add(ResourceType.WOOD, 20);
                getEmpire().getStock().add(ResourceType.STONE, 20);
                getEmpire().getStock().add(ResourceType.FOOD, 20);
                break;
            case WARRIOR:
                getEmpire().getStock().add(ResourceType.IRON, 30);
                break;
            case COASTAL:
                getEmpire().getStock().add(ResourceType.FOOD, 25);
                getEmpire().getStock().add(ResourceType.WOOD, 25);
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
        syncGarrisonHappiness(getEmpire());
    }

    /**
     * Season + happiness modifiers applied on top of a building's raw output.
     */
    public int adjustProduction(Building building, int raw) {
        if (building.isPaused(turn)) {
            return 0;
        }
        Empire empire = getEmpire(building.getOwnerId());
        if (empire == null) {
            empire = getEmpire();
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
        syncGarrisonHappiness(getEmpire());
    }

    public void syncGarrisonHappiness(Empire empire) {
        if (empire == null) {
            return;
        }
        TownHall hall = empire.getTownHall();
        if (hall == null) {
            empire.getHappiness().setGarrisonBonus(0);
            return;
        }
        int garrison = 0;
        for (Unit unit : unitsAt(hall.getHex())) {
            if (unit.isMilitary() && unit.getOwnerId() == hall.getOwnerId()) {
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
        for (Unit unit : getAllUnits()) {
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
        for (Unit unit : getAllUnits()) {
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
        if (!getEmpire().getStock().canPay(type, amount)) {
            return;
        }
        int gain = tribe.giftRelationGain(type, amount);
        if (gain <= 0) {
            return;
        }
        getEmpire().getStock().add(type, -amount);
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
                && getEmpire().getStock().canPay(ResourceType.FOOD, 30)
                && getEmpire().getStock().canPay(ResourceType.WOOD, 30)
                && getEmpire().getStock().canPay(ResourceType.IRON, 30);
    }

    public void askPeace(Tribe tribe) {
        if (!canAskPeace(tribe)) {
            return;
        }
        getEmpire().getStock().add(ResourceType.FOOD, -30);
        getEmpire().getStock().add(ResourceType.WOOD, -30);
        getEmpire().getStock().add(ResourceType.IRON, -30);
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
        for (Building building : getEmpire().getBuildings()) {
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
                if (hex == null || !isOwned(getCurrentPlayer(), hex)) {
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
        Player owner = getPlayer(unit.getOwnerId());
        if (owner == null) {
            owner = getCurrentPlayer();
        }
        revealAround(owner, unit);
    }

    public boolean canFoundTownHall(Builder builder, Hex hex) {
        if (builder == null || hex == null) {
            return false;
        }
        Player me = getCurrentPlayer();
        if (!owns(me, builder) || !builder.isOn(hex) || !builder.hasCharge()) {
            return false;
        }
        if (!hex.getTerrain().isLand() || hex.getBuilding() != null || isClaimed(hex)) {
            return false;
        }
        if (isOwned(me, hex)) {
            return false; // must be outside borders
        }
        if (!builder.canSpend(1)) {
            return false;
        }
        Empire empire = me.getEmpire();
        return empire.getStock().canPay(ResourceType.WOOD, FOUND_WOOD)
                && empire.getStock().canPay(ResourceType.STONE, FOUND_STONE)
                && empire.getStock().canPay(ResourceType.IRON, FOUND_IRON)
                && empire.getStock().canPay(ResourceType.FOOD, FOUND_FOOD);
    }

    public void foundTownHall(Builder builder, Hex hex) {
        if (!canFoundTownHall(builder, hex)) {
            return;
        }
        Player me = getCurrentPlayer();
        Empire empire = me.getEmpire();
        empire.getStock().add(ResourceType.WOOD, -FOUND_WOOD);
        empire.getStock().add(ResourceType.STONE, -FOUND_STONE);
        empire.getStock().add(ResourceType.IRON, -FOUND_IRON);
        empire.getStock().add(ResourceType.FOOD, -FOUND_FOOD);
        builder.spend(1);

        TownHall hall = new TownHall(hex);
        hall.setOwnerId(me.getId());
        hex.setBuilding(hall);
        empire.addTownHall(hall);
        claimFor(me, hex);
        for (Hex neighbour : map.neighbours(hex)) {
            claimFor(me, neighbour);
        }
        bus.publish(GameEvent.BUILDING_PLACED, hall);

        builder.useCharge();
        if (!builder.hasCharge()) {
            removeUnit(builder);
        }
        addLog(me.getName() + " founded a new Town Hall.");
    }

    /**
     * Called after Town Hall damage. Design decision: on elimination we remove
     * everything the player owns rather than leaving abandoned units on the map.
     */
    public void checkElimination(Player player) {
        if (player == null || !player.isAlive() || player.hasTownHall()) {
            return;
        }

        player.eliminate();
        addLog(player.getName() + " has been eliminated.");

        for (Unit unit : new ArrayList<>(player.getEmpire().getUnits())) {
            removeUnit(unit);
        }
        for (Building building : new ArrayList<>(player.getEmpire().getBuildings())) {
            Hex hex = building.getHex();
            if (hex != null) {
                hex.setBuilding(null);
                player.getFog().release(hex);
            }
        }
        // clear ownership fog for remaining claimed hexes
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex != null && player.getFog().isOwned(hex)) {
                    player.getFog().release(hex);
                }
            }
        }
        player.getEmpire().getBuildings().clear();
        player.getEmpire().setTownHall(null);
        if (selected != null && selected.getOwnerId() == player.getId()) {
            selected = null;
        }
    }

    public Player findWinner() {
        List<Player> standing = new ArrayList<>();
        for (Player player : players) {
            if (player.isAlive() && player.hasTownHall()) {
                standing.add(player);
            }
        }
        return standing.size() == 1 ? standing.get(0) : null;
    }
}

