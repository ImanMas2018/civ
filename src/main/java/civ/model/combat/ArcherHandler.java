package civ.model.combat;

import civ.model.Archer;
import civ.model.MilitaryUnit;

public class ArcherHandler extends DamageHandler {

    @Override
    protected boolean accepts(MilitaryUnit unit) {
        return unit instanceof Archer;
    }
}
