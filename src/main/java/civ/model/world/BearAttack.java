package civ.model.world;

import civ.model.Bear;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Terrain;
import java.util.ArrayList;
import java.util.List;

/**
 * Bears emerge from a forest. Prefer civilians, never hit buildings, leave when no
 * target remains within 3 hexes. Cooldown 5 turns; second bear if 3+ military near den.
 */
public class BearAttack implements Disaster {

    @Override
    public String getName() {
        return "Bear Attack";
    }

    @Override
    public boolean canHappen(Game game) {
        if (game.getTurn() - game.getLastBearTurn() < 5) {
            return false;
        }
        return !forestDens(game).isEmpty();
    }

    @Override
    public void apply(Game game) {
        List<Hex> dens = forestDens(game);
        Hex den = dens.get(game.getRandom().nextInt(dens.size()));

        int count = 1;
        if (game.playerMilitaryNear(den, 3) >= 3) {
            count = 2;
        }
        count = Math.min(2, count);

        List<Hex> spawnHexes = new ArrayList<>();
        spawnHexes.add(den);
        List<Bear> bears = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Hex spawn = pickSpawn(game, den, spawnHexes);
            if (spawn == null) {
                break;
            }
            Bear bear = new Bear(spawn.getCol(), spawn.getRow());
            game.addHostile(bear);
            bears.add(bear);
            spawnHexes.add(spawn);
            spawn.setDiscovered(true);
        }

        game.setLastBearTurn(game.getTurn());
        game.runBearBehaviour();

        boolean visible = den.isDiscovered();
        for (Hex hex : spawnHexes) {
            if (hex.isDiscovered()) {
                visible = true;
            }
        }
        game.setLastDisaster(new DisasterEffect(getName(), den, spawnHexes, visible));
        game.addLog(visible
                ? "Bears emerge from the forest at (" + den.getCol() + "," + den.getRow() + ")!"
                : "Growls echo from an undiscovered forest.");
    }

    private List<Hex> forestDens(Game game) {
        List<Hex> dens = new ArrayList<>();
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex != null && hex.getTerrain() == Terrain.FOREST
                        && hex.getBuilding() == null
                        && game.hostilesAt(hex).isEmpty()) {
                    dens.add(hex);
                }
            }
        }
        return dens;
    }

    private Hex pickSpawn(Game game, Hex den, List<Hex> used) {
        List<Hex> options = new ArrayList<>();
        for (Hex hex : game.getMap().withinRange(den, 1)) {
            if (!hex.getTerrain().isLand() || hex.isBlocked()) {
                continue;
            }
            if (hex.getBuilding() != null) {
                continue;
            }
            if (!game.hostilesAt(hex).isEmpty()) {
                continue;
            }
            if (used.contains(hex)) {
                continue;
            }
            options.add(hex);
        }
        if (options.isEmpty()) {
            return used.contains(den) ? null : den;
        }
        return options.get(game.getRandom().nextInt(options.size()));
    }
}
