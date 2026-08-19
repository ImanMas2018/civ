package civ.model.combat;

import civ.model.Cavalry;
import civ.model.MilitaryUnit;

public class CavalryHandler extends DamageHandler {

    @Override
    protected boolean accepts(MilitaryUnit unit) {
        return unit instanceof Cavalry;
    }
}
