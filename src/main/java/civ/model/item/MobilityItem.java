package civ.model.item;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;

public class MobilityItem implements Item {

    private static final int BONUS_AP = 2;

    @Override
    public ItemType getType() {
        return ItemType.MOBILITY;
    }

    @Override
    public String rejectionReason(Game game, Player player, Unit target, Hex destination) {
        return null;
    }

    @Override
    public void apply(Game game, Player player, Unit target, Hex destination) {
        // setAp deliberately allows going above maxAp — the spec asks for exactly that.
        target.setAp(target.getAp() + BONUS_AP);
        game.addLog(target.getTypeName() + " gained " + BONUS_AP + " action points.");
    }
}
