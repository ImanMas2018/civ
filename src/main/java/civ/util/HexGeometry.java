package civ.util;

import java.awt.Polygon;

/**
 * All hex-grid maths in one place (odd-r offset coordinates, pointy-top hexes).
 * Static toolbox — do not create instances.
 */
public final class HexGeometry {

    private HexGeometry() {
    }

    /** Neighbour offsets: [even/odd row][direction 0..5][col/row delta]. */
    private static final int[][][] DIRECTIONS = {
            {{+1, 0}, {0, -1}, {-1, -1}, {-1, 0}, {-1, +1}, {0, +1}},
            {{+1, 0}, {+1, -1}, {0, -1}, {-1, 0}, {0, +1}, {+1, +1}}
    };

    public static int[] neighbour(int col, int row, int direction) {
        int[] delta = DIRECTIONS[row & 1][direction];
        return new int[] {col + delta[0], row + delta[1]};
    }

    /** Hex steps between two tiles (via cube coordinates). */
    public static int distance(int col1, int row1, int col2, int row2) {
        int x1 = col1 - (row1 - (row1 & 1)) / 2;
        int z1 = row1;
        int y1 = -x1 - z1;

        int x2 = col2 - (row2 - (row2 & 1)) / 2;
        int z2 = row2;
        int y2 = -x2 - z2;

        return (Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2)) / 2;
    }

    public static double centerX(int col, int row, double size) {
        return size * Math.sqrt(3) * (col + 0.5 * (row & 1)) + size;
    }

    public static double centerY(int col, int row, double size) {
        return size * 1.5 * row + size;
    }

    public static Polygon polygon(double cx, double cy, double size) {
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 90);
            int x = (int) Math.round(cx + size * Math.cos(angle));
            int y = (int) Math.round(cy + size * Math.sin(angle));
            p.addPoint(x, y);
        }
        return p;
    }

    /** Nearest hex under a world-pixel point, or {-1, -1} if none. */
    public static int[] pixelToHex(double x, double y, double size, int cols, int rows) {
        int guessRow = (int) (y / (size * 1.5));
        int guessCol = (int) (x / (size * Math.sqrt(3)));

        int bestCol = -1;
        int bestRow = -1;
        double bestDistance = Double.MAX_VALUE;

        for (int row = guessRow - 1; row <= guessRow + 1; row++) {
            for (int col = guessCol - 1; col <= guessCol + 1; col++) {
                if (col < 0 || row < 0 || col >= cols || row >= rows) {
                    continue;
                }
                double dx = x - centerX(col, row, size);
                double dy = y - centerY(col, row, size);
                double d = dx * dx + dy * dy;
                if (d < bestDistance) {
                    bestDistance = d;
                    bestCol = col;
                    bestRow = row;
                }
            }
        }
        return new int[] {bestCol, bestRow};
    }
}
