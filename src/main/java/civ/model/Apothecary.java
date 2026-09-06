package civ.model;

import civ.model.command.Command;

/** Plains-only workshop with its own production queue, independent of the Town Hall. */
public class Apothecary extends Building {

    private Command activeCommand;
    private int turnsLeft;
    /** Client-only: queue text / busy flag from the last fog-filtered snapshot. */
    private String snapshotQueue;
    private boolean remoteBusy;

    public Apothecary(Hex hex) {
        super(BuildingType.APOTHECARY, hex);
    }

    public Apothecary(long id, long createdAt, Hex hex) {
        super(id, createdAt, BuildingType.APOTHECARY, hex);
    }

    public boolean isBusy() {
        return activeCommand != null || remoteBusy;
    }

    public Command getActiveCommand() {
        return activeCommand;
    }

    public int getTurnsLeft() {
        return turnsLeft;
    }

    public void start(Command command, Game game) {
        if (activeCommand != null) {
            return;
        }
        command.payCost(game);
        activeCommand = command;
        turnsLeft = command.getTurnsNeeded();
        remoteBusy = false;
        snapshotQueue = null;
    }

    public void cancelCommand(Game game) {
        if (activeCommand == null) {
            return;
        }
        activeCommand.cancel(game);
        activeCommand = null;
        turnsLeft = 0;
    }

    /** Called once per turn by TurnEngine. */
    public void tick(Game game) {
        if (activeCommand == null) {
            return;
        }
        turnsLeft--;
        if (turnsLeft <= 0) {
            activeCommand.execute(game);
            activeCommand = null;
        }
    }

    public String describeQueue() {
        if (snapshotQueue != null) {
            return snapshotQueue;
        }
        if (activeCommand == null) {
            return "Apothecary: idle";
        }
        return activeCommand.getLabel() + " — " + turnsLeft + " turns left";
    }

    /** Used by SnapshotApplier so the client can show busy state without a real Command. */
    public void applySnapshotQueue(String queue) {
        this.snapshotQueue = queue;
        this.remoteBusy = queue != null && !queue.contains("idle");
    }

    public void restoreQueue(Command command, int turnsLeft) {
        this.activeCommand = command;
        this.turnsLeft = turnsLeft;
    }

    @Override
    public int outputPerTurn(Empire empire, GameMap map) {
        return 0;
    }
}
