package civ.model.command;

import civ.model.Game;

public interface Command {

    String getLabel();

    int getTurnsNeeded();

    void payCost(Game game);

    void execute(Game game);

    /** Resources are not returned, by design. */
    void cancel(Game game);
}
