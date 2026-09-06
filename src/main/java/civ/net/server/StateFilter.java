package civ.net.server;

import civ.model.Building;
import civ.model.Edge;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Player;
import civ.model.ProductionBuilding;
import civ.model.ResourceType;
import civ.model.Tech;
import civ.model.TownHall;
import civ.model.Unit;
import civ.model.Wall;
import civ.model.Worker;
import civ.model.trade.TradeOffer;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeCamp;
import civ.net.protocol.dto.GameStateDto;
import civ.net.protocol.push.GameStateBroadcast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds fog-filtered snapshots — the anti-cheat class. */
public final class StateFilter {

    private StateFilter() {
    }

    /** Builds one snapshot per player and sends each to its own client. */
    public static void broadcast(ServerSession session) {
        Game game = session.getGame();
        if (game == null) {
            return;
        }
        String mapName = session.getLobby().getSelectedMap();
        for (Player player : game.getPlayers()) {
            ClientHandler client = session.getClients().byPlayerId(player.getId());
            if (client == null) {
                continue;
            }
            client.send(new GameStateBroadcast(build(game, player, mapName)));
        }
    }

    public static GameStateDto build(Game game, Player viewer, String mapName) {
        GameStateDto dto = new GameStateDto();
        dto.turn = game.getTurn();
        dto.currentPlayerId = game.getCurrentPlayer().getId();
        dto.yourPlayerId = viewer.getId();
        dto.season = game.getSeason().name();
        dto.mapName = mapName;

        Set<Hex> known = discoveredBy(game, viewer);
        Set<Hex> visible = currentlyVisible(game, viewer);
        // Ally vision is temporary: include visible hexes for this snapshot only.
        known.addAll(visible);

        for (Hex hex : known) {
            GameStateDto.HexDto hexDto = new GameStateDto.HexDto();
            hexDto.col = hex.getCol();
            hexDto.row = hex.getRow();
            hexDto.terrain = hex.getTerrain().name();
            hexDto.deposit = hex.getDeposit() == null ? null : hex.getDeposit().name();
            hexDto.depositAmount = hex.getDepositAmount();
            hexDto.road = hex.hasRoad();
            hexDto.blocked = hex.isBlocked();
            hexDto.reserved = hex.isReserved();
            hexDto.currentlyVisible = visible.contains(hex);
            Player owner = game.ownerOf(hex);
            hexDto.ownerId = owner == null ? null : owner.getId();
            dto.hexes.add(hexDto);
        }

        for (Player owner : game.getPlayers()) {
            for (Unit unit : owner.getEmpire().getUnits()) {
                Hex hex = game.hexOf(unit);
                if (hex == null) {
                    continue;
                }
                boolean own = owner.getId() == viewer.getId();
                if (own || visible.contains(hex)) {
                    dto.units.add(toUnitDto(unit, owner.getId(), false));
                }
            }
        }
        for (MilitaryUnit hostile : game.getHostiles()) {
            Hex hex = game.hexOf(hostile);
            if (hex != null && visible.contains(hex)) {
                dto.units.add(toUnitDto(hostile, 0L, true));
            }
        }

        for (Player owner : game.getPlayers()) {
            for (Building building : owner.getEmpire().getBuildings()) {
                Hex hex = building.getHex();
                if (hex == null) {
                    continue;
                }
                boolean own = owner.getId() == viewer.getId();
                if (own || known.contains(hex)) {
                    dto.buildings.add(toBuildingDto(building, owner.getId()));
                }
            }
        }
        for (Hex hex : known) {
            Building building = hex.getBuilding();
            if (building != null && findBuilding(dto.buildings, building.getId()) == null) {
                dto.buildings.add(toBuildingDto(building, building.getOwnerId()));
            }
        }

        for (Edge edge : game.getMap().getEdges().all()) {
            Hex a = game.getMap().get(edge.getCol1(), edge.getRow1());
            Hex b = game.getMap().get(edge.getCol2(), edge.getRow2());
            if (a == null || b == null) {
                continue;
            }
            if (!known.contains(a) && !known.contains(b)) {
                continue;
            }
            if (!edge.hasRiver() && !edge.hasWall()) {
                continue;
            }
            GameStateDto.EdgeDto edgeDto = new GameStateDto.EdgeDto();
            edgeDto.col1 = edge.getCol1();
            edgeDto.row1 = edge.getRow1();
            edgeDto.col2 = edge.getCol2();
            edgeDto.row2 = edge.getRow2();
            edgeDto.river = edge.hasRiver();
            edgeDto.wall = edge.hasWall();
            Wall wall = edge.getWall();
            edgeDto.wallHp = wall == null ? 0 : wall.getHp();
            dto.edges.add(edgeDto);
        }

        for (Tribe tribe : game.getTribes()) {
            if (!tribe.isDiscovered() && !viewer.getFog().isDiscovered(tribe.getCampHex())) {
                continue;
            }
            if (!known.contains(tribe.getCampHex()) && !tribe.isDiscovered()) {
                continue;
            }
            // Only share tribe info if the camp hex is known (or already discovered by this viewer)
            if (!known.contains(tribe.getCampHex())) {
                continue;
            }
            GameStateDto.TribeDto tribeDto = new GameStateDto.TribeDto();
            tribeDto.id = tribe.getId();
            tribeDto.name = tribe.getName();
            tribeDto.type = tribe.getType().name();
            tribeDto.campCol = tribe.getCampHex().getCol();
            tribeDto.campRow = tribe.getCampHex().getRow();
            tribeDto.discovered = tribe.isDiscovered();
            tribeDto.destroyed = tribe.isDestroyed();
            tribeDto.relation = tribe.getRelation();
            TribeCamp camp = tribe.getCamp();
            if (camp != null) {
                tribeDto.campHp = camp.getHp();
                tribeDto.campMaxHp = camp.getMaxHp();
            }
            dto.tribes.add(tribeDto);
        }

        dto.yourStock = toStock(viewer.getEmpire().getStock());
        dto.yourUnitCap = viewer.getEmpire().getUnitCap();
        dto.yourHappiness = viewer.getEmpire().happiness();
        dto.yourStarving = viewer.isStarving();
        for (Tech tech : viewer.getEmpire().getTechs()) {
            dto.yourTechs.add(tech.name());
        }

        dto.yourInbox = toTradeDtos(game, game.getTradeOffers().pendingFor(viewer));
        dto.yourOutgoing = toTradeDtos(game, game.getTradeOffers().pendingFrom(viewer));

        dto.players = publicPlayerInfo(game, viewer);
        dto.log = new ArrayList<>(game.getLog());
        return dto;
    }

    private static GameStateDto.BuildingDto findBuilding(List<GameStateDto.BuildingDto> list, long id) {
        for (GameStateDto.BuildingDto dto : list) {
            if (dto.id == id) {
                return dto;
            }
        }
        return null;
    }

    private static Set<Hex> discoveredBy(Game game, Player viewer) {
        Set<Hex> known = new HashSet<>();
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex != null && viewer.getFog().isDiscovered(hex)) {
                    known.add(hex);
                }
            }
        }
        return known;
    }

    /** Your own vision, plus every ally's vision — shared sight without mutating Fog. */
    private static Set<Hex> currentlyVisible(Game game, Player viewer) {
        Set<Hex> visible = new HashSet<>();
        for (Player player : game.getPlayers()) {
            boolean self = player.getId() == viewer.getId();
            boolean ally = game.getDiplomacy().sharesVision(viewer, player);
            if (!self && !ally) {
                continue;
            }
            for (Unit unit : player.getEmpire().getUnits()) {
                Hex centre = game.hexOf(unit);
                if (centre != null) {
                    visible.addAll(game.getMap().withinRange(centre, unit.getVisionRadius()));
                }
            }
            for (Building building : player.getEmpire().getBuildings()) {
                visible.addAll(game.getMap().withinRange(building.getHex(), 2));
            }
        }
        return visible;
    }

    private static GameStateDto.UnitDto toUnitDto(Unit unit, long ownerId, boolean hostile) {
        GameStateDto.UnitDto dto = new GameStateDto.UnitDto();
        dto.id = unit.getId();
        dto.createdAt = unit.getCreatedAt();
        dto.ownerId = ownerId;
        dto.type = unit.getTypeName();
        dto.col = unit.getCol();
        dto.row = unit.getRow();
        dto.ap = unit.getAp();
        dto.maxAp = unit.getMaxAp();
        dto.hp = unit.getBodyHp();
        dto.maxHp = unit.getMaxBodyHp();
        if (unit instanceof MilitaryUnit) {
            MilitaryUnit military = (MilitaryUnit) unit;
            dto.combatHp = military.getCombatHp();
            dto.maxCombatHp = military.getMaxCombatHp();
        }
        if (unit instanceof civ.model.Builder) {
            dto.charges = ((civ.model.Builder) unit).getCharges();
        }
        if (unit instanceof Worker) {
            dto.stationed = ((Worker) unit).isBusy();
        }
        dto.hostile = hostile;
        return dto;
    }

    private static GameStateDto.BuildingDto toBuildingDto(Building building, long ownerId) {
        GameStateDto.BuildingDto dto = new GameStateDto.BuildingDto();
        dto.id = building.getId();
        dto.createdAt = building.getCreatedAt();
        dto.ownerId = ownerId;
        dto.type = building.getType().name();
        dto.col = building.getHex().getCol();
        dto.row = building.getHex().getRow();
        dto.hp = building.getHp();
        dto.maxHp = building.getMaxHp();
        if (building instanceof TownHall) {
            TownHall hall = (TownHall) building;
            dto.level = hall.getLevel();
            dto.defensiveWall = hall.hasDefensiveWall();
            dto.queue = hall.describeQueue();
        }
        if (building instanceof ProductionBuilding) {
            for (Worker worker : ((ProductionBuilding) building).getWorkers()) {
                dto.workerIds.add(worker.getId());
            }
        }
        return dto;
    }

    private static GameStateDto.StockDto toStock(civ.model.Stockpile stock) {
        GameStateDto.StockDto dto = new GameStateDto.StockDto();
        dto.food = stock.get(ResourceType.FOOD);
        dto.wood = stock.get(ResourceType.WOOD);
        dto.stone = stock.get(ResourceType.STONE);
        dto.iron = stock.get(ResourceType.IRON);
        dto.lockedFood = stock.getLocked(ResourceType.FOOD);
        dto.lockedWood = stock.getLocked(ResourceType.WOOD);
        dto.lockedStone = stock.getLocked(ResourceType.STONE);
        dto.lockedIron = stock.getLocked(ResourceType.IRON);
        dto.capacity = stock.getCapacity();
        return dto;
    }

    private static List<GameStateDto.TradeOfferDto> toTradeDtos(Game game, List<TradeOffer> offers) {
        List<GameStateDto.TradeOfferDto> list = new ArrayList<>();
        for (TradeOffer offer : offers) {
            GameStateDto.TradeOfferDto dto = new GameStateDto.TradeOfferDto();
            dto.id = offer.getId();
            dto.fromPlayerId = offer.getFromPlayerId();
            dto.toPlayerId = offer.getToPlayerId();
            Player from = game.getPlayer(offer.getFromPlayerId());
            Player to = game.getPlayer(offer.getToPlayerId());
            dto.fromPlayerName = from == null ? "?" : from.getName();
            dto.toPlayerName = to == null ? "?" : to.getName();
            dto.offerFood = offer.offered(ResourceType.FOOD);
            dto.offerWood = offer.offered(ResourceType.WOOD);
            dto.offerStone = offer.offered(ResourceType.STONE);
            dto.offerIron = offer.offered(ResourceType.IRON);
            dto.askFood = offer.requested(ResourceType.FOOD);
            dto.askWood = offer.requested(ResourceType.WOOD);
            dto.askStone = offer.requested(ResourceType.STONE);
            dto.askIron = offer.requested(ResourceType.IRON);
            list.add(dto);
        }
        return list;
    }

    private static List<GameStateDto.PlayerDto> publicPlayerInfo(Game game, Player viewer) {
        List<GameStateDto.PlayerDto> list = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            GameStateDto.PlayerDto dto = new GameStateDto.PlayerDto();
            dto.id = player.getId();
            dto.createdAt = player.getCreatedAt();
            dto.name = player.getName();
            dto.colour = player.getColour().name();
            dto.alive = player.isAlive();
            dto.connected = player.isConnected();
            dto.diplomacyWithYou = game.getDiplomacy().between(viewer, player).name();
            list.add(dto);
        }
        return list;
    }
}
