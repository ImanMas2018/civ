package civ.model;

import civ.model.command.Command;

/** Plains-only workshop with its own production queue, independent of the Town Hall. */
public class Apothecary extends Building {

    private Command activeCommand;
    private int turnsLeft;

    public Apothecary(Hex hex) {
        super(BuildingType.APOTHECARY, hex);
    }

    public Apothecary(long id, long createdAt, Hex hex) {
        super(id, createdAt, BuildingType.APOTHECARY, hex);
    }

    public boolean isBusy() {
        return activeCommand != null;
    }

    public Command getActiveCommand() {
        return activeCommand;
    }

    public int getTurnsLeft() {
        return turnsLeft;
    }

    public void start(Command command, Game game) {
        if (isBusy()) {
            return;
        }
        command.payCost(game);
        activeCommand = command;
        turnsLeft = command.getTurnsNeeded();
    }

    public void cancelCommand(Game game) {
        if (!isBusy()) {
            return;
        }
        activeCommand.cancel(game);
        activeCommand = null;
        turnsLeft = 0;
    }

    /** Called once per turn by TurnEngine. */
    public void tick(Game game) {
        if (!isBusy()) {
            return;
        }
        turnsLeft--;
        if (turnsLeft <= 0) {
            activeCommand.execute(game);
            activeCommand = null;
        }
    }

    public String describeQueue() {
        if (!isBusy()) {
            return "Apothecary: idle";
        }
        return activeCommand.getLabel() + " — " + turnsLeft + " turns left";
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
