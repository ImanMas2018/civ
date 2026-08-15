package civ.model;

import civ.model.command.Command;

/**
 * Starting building. The +1 food / +1 wood safeguard is applied in
 * {@link Empire#netRatePerTurn()} and again in {@link TurnEngine}.
 * Only one production command at a time.
 */
public class TownHall extends Building {

    private Command activeCommand;
    private int turnsLeft;
    private int level = 1;

    public TownHall(Hex hex) {
        super(BuildingType.TOWN_HALL, hex);
    }

    public int getLevel() {
        return level;
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
            return "Town Hall: idle";
        }
        return activeCommand.getLabel() + " — " + turnsLeft + " turns left";
    }

    @Override
    public int outputPerTurn(Empire empire) {
        return 0;
    }
}
