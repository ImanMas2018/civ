package civ.model.save;

import civ.model.Archer;
import civ.model.Barbarian;
import civ.model.Bear;
import civ.model.Building;
import civ.model.BuildingType;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.Cavalry;
import civ.model.Edge;
import civ.model.Empire;
import civ.model.Explorer;
import civ.model.Game;
import civ.model.GameMap;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Outpost;
import civ.model.ProductionBuilding;
import civ.model.ResourceType;
import civ.model.Stockpile;
import civ.model.Swordsman;
import civ.model.Tech;
import civ.model.TownHall;
import civ.model.TownHallLevel;
import civ.model.TradingPost;
import civ.model.Unit;
import civ.model.UnitBlueprint;
import civ.model.Wall;
import civ.model.WildAnimal;
import civ.model.Worker;
import civ.model.command.Command;
import civ.model.command.ResearchTechCommand;
import civ.model.command.TrainUnitCommand;
import civ.model.command.UpgradeTownHallCommand;
import civ.model.tribe.QuestStatus;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeCamp;
import civ.model.tribe.TribeGuard;
import civ.model.tribe.TribeType;
import civ.model.world.Happiness;
import civ.model.world.Season;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SaveService {

    private static final Path FOLDER = Paths.get(System.getProperty("user.home"), ".civ-saves");
    private static final String[] SLOT_NAMES = {"slot1", "slot2", "slot3", "autosave"};
    private static final int SAVE_VERSION = 1;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ------------------------------------------------------------------ public API

    public SaveGame capture(Game game, String saveName) throws IOException {
        SaveGame data = new SaveGame();
        data.saveVersion = SAVE_VERSION;
        data.saveName = saveName;
        data.savedAtMillis = System.currentTimeMillis();
        data.mapSeed = game.getMapSeed();
        data.turn = game.getTurn();
        data.summary = buildSummary(game);

        Empire empire = game.getEmpire();
        Happiness happiness = empire.getHappiness();
        data.happiness = happiness.getRawValue();
        data.militaryCapHit = happiness.hasMilitaryCapHit();
        data.randomState = encodeRandom(game.getRandom());
        data.starving = game.isStarving();
        data.nextDockHalfPrice = game.isNextDockHalfPrice();
        data.unitCap = empire.getUnitCap();
        data.lastBearTurn = game.getLastBearTurn();

        Stockpile stock = empire.getStock();
        data.stockCapacity = stock.getCapacity();
        data.food = stock.get(ResourceType.FOOD);
        data.wood = stock.get(ResourceType.WOOD);
        data.stone = stock.get(ResourceType.STONE);
        data.iron = stock.get(ResourceType.IRON);

        captureTownHall(empire.getTownHall(), data);

        for (Tech tech : empire.getTechs()) {
            data.techs.add(tech.name());
        }

        captureHexes(game.getMap(), data);
        captureEdges(game.getMap(), data);

        Map<Building, Integer> buildingIds = new HashMap<>();
        captureBuildings(game, buildingIds, data);

        Map<Unit, Integer> unitIds = new HashMap<>();
        captureEmpireUnits(game, buildingIds, unitIds, data);
        captureHostiles(unitIds, game.getHostiles(), data);
        captureTribes(game, buildingIds, unitIds, data);

        data.log.addAll(game.getLog());
        return data;
    }

    public void apply(Game game, SaveGame data) throws IOException {
        if (data == null) {
            throw new IOException("Save data is missing.");
        }
        if (data.saveVersion > SAVE_VERSION) {
            throw new IOException("This save file is from a newer version.");
        }

        game.clearForLoad();
        game.setTurn(data.turn);
        game.setStarving(data.starving);
        game.setNextDockHalfPrice(data.nextDockHalfPrice);
        game.setLastBearTurn(data.lastBearTurn);
        game.restoreRandomFromBase64(data.randomState);

        Empire empire = game.getEmpire();
        empire.setUnitCap(data.unitCap);
        Happiness happiness = empire.getHappiness();
        happiness.setRawValue(data.happiness);
        happiness.setMilitaryCapHit(data.militaryCapHit);

        applyHexes(game.getMap(), data);
        applyEdges(game.getMap(), data);

        Map<Integer, Building> buildingsById = new HashMap<>();
        TownHall townHall = applyTownHall(game, empire, data, buildingsById);
        applyOtherBuildings(game, empire, data, buildingsById);

        empire.getTechs().clear();
        for (String techName : data.techs) {
            empire.getTechs().add(Tech.valueOf(techName));
        }

        Stockpile stock = empire.getStock();
        stock.setCapacity(data.stockCapacity);
        stock.set(ResourceType.FOOD, data.food);
        stock.set(ResourceType.WOOD, data.wood);
        stock.set(ResourceType.STONE, data.stone);
        stock.set(ResourceType.IRON, data.iron);

        if (townHall != null && data.queueKind != null && !data.queueKind.isEmpty()) {
            Command command = restoreQueueCommand(data);
            townHall.restoreQueue(command, data.queueTurnsLeft);
        }

        Map<Integer, Unit> unitsById = new HashMap<>();
        applyEmpireUnits(game, empire, data, unitsById);
        linkWorkerStations(data, unitsById, buildingsById);
        applyHostiles(game, data, unitsById);
        applyTribes(game, data, unitsById);

        game.getLog().addAll(data.log);
        game.syncGarrisonHappiness();
    }

    public void save(Game game, String slotName, String saveName) throws IOException {
        writeSave(capture(game, saveName), slotName);
    }

    public void autosave(Game game) throws IOException {
        save(game, "autosave", "Autosave");
    }

    public SaveGame load(String slotName) throws IOException {
        Path file = slotPath(slotName);
        if (!Files.exists(file)) {
            throw new IOException("Save slot \"" + slotName + "\" is empty.");
        }
        SaveGame data;
        try (Reader reader = Files.newBufferedReader(file)) {
            data = gson.fromJson(reader, SaveGame.class);
        }
        if (data == null || data.hexes == null || data.saveVersion > SAVE_VERSION) {
            throw new IOException("This save file is damaged or from a newer version.");
        }
        return data;
    }

    public List<SaveSlotInfo> listSlots() {
        List<SaveSlotInfo> rows = new ArrayList<>();
        for (String slotName : SLOT_NAMES) {
            SaveSlotInfo.Kind kind = "autosave".equals(slotName)
                    ? SaveSlotInfo.Kind.AUTOSAVE
                    : SaveSlotInfo.Kind.MANUAL;
            SaveSlotInfo row = new SaveSlotInfo(slotName, kind);
            Path file = slotPath(slotName);
            if (!Files.exists(file)) {
                row.setStatus(SaveSlotInfo.Status.EMPTY);
                rows.add(row);
                continue;
            }
            try {
                SaveGame data = load(slotName);
                row.setStatus(SaveSlotInfo.Status.OK);
                row.setDisplayName(data.saveName);
                row.setTurn(data.turn);
                row.setSeason(Season.forTurn(data.turn).getLabel());
                row.setSavedAtMillis(data.savedAtMillis);
                row.setTownHallLevel(data.townHallLevel);
                row.setSummary(data.summary);
            } catch (IOException ex) {
                row.setStatus(SaveSlotInfo.Status.DAMAGED);
            }
            rows.add(row);
        }
        return rows;
    }

    public void deleteSlot(String slotName) throws IOException {
        Files.deleteIfExists(slotPath(slotName));
        Files.deleteIfExists(slotPath(slotName + ".tmp"));
    }

    // ------------------------------------------------------------------ disk I/O

    private void writeSave(SaveGame data, String slotName) throws IOException {
        Files.createDirectories(FOLDER);

        Path finalFile = slotPath(slotName);
        Path tempFile = slotPath(slotName + ".tmp");

        try (Writer writer = Files.newBufferedWriter(tempFile)) {
            gson.toJson(data, writer);
        }

        try (Reader reader = Files.newBufferedReader(tempFile)) {
            SaveGame check = gson.fromJson(reader, SaveGame.class);
            if (check == null || check.hexes == null) {
                Files.deleteIfExists(tempFile);
                throw new IOException("The written save file did not read back correctly.");
            }
        }

        try {
            Files.move(tempFile, finalFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path slotPath(String slotName) {
        return FOLDER.resolve(slotName + ".json");
    }

    // ------------------------------------------------------------------ capture helpers

    private static String buildSummary(Game game) {
        Empire empire = game.getEmpire();
        return "units " + empire.getUnits().size()
                + "  buildings " + empire.getBuildings().size()
                + "  happiness " + empire.getHappiness().getValue();
    }

    private static void captureTownHall(TownHall hall, SaveGame data) {
        if (hall == null) {
            return;
        }
        data.townHallLevel = hall.getLevel();
        data.townHallHp = hall.getHp();
        data.townHallMaxHp = hall.getMaxHp();
        data.townHallDefence = hall.getDefence();
        data.townHallWall = hall.hasDefensiveWall();
        if (!hall.isBusy()) {
            return;
        }
        Command command = hall.getActiveCommand();
        data.queueTurnsLeft = hall.getTurnsLeft();
        if (command instanceof TrainUnitCommand) {
            data.queueKind = "TRAIN";
            data.queueTarget = blueprintFromTrainLabel(command.getLabel()).name();
        } else if (command instanceof ResearchTechCommand) {
            data.queueKind = "RESEARCH";
            data.queueTarget = techFromResearchLabel(command.getLabel()).name();
        } else if (command instanceof UpgradeTownHallCommand) {
            data.queueKind = "UPGRADE";
            data.queueTarget = levelFromUpgradeLabel(command.getLabel()).getNumber() + "";
        }
    }

    private static void captureHexes(GameMap map, SaveGame data) {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null) {
                    continue;
                }
                SaveGame.HexData hd = new SaveGame.HexData();
                hd.col = col;
                hd.row = row;
                hd.depositAmount = hex.getDepositAmount();
                hd.discovered = hex.isDiscovered();
                hd.owned = hex.isOwned();
                hd.road = hex.hasRoad();
                hd.blocked = hex.isBlocked();
                data.hexes.add(hd);
            }
        }
    }

    private static void captureEdges(GameMap map, SaveGame data) {
        for (Edge edge : map.getEdges().all()) {
            if (!edge.hasRiver() && !edge.hasWall()) {
                continue;
            }
            SaveGame.EdgeData ed = new SaveGame.EdgeData();
            ed.col1 = edge.getCol1();
            ed.row1 = edge.getRow1();
            ed.col2 = edge.getCol2();
            ed.row2 = edge.getRow2();
            ed.river = edge.hasRiver();
            if (edge.hasWall()) {
                ed.wallHp = edge.getWall().getHp();
            }
            data.edges.add(ed);
        }
    }

    private static void captureBuildings(Game game, Map<Building, Integer> buildingIds, SaveGame data) {
        int nextId = 1;
        Set<Building> seen = new HashSet<>();

        for (Building building : game.getEmpire().getBuildings()) {
            nextId = captureOneBuilding(building, buildingIds, seen, data, nextId);
        }

        GameMap map = game.getMap();
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex == null) {
                    continue;
                }
                Building building = hex.getBuilding();
                if (building == null || seen.contains(building)) {
                    continue;
                }
                if (building instanceof TribeCamp) {
                    continue;
                }
                if (building.getType() == BuildingType.TRADING_POST) {
                    nextId = captureOneBuilding(building, buildingIds, seen, data, nextId);
                }
            }
        }
    }

    private static int captureOneBuilding(Building building, Map<Building, Integer> buildingIds,
                                          Set<Building> seen, SaveGame data, int nextId) {
        if (building == null || seen.contains(building)) {
            return nextId;
        }
        seen.add(building);
        buildingIds.put(building, nextId);

        SaveGame.BuildingData bd = new SaveGame.BuildingData();
        bd.id = nextId;
        bd.type = building.getType().name();
        Hex hex = building.getHex();
        bd.col = hex.getCol();
        bd.row = hex.getRow();
        bd.hp = building.getHp();
        bd.maxHp = building.getMaxHp();
        bd.unpaidTurns = building.getUnpaidTurns();
        bd.pausedUntilTurn = building.getPausedUntilTurn();
        data.buildings.add(bd);
        return nextId + 1;
    }

    private static void captureEmpireUnits(Game game, Map<Building, Integer> buildingIds,
                                           Map<Unit, Integer> unitIds, SaveGame data) {
        int nextId = 1;
        for (Unit unit : game.getEmpire().getUnits()) {
            unitIds.put(unit, nextId);
            data.units.add(captureUnit(unit, nextId, buildingIds, null));
            nextId++;
        }
    }

    private static void captureHostiles(Map<Unit, Integer> unitIds,
                                        List<MilitaryUnit> hostiles, SaveGame data) {
        int nextId = unitIds.size() + 1;
        for (MilitaryUnit unit : hostiles) {
            unitIds.put(unit, nextId);
            data.hostiles.add(captureUnit(unit, nextId, null, null));
            nextId++;
        }
    }

    private static void captureTribes(Game game, Map<Building, Integer> buildingIds,
                                      Map<Unit, Integer> unitIds, SaveGame data) {
        int nextId = unitIds.size() + 1;
        for (Tribe tribe : game.getTribes()) {
            SaveGame.TribeData td = new SaveGame.TribeData();
            td.id = tribe.getId();
            td.name = tribe.getName();
            td.type = tribe.getType().name();
            Hex campHex = tribe.getCampHex();
            td.campCol = campHex.getCol();
            td.campRow = campHex.getRow();
            td.relation = tribe.getRelation();
            td.discovered = tribe.isDiscovered();
            td.destroyed = tribe.isDestroyed();
            td.allied = tribe.isAllied();
            td.tradeBonusPercent = tribe.getTradeBonusPercent();
            td.questBlockTurns = tribe.getQuestBlockTurns();
            td.turnsSinceGuardSpawn = tribe.getTurnsSinceGuardSpawn();
            td.turnsSinceQuestOffer = tribe.getTurnsSinceQuestOffer();

            TribeCamp camp = tribe.getCamp();
            if (camp != null) {
                td.campHp = camp.getHp();
                td.campMaxHp = camp.getMaxHp();
            } else {
                td.campHp = 0;
                td.campMaxHp = tribe.getType().getCampHp();
            }

            if (tribe.getQuest() != null) {
                td.questStatus = tribe.getQuest().getStatus().name();
                td.questTurnsLeft = tribe.getQuest().getTurnsLeft();
                td.questProgress = tribe.getQuest().getProgress();
            }

            for (TribeGuard guard : tribe.getGuards()) {
                unitIds.put(guard, nextId);
                td.guards.add(captureUnit(guard, nextId, buildingIds, tribe.getId()));
                nextId++;
            }
            data.tribes.add(td);
        }
    }

    private static SaveGame.UnitData captureUnit(Unit unit, int id,
                                                 Map<Building, Integer> buildingIds,
                                                 Integer tribeId) {
        SaveGame.UnitData ud = new SaveGame.UnitData();
        ud.id = id;
        ud.type = unit.getTypeName();
        ud.col = unit.getCol();
        ud.row = unit.getRow();
        ud.ap = unit.getAp();
        ud.bodyHp = unit.getBodyHp();
        ud.maxBodyHp = unit.getMaxBodyHp();
        if (unit instanceof MilitaryUnit) {
            MilitaryUnit military = (MilitaryUnit) unit;
            ud.combatHp = military.getCombatHp();
            ud.maxCombatHp = military.getMaxCombatHp();
            ud.attackPower = military.getAttackPower();
            ud.attackRange = military.getAttackRange();
        }
        if (unit instanceof Worker) {
            Worker worker = (Worker) unit;
            ProductionBuilding station = worker.getStation();
            if (station != null && buildingIds != null) {
                ud.stationBuildingId = buildingIds.get(station);
            }
        }
        ud.tribeId = tribeId;
        return ud;
    }

    private static String encodeRandom(Random random) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(random);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    // ------------------------------------------------------------------ apply helpers

    private static void applyHexes(GameMap map, SaveGame data) {
        for (SaveGame.HexData hd : data.hexes) {
            Hex hex = map.get(hd.col, hd.row);
            if (hex == null) {
                continue;
            }
            hex.setDepositAmount(hd.depositAmount);
            hex.setDiscovered(hd.discovered);
            hex.setOwned(hd.owned);
            hex.setRoad(hd.road);
            hex.setBlocked(hd.blocked);
        }
    }

    private static void applyEdges(GameMap map, SaveGame data) {
        for (SaveGame.EdgeData ed : data.edges) {
            Hex a = map.get(ed.col1, ed.row1);
            Hex b = map.get(ed.col2, ed.row2);
            if (a == null || b == null) {
                continue;
            }
            Edge edge = map.getEdges().getOrCreate(a, b);
            edge.setRiver(ed.river);
            if (ed.wallHp != null) {
                edge.setWall(new Wall(ed.wallHp));
            } else {
                edge.setWall(null);
            }
        }
    }

    private static TownHall applyTownHall(Game game, Empire empire, SaveGame data,
                                          Map<Integer, Building> buildingsById) throws IOException {
        SaveGame.BuildingData thData = findBuilding(data, BuildingType.TOWN_HALL.name());
        if (thData == null) {
            throw new IOException("Save is missing the Town Hall.");
        }
        Hex hex = requireHex(game.getMap(), thData.col, thData.row);
        TownHall townHall = new TownHall(hex);
        restoreBuildingFields(townHall, thData);
        townHall.restoreState(data.townHallLevel, data.townHallHp, data.townHallMaxHp,
                data.townHallDefence, data.townHallWall);
        hex.setBuilding(townHall);
        empire.setTownHall(townHall);
        buildingsById.put(thData.id, townHall);
        return townHall;
    }

    private static void applyOtherBuildings(Game game, Empire empire, SaveGame data,
                                            Map<Integer, Building> buildingsById) throws IOException {
        for (SaveGame.BuildingData bd : data.buildings) {
            if (BuildingType.TOWN_HALL.name().equals(bd.type)) {
                continue;
            }
            BuildingType type = BuildingType.valueOf(bd.type);
            Hex hex = requireHex(game.getMap(), bd.col, bd.row);
            Building building = createBuildingSilent(type, hex);
            restoreBuildingFields(building, bd);
            buildingsById.put(bd.id, building);

            if (type == BuildingType.TRADING_POST) {
                continue;
            }
            empire.getBuildings().add(building);
        }
    }

    /** Same construction rules as {@link civ.model.factory.BuildingFactory} without events. */
    private static Building createBuildingSilent(BuildingType type, Hex hex) {
        Building building;
        if (type == BuildingType.TOWN_HALL) {
            building = new TownHall(hex);
        } else if (type == BuildingType.TRADING_POST) {
            building = new TradingPost(hex);
        } else if (type == BuildingType.OUTPOST) {
            building = new Outpost(hex);
        } else {
            building = new ProductionBuilding(type, hex);
        }
        hex.setBuilding(building);
        return building;
    }

    private static void restoreBuildingFields(Building building, SaveGame.BuildingData bd) {
        building.restoreHealth(bd.hp, bd.maxHp);
        building.setUnpaidTurns(bd.unpaidTurns);
        building.setPausedUntilTurn(bd.pausedUntilTurn);
    }

    private static Command restoreQueueCommand(SaveGame data) throws IOException {
        if ("TRAIN".equals(data.queueKind)) {
            return new TrainUnitCommand(UnitBlueprint.valueOf(data.queueTarget));
        }
        if ("RESEARCH".equals(data.queueKind)) {
            return new ResearchTechCommand(Tech.valueOf(data.queueTarget));
        }
        if ("UPGRADE".equals(data.queueKind)) {
            int level = Integer.parseInt(data.queueTarget);
            return new UpgradeTownHallCommand(TownHallLevel.of(level));
        }
        throw new IOException("Unknown queue kind: " + data.queueKind);
    }

    private static void applyEmpireUnits(Game game, Empire empire, SaveGame data,
                                       Map<Integer, Unit> unitsById) throws IOException {
        for (SaveGame.UnitData ud : data.units) {
            Unit unit = createPlayerUnit(ud);
            restoreUnitFields(unit, ud);
            empire.getUnits().add(unit);
            unitsById.put(ud.id, unit);
        }
    }

    private static void linkWorkerStations(SaveGame data, Map<Integer, Unit> unitsById,
                                           Map<Integer, Building> buildingsById) throws IOException {
        for (SaveGame.UnitData ud : data.units) {
            if (ud.stationBuildingId == null) {
                continue;
            }
            Unit unit = unitsById.get(ud.id);
            Building building = buildingsById.get(ud.stationBuildingId);
            if (!(unit instanceof Worker)) {
                throw new IOException("Non-worker has a station reference.");
            }
            if (!(building instanceof ProductionBuilding)) {
                throw new IOException("Worker station building is missing.");
            }
            ProductionBuilding production = (ProductionBuilding) building;
            production.addWorker((Worker) unit);
        }
    }

    private static void applyHostiles(Game game, SaveGame data,
                                      Map<Integer, Unit> unitsById) throws IOException {
        for (SaveGame.UnitData ud : data.hostiles) {
            MilitaryUnit unit = createHostileUnit(ud);
            restoreUnitFields(unit, ud);
            game.getHostiles().add(unit);
            unitsById.put(ud.id, unit);
        }
    }

    private static void applyTribes(Game game, SaveGame data,
                                    Map<Integer, Unit> unitsById) throws IOException {
        GameMap map = game.getMap();
        for (SaveGame.TribeData td : data.tribes) {
            Hex campHex = requireHex(map, td.campCol, td.campRow);
            TribeType type = TribeType.valueOf(td.type);
            Tribe tribe = new Tribe(td.name, type, campHex);

            tribe.setDiscovered(td.discovered);
            tribe.setDestroyed(td.destroyed);
            tribe.setTradeBonusPercent(td.tradeBonusPercent);
            tribe.setQuestBlockTurns(td.questBlockTurns);
            tribe.setTurnsSinceGuardSpawn(td.turnsSinceGuardSpawn);
            tribe.setTurnsSinceQuestOffer(td.turnsSinceQuestOffer);
            tribe.setRelationAbsolute(td.relation, null);
            tribe.setAllied(td.allied);

            if (td.questStatus != null && tribe.getQuest() != null) {
                tribe.getQuest().restore(
                        QuestStatus.valueOf(td.questStatus),
                        td.questTurnsLeft,
                        td.questProgress);
            }

            if (!td.destroyed) {
                TribeCamp camp = new TribeCamp(tribe, campHex);
                camp.restoreHealth(td.campHp, td.campMaxHp);
                tribe.setCamp(camp);
                campHex.setBuilding(camp);
            }

            for (SaveGame.UnitData gd : td.guards) {
                TribeGuard guard = new TribeGuard(tribe, gd.col, gd.row);
                restoreUnitFields(guard, gd);
                tribe.getGuards().add(guard);
                unitsById.put(gd.id, guard);
            }

            game.getTribes().add(tribe);
        }
    }

    private static Unit createPlayerUnit(SaveGame.UnitData ud) throws IOException {
        switch (ud.type) {
            case "Worker":
                return new Worker(ud.col, ud.row);
            case "Builder":
                return new Builder(ud.col, ud.row);
            case "Explorer":
                return new Explorer(ud.col, ud.row);
            case "Border Expander":
                return new BorderExpander(ud.col, ud.row);
            case "Swordsman":
                return new Swordsman(ud.col, ud.row);
            case "Archer":
                return new Archer(ud.col, ud.row);
            case "Cavalry":
                return new Cavalry(ud.col, ud.row);
            default:
                throw new IOException("Unknown unit type: " + ud.type);
        }
    }

    private static MilitaryUnit createHostileUnit(SaveGame.UnitData ud) throws IOException {
        switch (ud.type) {
            case "Barbarian":
                return new Barbarian(ud.col, ud.row);
            case "Wild Animal":
                return new WildAnimal(ud.col, ud.row);
            case "Bear":
                return new Bear(ud.col, ud.row);
            case "Tribe Guard":
                throw new IOException("Tribe Guard must be restored through TribeData.");
            default:
                throw new IOException("Unknown hostile type: " + ud.type);
        }
    }

    private static void restoreUnitFields(Unit unit, SaveGame.UnitData ud) {
        unit.setAp(ud.ap);
        restoreBodyHp(unit, ud.bodyHp, ud.maxBodyHp);
        if (unit instanceof MilitaryUnit && ud.combatHp != null) {
            MilitaryUnit military = (MilitaryUnit) unit;
            military.setCombatHp(ud.combatHp);
        }
    }

    private static void restoreBodyHp(Unit unit, int current, int max) {
        unit.restoreBodyHp(current, max);
    }

    private static SaveGame.BuildingData findBuilding(SaveGame data, String typeName) {
        for (SaveGame.BuildingData bd : data.buildings) {
            if (typeName.equals(bd.type)) {
                return bd;
            }
        }
        return null;
    }

    private static Hex requireHex(GameMap map, int col, int row) throws IOException {
        Hex hex = map.get(col, row);
        if (hex == null) {
            throw new IOException("Save references missing hex (" + col + ", " + row + ").");
        }
        return hex;
    }

    private static UnitBlueprint blueprintFromTrainLabel(String label) {
        String suffix = label.substring("Training ".length());
        for (UnitBlueprint blueprint : UnitBlueprint.values()) {
            if (blueprint.getLabel().equals(suffix)) {
                return blueprint;
            }
        }
        throw new IllegalStateException("Unknown training label: " + label);
    }

    private static Tech techFromResearchLabel(String label) {
        String suffix = label.substring("Researching ".length());
        for (Tech tech : Tech.values()) {
            if (tech.getLabel().equals(suffix)) {
                return tech;
            }
        }
        throw new IllegalStateException("Unknown research label: " + label);
    }

    private static TownHallLevel levelFromUpgradeLabel(String label) {
        String suffix = label.substring("Upgrading to ".length());
        for (TownHallLevel level : TownHallLevel.values()) {
            if (level.getLabel().equals(suffix)) {
                return level;
            }
        }
        throw new IllegalStateException("Unknown upgrade label: " + label);
    }
}
