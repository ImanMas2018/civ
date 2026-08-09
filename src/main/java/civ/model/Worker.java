package civ.model;

/**
 * Gathers from buildings once stationed inside one.
 * Stationing itself arrives in Step 4; for now a Worker is just a slow unit.
 */
public class Worker extends Unit {

    public Worker(int col, int row) {
        super("Worker", 3, 1, col, row);
    }

    @Override
    public String describe() {
        return super.describe() + "  idle";
    }
}
