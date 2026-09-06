package civ.model;

public class Explorer extends Unit {

    public Explorer(int col, int row) {
        super("Explorer", 6, 3, col, row);
    }

    public Explorer(long id, long createdAt, int col, int row) {
        super(id, createdAt, "Explorer", 6, 3, col, row);
    }
}
