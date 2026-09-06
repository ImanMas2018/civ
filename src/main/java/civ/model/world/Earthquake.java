package civ.model.world;

import civ.model.Building;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.TownHall;
import civ.model.Unit;
import java.util.ArrayList;
import java.util.List;

public class Earthquake implements Disaster {

    @Override
    public String getName() {
        return "Earthquake";
    }

    @Override
    public boolean canHappen(Game game) {
        return true;
    }

    @Override
    public void apply(Game game) {
        Hex centre = pickEpicenter(game);
        List<Hex> affected = new ArrayList<>();
        for (Hex hex : game.getMap().withinRange(centre, 2)) {
            affected.add(hex);
        }

        for (Hex hex : affected) {
            for (Unit unit : new ArrayList<>(game.unitsAt(hex))) {
                unit.damageBody(10);
                if (unit.isBodyDead()) {
                    game.killUnit(unit, "crushed by the earthquake");
                }
            }
            for (civ.model.MilitaryUnit hostile : new ArrayList<>(game.hostilesAt(hex))) {
                hostile.damageBody(10);
                if (hostile.isBodyDead()) {
                    game.buryUnit(hostile);
                }
            }
            Building building = hex.getBuilding();
            if (building instanceof TownHall) {
                int hp = building.getHp();
                building.damage(Math.min(50, Math.max(0, hp - 1)));
            } else if (building != null) {
                building.damage(20);
                if (building.isDestroyed()) {
                    game.destroyBuilding(building, "collapsed in the earthquake");
                }
            }
            if (hex.getTerrain().isLand() && hex.getBuilding() == null
                    && game.getRandom().nextBoolean()) {
                hex.setBlocked(true);
            }
        }

        boolean visible = false;
        for (Hex hex : affected) {
            if (game.isDiscovered(game.getCurrentPlayer(), hex)) {
                visible = true;
                break;
            }
        }
        game.setLastDisaster(new DisasterEffect(getName(), centre, affected, visible));
        game.addLog(visible
                ? "Earthquake near (" + centre.getCol() + "," + centre.getRow() + ")!"
                : "Rumours of an earthquake in unexplored lands.");
    }

    private Hex pickEpicenter(Game game) {
        List<Hex> land = new ArrayList<>();
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex != null && hex.getTerrain().isLand()) {
                    land.add(hex);
                }
            }
        }
        if (land.isEmpty()) {
            return game.getMap().get(game.getCentreCol(), game.getCentreRow());
        }
        return land.get(game.getRandom().nextInt(land.size()));
    }
}
