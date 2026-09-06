package civ.net.client;

import civ.model.Archer;
import civ.model.Barbarian;
import civ.model.Bear;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Cavalry;
import civ.model.Edge;
import civ.model.Empire;
import civ.model.Explorer;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Outpost;
import civ.model.Player;
import civ.model.PlayerColour;
import civ.model.ProductionBuilding;
import civ.model.ResourceType;
import civ.model.Swordsman;
import civ.model.Tech;
import civ.model.TownHall;
import civ.model.TradingPost;
import civ.model.Unit;
import civ.model.Wall;
import civ.model.Worker;
import civ.model.map.MapPreset;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeCamp;
import civ.model.tribe.TribeType;
import civ.net.protocol.dto.GameStateDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Rebuilds the local throwaway Game from a fog-filtered snapshot. */
public final class SnapshotApplier {

    private SnapshotApplier() {
    }

    public static Game createShell(GameStateDto first) throws IOException {
        if (first.mapName == null || first.mapName.isEmpty()) {
            throw new IOException("Snapshot has no map name.");
        }
        MapPreset preset = new MapPreset(first.mapName);
        Game game = new Game(preset, new ArrayList<>());
        for (GameStateDto.PlayerDto dto : first.players) {
            PlayerColour colour = PlayerColour.valueOf(dto.colour);
            game.getPlayers().add(new Player(dto.id, dto.createdAt, dto.name, colour, game.getMap()));
        }
        apply(game, first);
        return game;
    }

    public static void apply(Game game, GameStateDto dto) {
        long selectedId = game.getSelected() == null ? -1L : game.getSelected().getId();

        clearDynamicState(game);

        for (GameStateDto.PlayerDto playerDto : dto.players) {
            Player player = game.getPlayer(playerDto.id);
            if (player == null) {
                continue;
            }
            if (playerDto.alive) {
                // Player.eliminate is one-way; for dead players we leave as-is after first elim
            } else if (player.isAlive()) {
                player.eliminate();
            }
            player.setConnected(playerDto.connected);
        }

        Player viewer = game.getPlayer(dto.yourPlayerId);
        if (viewer != null) {
            game.setViewpointPlayerId(viewer.getId());
            viewer.getEmpire().setUnitCap(dto.yourUnitCap);
            viewer.getEmpire().getHappiness().setRawValue(dto.yourHappiness);
            viewer.setStarving(dto.yourStarving);
            viewer.getEmpire().getTechs().clear();
            if (dto.yourTechs != null) {
                for (String name : dto.yourTechs) {
                    try {
                        viewer.getEmpire().getTechs().add(Tech.valueOf(name));
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            applyStock(viewer.getEmpire(), dto.yourStock);
        }

        game.getDiplomacy().clear();
        if (viewer != null) {
            for (GameStateDto.PlayerDto playerDto : dto.players) {
                Player other = game.getPlayer(playerDto.id);
                if (other == null || other.getId() == viewer.getId()) {
                    continue;
                }
                try {
                    game.getDiplomacy().set(viewer, other,
                            civ.model.diplomacy.DiplomaticState.valueOf(playerDto.diplomacyWithYou));
                } catch (RuntimeException ignored) {
                }
            }
        }

        applyTradeOffers(game, dto);

        // Fog + hex cosmetics for the viewer
        if (viewer != null) {
            for (int col = 0; col < game.getMap().getCols(); col++) {
                for (int row = 0; row < game.getMap().getRows(); row++) {
                    Hex hex = game.getMap().get(col, row);
                    if (hex != null) {
                        viewer.getFog().setDiscovered(hex, false);
                        viewer.getFog().setOwned(hex, false);
                    }
                }
            }
        }

        for (GameStateDto.HexDto hexDto : dto.hexes) {
            Hex hex = game.getMap().get(hexDto.col, hexDto.row);
            if (hex == null) {
                continue;
            }
            hex.setRoad(hexDto.road);
            hex.setBlocked(hexDto.blocked);
            hex.setReserved(hexDto.reserved);
            hex.setDepositAmount(hexDto.depositAmount);
            if (viewer != null) {
                viewer.getFog().setDiscovered(hex, true);
            }
            if (hexDto.ownerId != null) {
                Player owner = game.getPlayer(hexDto.ownerId);
                if (owner != null) {
                    owner.getFog().setOwned(hex, true);
                    if (viewer != null && owner.getId() != viewer.getId()) {
                        // remembered ownership outline for other players
                        viewer.getFog().setDiscovered(hex, true);
                    }
                }
            }
        }

        Map<Long, Building> buildingsById = new HashMap<>();
        for (GameStateDto.BuildingDto buildingDto : dto.buildings) {
            Building building = createBuilding(game, buildingDto);
            if (building == null) {
                continue;
            }
            buildingsById.put(building.getId(), building);
            Player owner = game.getPlayer(buildingDto.ownerId);
            if (owner != null) {
                building.setOwnerId(owner.getId());
                if (building instanceof TownHall) {
                    owner.getEmpire().addTownHall((TownHall) building);
                } else {
                    owner.getEmpire().getBuildings().add(building);
                }
            }
        }

        for (GameStateDto.UnitDto unitDto : dto.units) {
            Unit unit = createUnit(unitDto);
            if (unit == null) {
                continue;
            }
            unit.setAp(unitDto.ap);
            unit.restoreBodyHp(unitDto.hp, unitDto.maxHp);
            if (unit instanceof MilitaryUnit) {
                ((MilitaryUnit) unit).setCombatHp(unitDto.combatHp);
            }
            if (unit instanceof Builder && unitDto.charges >= 0) {
                ((Builder) unit).setCharges(unitDto.charges);
            }
            if (unitDto.hostile || unitDto.ownerId == 0L) {
                if (unit instanceof MilitaryUnit) {
                    game.getHostiles().add((MilitaryUnit) unit);
                }
            } else {
                Player owner = game.getPlayer(unitDto.ownerId);
                if (owner != null) {
                    unit.setOwnerId(owner.getId());
                    owner.getEmpire().getUnits().add(unit);
                }
            }
        }

        // Re-link stationed workers after both sides exist
        for (GameStateDto.BuildingDto buildingDto : dto.buildings) {
            Building building = buildingsById.get(buildingDto.id);
            if (!(building instanceof ProductionBuilding) || buildingDto.workerIds == null) {
                continue;
            }
            ProductionBuilding production = (ProductionBuilding) building;
            for (Long workerId : buildingDto.workerIds) {
                Unit unit = game.findUnit(workerId);
                if (unit instanceof Worker) {
                    production.addWorker((Worker) unit);
                }
            }
        }

        for (GameStateDto.EdgeDto edgeDto : dto.edges) {
            Hex a = game.getMap().get(edgeDto.col1, edgeDto.row1);
            Hex b = game.getMap().get(edgeDto.col2, edgeDto.row2);
            if (a == null || b == null) {
                continue;
            }
            Edge edge = game.getMap().getEdges().getOrCreate(a, b);
            edge.setRiver(edgeDto.river);
            if (edgeDto.wall) {
                edge.setWall(new Wall(edgeDto.wallHp));
            } else {
                edge.setWall(null);
            }
        }

        for (GameStateDto.TribeDto tribeDto : dto.tribes) {
            Hex campHex = game.getMap().get(tribeDto.campCol, tribeDto.campRow);
            if (campHex == null) {
                continue;
            }
            TribeType type;
            try {
                type = TribeType.valueOf(tribeDto.type);
            } catch (RuntimeException ex) {
                continue;
            }
            Tribe tribe = new Tribe(tribeDto.id, 0L, tribeDto.name, type, campHex);
            tribe.setDiscovered(tribeDto.discovered);
            tribe.setDestroyed(tribeDto.destroyed);
            tribe.setRelationAbsolute(tribeDto.relation, null);
            if (campHex.getBuilding() == null) {
                TribeCamp camp = new TribeCamp(tribe, campHex);
                camp.restoreHealth(tribeDto.campHp, tribeDto.campMaxHp);
                campHex.setBuilding(camp);
                tribe.setCamp(camp);
            } else if (campHex.getBuilding() instanceof TribeCamp) {
                TribeCamp camp = (TribeCamp) campHex.getBuilding();
                camp.restoreHealth(tribeDto.campHp, tribeDto.campMaxHp);
                tribe.setCamp(camp);
            }
            game.getTribes().add(tribe);
        }

        game.setTurn(dto.turn);
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getId() == dto.currentPlayerId) {
                game.setCurrentPlayerIndex(i);
                break;
            }
        }

        game.getLog().clear();
        if (dto.log != null) {
            game.getLog().addAll(dto.log);
        }

        if (selectedId > 0) {
            Unit again = game.findUnit(selectedId);
            game.select(again);
        } else {
            game.select(null);
        }
    }

    private static void clearDynamicState(Game game) {
        for (Player player : game.getPlayers()) {
            for (Building building : new ArrayList<>(player.getEmpire().getBuildings())) {
                if (building.getHex() != null) {
                    building.getHex().setBuilding(null);
                }
            }
            player.getEmpire().getBuildings().clear();
            player.getEmpire().getUnits().clear();
            player.getEmpire().setTownHall(null);
            for (int col = 0; col < game.getMap().getCols(); col++) {
                for (int row = 0; row < game.getMap().getRows(); row++) {
                    Hex hex = game.getMap().get(col, row);
                    if (hex != null) {
                        player.getFog().setOwned(hex, false);
                    }
                }
            }
        }
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex != null) {
                    hex.setBuilding(null);
                    hex.setRoad(false);
                    hex.setBlocked(false);
                    hex.setReserved(false);
                }
            }
        }
        for (Edge edge : game.getMap().getEdges().all()) {
            edge.setWall(null);
        }
        game.getHostiles().clear();
        game.getTribes().clear();
        game.select(null);
    }

    private static void applyStock(Empire empire, GameStateDto.StockDto stock) {
        if (stock == null) {
            return;
        }
        empire.getStock().setCapacity(stock.capacity);
        empire.getStock().set(ResourceType.FOOD, stock.food);
        empire.getStock().set(ResourceType.WOOD, stock.wood);
        empire.getStock().set(ResourceType.STONE, stock.stone);
        empire.getStock().set(ResourceType.IRON, stock.iron);
        empire.getStock().setLocked(ResourceType.FOOD, stock.lockedFood);
        empire.getStock().setLocked(ResourceType.WOOD, stock.lockedWood);
        empire.getStock().setLocked(ResourceType.STONE, stock.lockedStone);
        empire.getStock().setLocked(ResourceType.IRON, stock.lockedIron);
    }

    private static void applyTradeOffers(Game game, GameStateDto dto) {
        // Client only keeps pending offers visible to this player for UI.
        // Clear by cancelling status is awkward — rebuild via reflection of ids isn't needed:
        // we store inbox DTOs on a side list? Simpler: clear tradeOffers list via cancel-all
        // isn't available. Add a clear method.
        game.getTradeOffers().clear();
        if (dto.yourInbox != null) {
            for (GameStateDto.TradeOfferDto offerDto : dto.yourInbox) {
                game.getTradeOffers().add(fromDto(offerDto));
            }
        }
        if (dto.yourOutgoing != null) {
            for (GameStateDto.TradeOfferDto offerDto : dto.yourOutgoing) {
                if (game.getTradeOffers().byId(offerDto.id) == null) {
                    game.getTradeOffers().add(fromDto(offerDto));
                }
            }
        }
    }

    private static civ.model.trade.TradeOffer fromDto(GameStateDto.TradeOfferDto dto) {
        civ.model.trade.TradeOffer offer = new civ.model.trade.TradeOffer(
                dto.id, 0L, dto.fromPlayerId, dto.toPlayerId);
        offer.getOffered().put(ResourceType.FOOD, dto.offerFood);
        offer.getOffered().put(ResourceType.WOOD, dto.offerWood);
        offer.getOffered().put(ResourceType.STONE, dto.offerStone);
        offer.getOffered().put(ResourceType.IRON, dto.offerIron);
        offer.getRequested().put(ResourceType.FOOD, dto.askFood);
        offer.getRequested().put(ResourceType.WOOD, dto.askWood);
        offer.getRequested().put(ResourceType.STONE, dto.askStone);
        offer.getRequested().put(ResourceType.IRON, dto.askIron);
        return offer;
    }

    private static Building createBuilding(Game game, GameStateDto.BuildingDto dto) {
        Hex hex = game.getMap().get(dto.col, dto.row);
        if (hex == null) {
            return null;
        }
        BuildingType type;
        try {
            type = BuildingType.valueOf(dto.type);
        } catch (RuntimeException ex) {
            return null;
        }
        Building building;
        if (type == BuildingType.TOWN_HALL) {
            TownHall hall = new TownHall(dto.id, dto.createdAt, hex);
            hall.restoreState(Math.max(1, dto.level), dto.hp, dto.maxHp,
                    dto.defensiveWall ? 30 : 10, dto.defensiveWall);
            building = hall;
        } else if (type == BuildingType.TRADING_POST) {
            building = new TradingPost(dto.id, dto.createdAt, hex);
            building.restoreHealth(dto.hp, dto.maxHp);
        } else if (type == BuildingType.OUTPOST) {
            building = new Outpost(dto.id, dto.createdAt, hex);
            building.restoreHealth(dto.hp, dto.maxHp);
        } else if (type == BuildingType.TRIBE_CAMP) {
            // tribe wiring happens in the tribe loop
            return null;
        } else {
            building = new ProductionBuilding(dto.id, dto.createdAt, type, hex);
            building.restoreHealth(dto.hp, dto.maxHp);
        }
        hex.setBuilding(building);
        return building;
    }

    private static Unit createUnit(GameStateDto.UnitDto dto) {
        String type = dto.type;
        long id = dto.id;
        long createdAt = dto.createdAt;
        int col = dto.col;
        int row = dto.row;
        if ("Worker".equals(type)) {
            return new Worker(id, createdAt, col, row);
        }
        if ("Builder".equals(type)) {
            return new Builder(id, createdAt, col, row);
        }
        if ("Explorer".equals(type)) {
            return new Explorer(id, createdAt, col, row);
        }
        if ("Border Expander".equals(type)) {
            return new BorderExpander(id, createdAt, col, row);
        }
        if ("Swordsman".equals(type)) {
            return new Swordsman(id, createdAt, col, row);
        }
        if ("Archer".equals(type)) {
            return new Archer(id, createdAt, col, row);
        }
        if ("Cavalry".equals(type)) {
            return new Cavalry(id, createdAt, col, row);
        }
        if ("Barbarian".equals(type)) {
            return new Barbarian(id, createdAt, col, row);
        }
        if ("Bear".equals(type)) {
            return new Bear(id, createdAt, col, row);
        }
        return null;
    }
}
