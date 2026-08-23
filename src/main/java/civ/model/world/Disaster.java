package civ.model.world;

import civ.model.Game;

/** Strategy: each disaster knows its own preconditions and effects. */
public interface Disaster {

    String getName();

    boolean canHappen(Game game);

    void apply(Game game);
}
