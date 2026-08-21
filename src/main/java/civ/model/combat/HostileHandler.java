package civ.model.combat;

import civ.model.Barbarian;
import civ.model.MilitaryUnit;
import civ.model.WildAnimal;
import civ.model.tribe.TribeGuard;

/** Hostiles and tribe guards take hits before the player's sword → archer → cavalry order. */
public class HostileHandler extends DamageHandler {

    @Override
    protected boolean accepts(MilitaryUnit unit) {
        return unit.isHostile()
                || unit instanceof TribeGuard
                || unit instanceof Barbarian
                || unit instanceof WildAnimal;
    }
}
