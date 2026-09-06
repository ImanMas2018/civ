package civ.model.tribe;

import civ.model.Entity;
import civ.model.Hex;
import civ.model.ResourceType;
import civ.model.event.EventBus;
import civ.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;

public class Tribe extends Entity {

    private final String name;
    private final TribeType type;
    private final Hex campHex;
    private final List<TribeGuard> guards = new ArrayList<>();

    private int relation = 0;
    private RelationState state = new NeutralState();
    /** Tribe-specific discovery flag (independent of per-player Fog). */
    private boolean discovered = false;
    private boolean destroyed = false;
    private boolean allied = false;
    private int tradeBonusPercent = 0;
    private int questBlockTurns = 0;
    private int turnsSinceGuardSpawn = 0;
    private int turnsSinceQuestOffer = 0;
    private Quest quest;
    private TribeCamp camp;

    public Tribe(String name, TribeType type, Hex campHex) {
        this.name = name;
        this.type = type;
        this.campHex = campHex;
        this.quest = Quest.forType(type);
    }

    public Tribe(long id, long createdAt, String name, TribeType type, Hex campHex) {
        super(id, createdAt);
        this.name = name;
        this.type = type;
        this.campHex = campHex;
        this.quest = Quest.forType(type);
    }

    public String getName() {
        return name;
    }

    public TribeType getType() {
        return type;
    }

    public Hex getCampHex() {
        return campHex;
    }

    public TribeCamp getCamp() {
        return camp;
    }

    public void setCamp(TribeCamp camp) {
        this.camp = camp;
    }

    public List<TribeGuard> getGuards() {
        return guards;
    }

    public RelationState getState() {
        return state;
    }

    public int getRelation() {
        return relation;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void discover() {
        discovered = true;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public void setQuestBlockTurns(int questBlockTurns) {
        this.questBlockTurns = questBlockTurns;
    }

    public void setTradeBonusPercent(int tradeBonusPercent) {
        this.tradeBonusPercent = tradeBonusPercent;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void markDestroyed() {
        destroyed = true;
        allied = false;
        if (quest != null && (quest.getStatus() == QuestStatus.ACTIVE
                || quest.getStatus() == QuestStatus.READY
                || quest.getStatus() == QuestStatus.AVAILABLE)) {
            quest.cancel();
        }
    }

    public boolean isAllied() {
        return allied && relation >= 70;
    }

    public void setAllied(boolean allied) {
        this.allied = allied;
    }

    public int getTradeBonusPercent() {
        return tradeBonusPercent;
    }

    public void addTradeBonusPercent(int amount) {
        tradeBonusPercent += amount;
    }

    public boolean isQuestBlocked() {
        return questBlockTurns > 0;
    }

    public int getQuestBlockTurns() {
        return questBlockTurns;
    }

    public Quest getQuest() {
        return quest;
    }

    public void replaceQuest() {
        quest = Quest.forType(type);
    }

    public int getTurnsSinceGuardSpawn() {
        return turnsSinceGuardSpawn;
    }

    public void setTurnsSinceGuardSpawn(int turns) {
        turnsSinceGuardSpawn = turns;
    }

    public int getTurnsSinceQuestOffer() {
        return turnsSinceQuestOffer;
    }

    public void setTurnsSinceQuestOffer(int turns) {
        turnsSinceQuestOffer = turns;
    }

    public void changeRelation(int delta, EventBus bus) {
        int before = relation;
        relation = Math.max(-100, Math.min(100, relation + delta));
        RelationState newState = stateFor(relation);
        if (allied && relation < 70) {
            allied = false;
        }
        if (!newState.getName().equals(state.getName())) {
            state = newState;
            if (bus != null) {
                bus.publish(GameEvent.RELATION_CHANGED, this);
            }
        } else if (before != relation && bus != null) {
            bus.publish(GameEvent.RELATION_CHANGED, this);
        }
    }

    public void setRelationAbsolute(int value, EventBus bus) {
        changeRelation(value - relation, bus);
    }

    public void tickQuestBlock() {
        if (questBlockTurns > 0) {
            questBlockTurns--;
        }
    }

    public void blockQuests(int turns) {
        questBlockTurns = Math.max(questBlockTurns, turns);
    }

    public int giftRelationGain(ResourceType type, int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (type == ResourceType.FOOD || type == ResourceType.WOOD) {
            return (amount / 10) * 2;
        }
        if (type == ResourceType.STONE) {
            return (amount / 10) * 3;
        }
        if (type == ResourceType.IRON) {
            return (amount / 5) * 3;
        }
        return 0;
    }

    public String describeTradable() {
        switch (type) {
            case FARMER:
            case COASTAL:
                return "Gives Food";
            case MOUNTAIN:
                return "Gives Stone or Iron";
            case TRADER:
                return "Gives any resource (80%)";
            case WARRIOR:
                return "Does not trade resources";
            default:
                return "";
        }
    }

    private RelationState stateFor(int value) {
        if (value <= -50) {
            return new EnemyState();
        }
        if (value <= -20) {
            return new DispleasedState();
        }
        if (value <= 19) {
            return new NeutralState();
        }
        if (allied && value >= 70) {
            return new AlliedState();
        }
        return new FriendlyState();
    }
}
