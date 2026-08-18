package civ.model;

import civ.util.HexGeometry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Fills a GameMap with terrain, coasts, mountain ranges and river edges.
 * The starting ring stays open land so the player is never boxed in.
 */
public class MapGenerator {

    private final Random random;

    public MapGenerator(long seed) {
        this.random = new Random(seed);
    }

    public void fill(GameMap map, int centreCol, int centreRow) {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                map.set(createLandHex(col, row));
            }
        }
        paintCoastsAndLakes(map, centreCol, centreRow);
        paintMountainRanges(map, centreCol, centreRow);
        keepStartOpen(map, centreCol, centreRow);
        guaranteeForestNear(map, centreCol, centreRow);
        guaranteeFarmNear(map, centreCol, centreRow);
        guaranteeNearbySea(map, centreCol, centreRow);
        paintRivers(map, centreCol, centreRow);
        ensureReachable(map, centreCol, centreRow);
    }

    private Hex createLandHex(int col, int row) {
        int roll = random.nextInt(100);

        if (roll < 28) {
            return new Hex(col, row, Terrain.FOREST, ResourceType.WOOD, 60 + random.nextInt(40));
        }
        if (roll < 46) {
            boolean iron = random.nextInt(100) < 30;
            ResourceType deposit = iron ? ResourceType.IRON : ResourceType.STONE;
            return new Hex(col, row, Terrain.MOUNTAIN, deposit, 40 + random.nextInt(40));
        }
        if (roll < 72) {
            boolean food = random.nextInt(100) < 40;
            return new Hex(col, row, Terrain.GRASSLAND,
                    food ? ResourceType.FOOD : null,
                    food ? 80 + random.nextInt(40) : 0);
        }
        boolean animals = random.nextInt(100) < 30;
        return new Hex(col, row, Terrain.PLAINS,
                animals ? ResourceType.FOOD : null,
                animals ? 60 + random.nextInt(30) : 0);
    }

    private void paintCoastsAndLakes(GameMap map, int centreCol, int centreRow) {
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                int dist = HexGeometry.distance(centreCol, centreRow, col, row);
                if (dist <= 3) {
                    continue;
                }
                boolean rim = col <= 1 || col >= map.getCols() - 2
                        || row <= 1 || row >= map.getRows() - 2;
                if (rim && random.nextInt(100) < 70) {
                    map.set(seaHex(col, row));
                }
            }
        }
        for (int n = 0; n < 3; n++) {
            int col = 2 + random.nextInt(Math.max(1, map.getCols() - 4));
            int row = 2 + random.nextInt(Math.max(1, map.getRows() - 4));
            if (HexGeometry.distance(centreCol, centreRow, col, row) < 5) {
                continue;
            }
            Hex centre = map.get(col, row);
            if (centre == null) {
                continue;
            }
            map.set(seaHex(col, row));
            for (Hex neighbour : map.neighbours(centre)) {
                if (HexGeometry.distance(centreCol, centreRow,
                        neighbour.getCol(), neighbour.getRow()) > 4
                        && random.nextBoolean()) {
                    map.set(seaHex(neighbour.getCol(), neighbour.getRow()));
                }
            }
        }
    }

    private Hex seaHex(int col, int row) {
        boolean fish = random.nextInt(100) < 55;
        return new Hex(col, row, Terrain.SEA,
                fish ? ResourceType.FOOD : null,
                fish ? 40 + random.nextInt(40) : 0);
    }

    private void paintMountainRanges(GameMap map, int centreCol, int centreRow) {
        int placed = 0;
        for (int attempt = 0; attempt < 40 && placed < 2; attempt++) {
            int col = random.nextInt(map.getCols());
            int row = random.nextInt(map.getRows());
            if (HexGeometry.distance(centreCol, centreRow, col, row) < 6) {
                continue;
            }
            Hex start = map.get(col, row);
            if (start == null || start.getTerrain().isSea()) {
                continue;
            }
            int dir = random.nextInt(6);
            int steps = 0;
            for (int step = 0; step < 7; step++) {
                Hex hex = map.get(col, row);
                if (hex == null || hex.getTerrain().isSea()) {
                    break;
                }
                if (HexGeometry.distance(centreCol, centreRow, col, row) > 3) {
                    map.set(new Hex(col, row, Terrain.MOUNTAIN_RANGE, null, 0));
                    steps++;
                }
                int[] next = HexGeometry.neighbour(col, row, dir);
                col = next[0];
                row = next[1];
            }
            if (steps > 0) {
                placed++;
            }
        }
        if (placed == 0) {
            int col = Math.min(map.getCols() - 2, centreCol + 7);
            int row = centreRow;
            if (map.inside(col, row)
                    && HexGeometry.distance(centreCol, centreRow, col, row) > 3) {
                map.set(new Hex(col, row, Terrain.MOUNTAIN_RANGE, null, 0));
            }
        }
    }

    private void keepStartOpen(GameMap map, int centreCol, int centreRow) {
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.withinRange(centre, 2)) {
            if (hex.getTerrain().isSea() || hex.getTerrain().isMountainRange()) {
                map.set(new Hex(hex.getCol(), hex.getRow(), Terrain.PLAINS, null, 0));
            }
        }
    }

    /** A sea hex the explorer can reach, so Dock / Seafaring can be tested. */
    private void guaranteeNearbySea(GameMap map, int centreCol, int centreRow) {
        for (Hex hex : map.withinRange(map.get(centreCol, centreRow), 4)) {
            if (hex.getTerrain().isSea()) {
                return;
            }
        }
        int[] spot = HexGeometry.neighbour(centreCol, centreRow, 3);
        int[] outer = HexGeometry.neighbour(spot[0], spot[1], 3);
        if (map.inside(outer[0], outer[1])
                && HexGeometry.distance(centreCol, centreRow, outer[0], outer[1]) >= 2) {
            map.set(seaHex(outer[0], outer[1]));
        }
    }

    private void paintRivers(GameMap map, int centreCol, int centreRow) {
        List<Hex> seas = new ArrayList<>();
        List<Hex> hills = new ArrayList<>();
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Hex hex = map.get(col, row);
                if (hex.getTerrain().isSea()) {
                    seas.add(hex);
                } else if (hex.getTerrain() == Terrain.MOUNTAIN
                        || hex.getTerrain().isMountainRange()) {
                    hills.add(hex);
                }
            }
        }
        if (seas.isEmpty() || hills.isEmpty()) {
            return;
        }
        for (int n = 0; n < 2; n++) {
            Hex start = hills.get(random.nextInt(hills.size()));
            Hex goal = seas.get(random.nextInt(seas.size()));
            Hex current = start;
            for (int step = 0; step < 14 && current != goal; step++) {
                Hex next = stepToward(map, current, goal);
                if (next == null || next == current) {
                    break;
                }
                if (HexGeometry.distance(centreCol, centreRow, next.getCol(), next.getRow()) > 1) {
                    map.getEdges().getOrCreate(current, next).setRiver(true);
                }
                current = next;
            }
        }
    }

    private Hex stepToward(GameMap map, Hex from, Hex goal) {
        Hex best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Hex neighbour : map.neighbours(from)) {
            int dist = HexGeometry.distance(
                    neighbour.getCol(), neighbour.getRow(),
                    goal.getCol(), goal.getRow());
            if (dist < bestDist) {
                bestDist = dist;
                best = neighbour;
            }
        }
        return best;
    }

    private void ensureReachable(GameMap map, int centreCol, int centreRow) {
        int reachable = countReachableLand(map, centreCol, centreRow);
        if (reachable >= 40) {
            return;
        }
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.withinRange(centre, 5)) {
            if (hex.getTerrain().isMountainRange() || hex.getTerrain().isSea()) {
                map.set(new Hex(hex.getCol(), hex.getRow(), Terrain.PLAINS, null, 0));
            }
        }
    }

    private int countReachableLand(GameMap map, int centreCol, int centreRow) {
        boolean[][] seen = new boolean[map.getCols()][map.getRows()];
        Queue<Hex> queue = new ArrayDeque<>();
        Hex start = map.get(centreCol, centreRow);
        queue.add(start);
        seen[centreCol][centreRow] = true;
        int count = 0;
        while (!queue.isEmpty()) {
            Hex hex = queue.poll();
            if (!hex.getTerrain().isLand()) {
                continue;
            }
            count++;
            for (Hex neighbour : map.neighbours(hex)) {
                if (!seen[neighbour.getCol()][neighbour.getRow()]
                        && neighbour.getTerrain().isPassable()
                        && !neighbour.getTerrain().isSea()) {
                    seen[neighbour.getCol()][neighbour.getRow()] = true;
                    queue.add(neighbour);
                }
            }
        }
        return count;
    }

    private void guaranteeForestNear(GameMap map, int centreCol, int centreRow) {
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() == Terrain.FOREST) {
                return;
            }
        }
        int[] spot = HexGeometry.neighbour(centreCol, centreRow, 0);
        if (map.inside(spot[0], spot[1])) {
            map.set(new Hex(spot[0], spot[1], Terrain.FOREST, ResourceType.WOOD, 80));
        }
    }

    private void guaranteeFarmNear(GameMap map, int centreCol, int centreRow) {
        Hex centre = map.get(centreCol, centreRow);
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() == Terrain.GRASSLAND && hex.hasResource()
                    && hex.getDeposit() == ResourceType.FOOD) {
                return;
            }
        }
        for (Hex hex : map.neighbours(centre)) {
            if (hex.getTerrain() != Terrain.FOREST) {
                map.set(new Hex(hex.getCol(), hex.getRow(), Terrain.GRASSLAND, ResourceType.FOOD, 100));
                return;
            }
        }
    }
}
