package civ.model.command;

import civ.model.Game;

/**
 * One Town Hall job. {@link civ.model.TownHall} only counts down and calls these
 * three methods — it never knows about units, techs or upgrades.
 */
public interface Command {

    String getLabel();

    int getTurnsNeeded();

    void payCost(Game game);

    void execute(Game game);

    /** Resources are not returned, by design. */
    void cancel(Game game);
}
