package civ.model.item;

import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.Player;
import civ.model.Unit;

public class CombatItem implements Item {

    @Override
    public ItemType getType() {
        return ItemType.COMBAT;
    }

    @Override
    public String rejectionReason(Game game, Player player, Unit target, Hex destination) {
        return target.isMilitary() ? null : "Only military units can use a combat item.";
    }

    @Override
    public void apply(Game game, Player player, Unit target, Hex destination) {
        ((MilitaryUnit) target).setCombatBuffed(true);
        game.addLog(target.getTypeName() + " is emboldened for this turn.");
    }
}
