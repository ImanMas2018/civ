package civ.model.world;

public class Happiness {

    private int value = 0;
    private int garrisonBonus = 0;
    private boolean militaryCapHit = false;

    public void add(int delta) {
        value += delta;
    }

    public int getValue() {
        return value + garrisonBonus;
    }

    public int getRawValue() {
        return value;
    }

    public void setRawValue(int value) {
        this.value = value;
    }

    public boolean hasMilitaryCapHit() {
        return militaryCapHit;
    }

    public void setMilitaryCapHit(boolean militaryCapHit) {
        this.militaryCapHit = militaryCapHit;
    }

    /** Sync +1 per military unit currently on the Town Hall hex. */
    public void setGarrisonBonus(int bonus) {
        this.garrisonBonus = Math.max(0, bonus);
    }

    public int getGarrisonBonus() {
        return garrisonBonus;
    }

    public String getLevelName() {
        int v = getValue();
        if (v >= 3) {
            return "Golden Age";
        }
        if (v >= -2) {
            return "Normal";
        }
        if (v >= -4) {
            return "Discontent";
        }
        return "Revolt";
    }

    public double productionMultiplier() {
        return getValue() >= 3 ? 1.10 : 1.0;
    }

    public int workerOutputPenalty() {
        return getValue() <= -3 ? 1 : 0;
    }

    public int apPenalty() {
        return getValue() <= -5 ? 1 : 0;
    }

    /** One-shot when the military unit cap is first reached. */
    public void noteMilitaryCapReached() {
        if (!militaryCapHit) {
            militaryCapHit = true;
            add(-1);
        }
    }
}
