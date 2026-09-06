package civ.model.command;

import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.item.ItemType;
import java.util.Map;

public class CraftItemCommand implements Command {

    private final ItemType type;
    private final Player owner;

    public CraftItemCommand(ItemType type, Player owner) {
        this.type = type;
        this.owner = owner;
    }

    @Override
    public String getLabel() {
        return "Crafting " + type.getLabel();
    }

    @Override
    public int getTurnsNeeded() {
        return 1;
    }

    @Override
    public void payCost(Game game) {
        for (Map.Entry<ResourceType, Integer> entry : type.getCost().entrySet()) {
            if (entry.getValue() > 0) {
                owner.getEmpire().getStock().add(entry.getKey(), -entry.getValue());
            }
        }
    }

    @Override
    public void execute(Game game) {
        owner.getEmpire().getInventory().add(type);
        game.addLog(type.getLabel() + " is ready.");
    }

    @Override
    public void cancel(Game game) {
        game.addLog(getLabel() + " cancelled. Resources are lost.");
    }

    public ItemType getItemType() {
        return type;
    }
}
