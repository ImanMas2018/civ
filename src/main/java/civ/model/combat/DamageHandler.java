package civ.model.combat;

import civ.model.MilitaryUnit;
import java.util.List;

public abstract class DamageHandler {

    private DamageHandler next;

    /** Builds the chain and returns the new link, so calls read as one sentence. */
    public DamageHandler setNext(DamageHandler next) {
        this.next = next;
        return next;
    }

    /** Applies {@code hits} to the units, returns how many hits were not used. */
    public int handle(List<MilitaryUnit> units, int hits) {
        int remaining = hits;

        for (MilitaryUnit unit : units) {
            if (remaining <= 0) {
                break;
            }
            if (!accepts(unit)) {
                continue;
            }
            while (remaining > 0 && !unit.isDead()) {
                unit.takeCombatHit();
                remaining--;
            }
        }

        if (remaining > 0 && next != null) {
            return next.handle(units, remaining);
        }
        return remaining;
    }

    protected abstract boolean accepts(MilitaryUnit unit);
}
