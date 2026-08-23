package civ.model.world;

import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Edge;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.ProductionBuilding;
import civ.model.TownHall;
import civ.model.Unit;
import java.util.ArrayList;
import java.util.List;

/**
 * Autumn-only flood near river or coast: −20 body HP, AP wiped, roads/farms gone,
 * other buildings −30 HP and paused until end of next turn.
 */
public class Flood implements Disaster {

    @Override
    public String getName() {
        return "Flood";
    }

    @Override
    public boolean canHappen(Game game) {
        if (Season.forTurn(game.getTurn()) != Season.AUTUMN) {
            return false;
        }
        return !floodableHexes(game).isEmpty();
    }

    @Override
    public void apply(Game game) {
        List<Hex> seeds = floodableHexes(game);
        Hex start = seeds.get(game.getRandom().nextInt(seeds.size()));
        List<Hex> affected = new ArrayList<>();
        affected.add(start);
        for (Hex neighbour : game.getMap().neighbours(start)) {
            if (neighbour.getTerrain().isLand()) {
                affected.add(neighbour);
            }
        }

        int pauseUntil = game.getTurn() + 1;
        for (Hex hex : affected) {
            for (Unit unit : new ArrayList<>(game.unitsAt(hex))) {
                unit.damageBody(20);
                unit.emptyAp();
                if (unit.isBodyDead()) {
                    game.killUnit(unit, "swept away by the flood");
                }
            }
            for (MilitaryUnit hostile : new ArrayList<>(game.hostilesAt(hex))) {
                hostile.damageBody(20);
                hostile.emptyAp();
                if (hostile.isBodyDead()) {
                    game.buryUnit(hostile);
                }
            }

            hex.setRoad(false);
            Building building = hex.getBuilding();
            if (building == null) {
                continue;
            }
            if (building.getType() == BuildingType.FARM) {
                if (building instanceof ProductionBuilding) {
                    ((ProductionBuilding) building).releaseAllWorkers();
                }
                game.destroyBuilding(building, "washed away by the flood");
                continue;
            }
            if (building instanceof TownHall) {
                int hp = building.getHp();
                building.damage(Math.min(30, Math.max(0, hp - 1)));
            } else {
                building.damage(30);
                if (building.isDestroyed()) {
                    game.destroyBuilding(building, "destroyed by the flood");
                } else {
                    building.pauseUntil(pauseUntil);
                }
            }
        }

        boolean visible = false;
        for (Hex hex : affected) {
            if (hex.isDiscovered()) {
                visible = true;
                break;
            }
        }
        game.setLastDisaster(new DisasterEffect(getName(), start, affected, visible));
        game.addLog(visible
                ? "Flood waters rise near (" + start.getCol() + "," + start.getRow() + ")!"
                : "Distant floods are reported beyond the fog.");
    }

    private List<Hex> floodableHexes(Game game) {
        List<Hex> result = new ArrayList<>();
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex == null || !hex.getTerrain().isLand()) {
                    continue;
                }
                if (touchesRiverOrCoast(game, hex)) {
                    result.add(hex);
                }
            }
        }
        return result;
    }

    private boolean touchesRiverOrCoast(Game game, Hex hex) {
        if (game.isCoastal(hex)) {
            return true;
        }
        for (Hex neighbour : game.getMap().neighbours(hex)) {
            Edge edge = game.getMap().getEdges().find(hex, neighbour);
            if (edge != null && edge.hasRiver()) {
                return true;
            }
        }
        return false;
    }
}
