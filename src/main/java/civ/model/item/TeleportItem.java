package civ.model.item;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;

public class TeleportItem implements Item {

    @Override
    public ItemType getType() {
        return ItemType.TELEPORT;
    }

    @Override
    public String rejectionReason(Game game, Player player, Unit target, Hex destination) {
        if (destination == null) {
            return "Choose a destination hex.";
        }
        if (!player.getFog().isDiscovered(destination)) {
            return "You cannot teleport into unexplored territory.";
        }
        if (game.anyUnitAt(destination)) {
            return "That hex already contains a unit.";
        }
        if (!game.isPassableFor(target, destination)) {
            return "That unit cannot stand on " + destination.getTerrain().getLabel() + ".";
        }
        return null;
    }

    @Override
    public void apply(Game game, Player player, Unit target, Hex destination) {
        target.moveTo(destination);
        game.revealAround(player, target);
        game.addLog(target.getTypeName() + " teleported.");
    }
}
