package civ.model.combat;

import civ.model.MilitaryUnit;
import civ.model.Swordsman;

public class SwordsmanHandler extends DamageHandler {

    @Override
    protected boolean accepts(MilitaryUnit unit) {
        return unit instanceof Swordsman;
    }
}
