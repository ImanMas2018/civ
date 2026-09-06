package civ.model.diplomacy;

/**
 * State pattern for player-to-player relations — same shape as Phase 2's
 * {@code RelationState}, but as an enum because the set is fixed and has no per-state data.
 */
public enum DiplomaticState {

    NEUTRAL("Neutral") {
        @Override
        public boolean allowsAttack() {
            return false;
        }

        @Override
        public boolean sharesVision() {
            return false;
        }

        @Override
        public boolean allowsTrade() {
            return true;
        }

        @Override
        public String lockReason() {
            return "You are not at war with this player.";
        }
    },
    ENEMY("Enemy") {
        @Override
        public boolean allowsAttack() {
            return true;
        }

        @Override
        public boolean sharesVision() {
            return false;
        }

        @Override
        public boolean allowsTrade() {
            return true;
        }

        @Override
        public String lockReason() {
            return "";
        }
    },
    ALLIED("Allied") {
        @Override
        public boolean allowsAttack() {
            return false;
        }

        @Override
        public boolean sharesVision() {
            return true;
        }

        @Override
        public boolean allowsTrade() {
            return true;
        }

        @Override
        public String lockReason() {
            return "You cannot attack an ally.";
        }
    };

    private final String label;

    DiplomaticState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public abstract boolean allowsAttack();

    public abstract boolean sharesVision();

    public abstract boolean allowsTrade();

    public abstract String lockReason();
}
