package civ.model;

import civ.model.command.Command;

/**
 * Starting building. The +1 food / +1 wood safeguard is applied in
 * {@link Empire#netRatePerTurn()} and again in {@link TurnEngine}.
 * Only one production command at a time.
 */
public class TownHall extends Building {

    private static final int START_HP = 200;
    private static final int START_DEFENCE = 10;
    private static final int WALL_DEFENCE = 30;
    private static final int WALL_MAX_HP = 350;

    private Command activeCommand;
    private int turnsLeft;
    private int level = 1;
    private int defence = START_DEFENCE;
    private boolean defensiveWall = false;

    public TownHall(Hex hex) {
        super(BuildingType.TOWN_HALL, hex);
        setHealth(START_HP, START_HP);
    }

    public int getLevel() {
        return level;
    }

    public TownHallLevel getRank() {
        return TownHallLevel.of(level);
    }

    public int getDefence() {
        return defence;
    }

    public boolean hasDefensiveWall() {
        return defensiveWall;
    }

    public String describeLevel() {
        TownHallLevel rank = getRank();
        return "Level " + rank.getNumber() + " — " + rank.getLabel();
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

    /** Apply the instant effects of finishing the next rank. */
    public void promote(Game game) {
        TownHallLevel current = getRank();
        TownHallLevel next = current.next();
        if (next == null) {
            return;
        }
        int oldStorage = current.getStorage();
        level = next.getNumber();
        game.getEmpire().getStock().setCapacity(
                game.getEmpire().getStock().getCapacity() + (next.getStorage() - oldStorage));
        if (next.getHeal() > 0) {
            heal(next.getHeal());
        }
        game.addLog("Town Hall is now a " + next.getLabel() + ".");
    }

    public void applyDefensiveArchitecture() {
        defensiveWall = true;
        defence = WALL_DEFENCE;
        setMaxHp(WALL_MAX_HP);
    }

    public void restoreState(int level, int hp, int maxHp, int defence, boolean wall) {
        this.level = level;
        this.defence = defence;
        this.defensiveWall = wall;
        setHealth(hp, maxHp);
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
