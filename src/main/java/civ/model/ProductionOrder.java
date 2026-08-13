package civ.model;

/**
 * One Town Hall job: train a unit or research a tech.
 * A baby Command: it stores the action and runs it when the countdown hits 0.
 */
public class ProductionOrder {

    private final String label;
    private final int totalTurns;
    private int turnsLeft;
    private final Runnable onFinished;

    public ProductionOrder(String label, int totalTurns, Runnable onFinished) {
        this.label = label;
        this.totalTurns = totalTurns;
        this.turnsLeft = totalTurns;
        this.onFinished = onFinished;
    }

    public String getLabel() {
        return label;
    }

    public int getTurnsLeft() {
        return turnsLeft;
    }

    public int getTotalTurns() {
        return totalTurns;
    }

    public boolean isDone() {
        return turnsLeft <= 0;
    }

    /** Called once per turn. Returns true on the turn it completes. */
    public boolean tick() {
        turnsLeft--;
        if (turnsLeft <= 0) {
            onFinished.run();
            return true;
        }
        return false;
    }
}
