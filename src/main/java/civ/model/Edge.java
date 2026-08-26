package civ.model;

import java.util.Objects;

public class Edge {

    private final int col1;
    private final int row1;
    private final int col2;
    private final int row2;

    private boolean river;
    private Wall wall;

    public Edge(Hex a, Hex b) {
        boolean aFirst = a.getRow() < b.getRow()
                || (a.getRow() == b.getRow() && a.getCol() <= b.getCol());
        Hex first = aFirst ? a : b;
        Hex second = aFirst ? b : a;

        this.col1 = first.getCol();
        this.row1 = first.getRow();
        this.col2 = second.getCol();
        this.row2 = second.getRow();
    }

    public int getCol1() {
        return col1;
    }

    public int getRow1() {
        return row1;
    }

    public int getCol2() {
        return col2;
    }

    public int getRow2() {
        return row2;
    }

    public boolean hasRiver() {
        return river;
    }

    public void setRiver(boolean river) {
        this.river = river;
    }

    public Wall getWall() {
        return wall;
    }

    public void setWall(Wall wall) {
        this.wall = wall;
    }

    public boolean hasWall() {
        return wall != null && !wall.isDestroyed();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Edge)) {
            return false;
        }
        Edge that = (Edge) other;
        return col1 == that.col1 && row1 == that.row1
                && col2 == that.col2 && row2 == that.row2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(col1, row1, col2, row2);
    }
}
