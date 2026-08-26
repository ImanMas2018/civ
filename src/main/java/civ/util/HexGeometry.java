package civ.util;

import java.awt.geom.Path2D;

public final class HexGeometry {

    private HexGeometry() {
    }

    /** Horizontal distance between centres of adjacent columns (same row). */
    public static double horizSpacing(double size) {
        return Math.sqrt(3) * size;
    }

    /** Vertical distance between centres of adjacent rows. */
    public static double vertSpacing(double size) {
        return 1.5 * size;
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
        return size + horizSpacing(size) * (col + 0.5 * (row & 1));
    }

    public static double centerY(int col, int row, double size) {
        return size + vertSpacing(size) * row;
    }

    /**
     * A pointy-top regular hexagon as a Path2D (double precision — avoids
     * int-rounding that can collapse a hex into a triangle at small sizes).
     *
     * Corners are the classic 6 offsets, no trig loop at draw time:
     *   (0,-1), (±√3/2, -1/2), (±√3/2, +1/2), (0,+1)
     */
    public static Path2D.Double hexPath(double cx, double cy, double size) {
        Path2D.Double path = new Path2D.Double();
        writeHexPath(path, cx, cy, size);
        return path;
    }

    /**
     * Same hexagon, but written into a path you already own. The map redraws
     * hundreds of hexes per frame, so reusing one path avoids hundreds of
     * throw-away objects every time the camera moves.
     */
    public static void writeHexPath(Path2D.Double path, double cx, double cy, double size) {
        double w = Math.sqrt(3) / 2.0 * size; // half-width
        double h = size;                       // half-height (centre to tip)

        path.reset();
        path.moveTo(cx, cy - h);       // top
        path.lineTo(cx + w, cy - h / 2); // top-right
        path.lineTo(cx + w, cy + h / 2); // bottom-right
        path.lineTo(cx, cy + h);       // bottom
        path.lineTo(cx - w, cy + h / 2); // bottom-left
        path.lineTo(cx - w, cy - h / 2); // top-left
        path.closePath();
    }

    /** Nearest hex under a world-pixel point, or {-1, -1} if none. */
    public static int[] pixelToHex(double x, double y, double size, int cols, int rows) {
        int guessRow = (int) (y / vertSpacing(size));
        int guessCol = (int) (x / horizSpacing(size));

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
