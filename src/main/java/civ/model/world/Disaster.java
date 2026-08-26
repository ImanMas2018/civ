package civ.model.world;

import civ.model.Game;

public interface Disaster {

    String getName();

    boolean canHappen(Game game);

    void apply(Game game);
}
