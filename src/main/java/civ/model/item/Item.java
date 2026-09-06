package civ.model.item;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;

public interface Item {

    ItemType getType();

    /** Null when the item may be used, otherwise the exact reason it may not. */
    String rejectionReason(Game game, Player player, Unit target, Hex destination);

    void apply(Game game, Player player, Unit target, Hex destination);
}
