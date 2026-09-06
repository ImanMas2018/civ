package civ.model.tribe;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Terrain;
import civ.util.HexGeometry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TribePlacer {

    private static final String[] NAMES = {
            "Riverfolk", "Iron Wolves", "Silk Caravan", "Peak Kin", "Salt Haven"
    };

    private final Random random;

    public TribePlacer(Random random) {
        this.random = random;
    }

    public List<Tribe> place(Game game, int centreCol, int centreRow) {
        List<Tribe> tribes = new ArrayList<>();
        TribeType[] types = TribeType.values();
        for (int i = 0; i < types.length; i++) {
            Hex hex = pickHex(game, centreCol, centreRow, types[i], tribes);
            if (hex == null) {
                continue;
            }
            Tribe tribe = new Tribe(NAMES[i], types[i], hex);
            TribeCamp camp = new TribeCamp(tribe, hex);
            tribe.setCamp(camp);
            hex.setBuilding(camp);
            // Non-player territory — reserved, not player-owned fog.
            hex.setReserved(true);
            for (Hex neighbour : game.getMap().neighbours(hex)) {
                if (neighbour.getTerrain().isLand() && !game.isClaimed(neighbour)) {
                    neighbour.setReserved(true);
                }
            }
            int guards = types[i] == TribeType.WARRIOR ? 2 : 1;
            for (int g = 0; g < guards; g++) {
                tribe.getGuards().add(new TribeGuard(tribe, hex.getCol(), hex.getRow()));
            }
            tribes.add(tribe);
        }
        return tribes;
    }

    private Hex pickHex(Game game, int centreCol, int centreRow,
                        TribeType type, List<Tribe> placed) {
        List<Hex> candidates = new ArrayList<>();
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || game.isClaimed(hex)) {
                    continue;
                }
                if (hex.getBuilding() != null) {
                    continue;
                }
                int distance = HexGeometry.distance(centreCol, centreRow, col, row);
                if (distance < 6) {
                    continue;
                }
                if (tooCloseToOthers(hex, placed)) {
                    continue;
                }
                if (type == TribeType.COASTAL && !isCoastal(game, hex)) {
                    continue;
                }
                if (type == TribeType.MOUNTAIN && hex.getTerrain() != Terrain.MOUNTAIN
                        && !nearMountain(game, hex)) {
                    continue;
                }
                if ((type == TribeType.FARMER || type == TribeType.COASTAL)
                        && hex.getTerrain() != Terrain.GRASSLAND
                        && hex.getTerrain() != Terrain.PLAINS) {
                    continue;
                }
                candidates.add(hex);
            }
        }
        if (candidates.isEmpty()) {
            return fallback(game, centreCol, centreRow, placed);
        }
        Collections.shuffle(candidates, random);
        Terrain preferred = type.getPreferredTerrain();
        for (Hex hex : candidates) {
            if (hex.getTerrain() == preferred) {
                return hex;
            }
        }
        return candidates.get(0);
    }

    private Hex fallback(Game game, int centreCol, int centreRow, List<Tribe> placed) {
        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                Hex hex = game.getMap().get(col, row);
                if (hex == null || !hex.getTerrain().isLand() || game.isClaimed(hex)) {
                    continue;
                }
                if (hex.getBuilding() != null) {
                    continue;
                }
                if (HexGeometry.distance(centreCol, centreRow, col, row) < 6) {
                    continue;
                }
                if (tooCloseToOthers(hex, placed)) {
                    continue;
                }
                return hex;
            }
        }
        return null;
    }

    private boolean tooCloseToOthers(Hex hex, List<Tribe> placed) {
        for (Tribe tribe : placed) {
            Hex camp = tribe.getCampHex();
            if (HexGeometry.distance(hex.getCol(), hex.getRow(), camp.getCol(), camp.getRow()) < 4) {
                return true;
            }
        }
        return false;
    }

    private boolean isCoastal(Game game, Hex hex) {
        for (Hex neighbour : game.getMap().neighbours(hex)) {
            if (neighbour.getTerrain().isSea()) {
                return true;
            }
        }
        return false;
    }

    private boolean nearMountain(Game game, Hex hex) {
        if (hex.getTerrain() == Terrain.MOUNTAIN) {
            return true;
        }
        for (Hex neighbour : game.getMap().neighbours(hex)) {
            if (neighbour.getTerrain() == Terrain.MOUNTAIN) {
                return true;
            }
        }
        return false;
    }
}
