package civ.model.tribe;

import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.util.HexGeometry;
import java.util.List;

public class TribeTurnBehaviour {

    public void act(Game game, Tribe tribe) {
        if (tribe.isDestroyed() || !tribe.isDiscovered()) {
            return;
        }
        tribe.tickQuestBlock();
        if (tribe.getQuest() != null) {
            tribe.getQuest().refreshReady(game, tribe);
            tribe.getQuest().tickDeadline();
            if (tribe.getQuest().getStatus() == QuestStatus.FAILED) {
                tribe.changeRelation(-10, game.getBus());
                tribe.blockQuests(5);
                game.addLog(tribe.getName() + " quest failed (−10 relation).");
                tribe.replaceQuest();
            }
        }

        for (TribeGuard guard : tribe.getGuards()) {
            guard.refresh(0);
        }

        if (tribe.getState().isHostile()) {
            if (tryDefend(game, tribe)) {
                return;
            }
            if (trySpawnGuard(game, tribe)) {
                return;
            }
            return;
        }

        if (tribe.getState().allowsQuest()) {
            tribe.setTurnsSinceQuestOffer(tribe.getTurnsSinceQuestOffer() + 1);
            if (tribe.getTurnsSinceQuestOffer() >= 5
                    && !tribe.isQuestBlocked()
                    && (tribe.getQuest() == null
                    || tribe.getQuest().getStatus() == QuestStatus.COMPLETED
                    || tribe.getQuest().getStatus() == QuestStatus.CANCELLED
                    || tribe.getQuest().getStatus() == QuestStatus.FAILED)) {
                tribe.replaceQuest();
                tribe.setTurnsSinceQuestOffer(0);
                game.addLog(tribe.getName() + " offers a new quest.");
                return;
            }
        }

        warnMilitaryTrespass(game, tribe);
    }

    private boolean tryDefend(Game game, Tribe tribe) {
        List<MilitaryUnit> threats = game.playerMilitaryNear(tribe.getCampHex(), 2);
        if (threats.isEmpty() || tribe.getGuards().isEmpty()) {
            return false;
        }
        MilitaryUnit target = threats.get(0);
        TribeGuard closest = tribe.getGuards().get(0);
        int best = Integer.MAX_VALUE;
        for (TribeGuard guard : tribe.getGuards()) {
            int d = HexGeometry.distance(
                    guard.getCol(), guard.getRow(), target.getCol(), target.getRow());
            if (d < best) {
                best = d;
                closest = guard;
            }
        }
        if (best <= 1 && closest.getAp() >= 1) {
            closest.spend(1);
            target.takeCombatHit();
            game.addLog(tribe.getName() + " guard struck your " + target.getTypeName() + ".");
            if (target.isDead()) {
                game.buryUnit(target);
            }
            return true;
        }
        if (best > 1 && closest.getAp() >= 1) {
            Hex step = stepToward(game, closest, target.getCol(), target.getRow());
            if (step != null) {
                closest.spend(1);
                closest.moveTo(step);
                return true;
            }
        }
        return false;
    }

    private boolean trySpawnGuard(Game game, Tribe tribe) {
        tribe.setTurnsSinceGuardSpawn(tribe.getTurnsSinceGuardSpawn() + 1);
        int cap = tribe.getType() == TribeType.WARRIOR ? 5 : 3;
        if (tribe.getGuards().size() >= cap) {
            return false;
        }
        if (tribe.getTurnsSinceGuardSpawn() < 3) {
            return false;
        }
        tribe.setTurnsSinceGuardSpawn(0);
        TribeGuard guard = new TribeGuard(
                tribe, tribe.getCampHex().getCol(), tribe.getCampHex().getRow());
        tribe.getGuards().add(guard);
        game.addLog(tribe.getName() + " raised a new guard.");
        return true;
    }

    private void warnMilitaryTrespass(Game game, Tribe tribe) {
        List<MilitaryUnit> inside = game.playerMilitaryNear(tribe.getCampHex(), 1);
        if (inside.isEmpty()) {
            return;
        }
        int drop = tribe.getState().getName().equals("Displeased") ? 4 : 2;
        tribe.changeRelation(-drop, game.getBus());
        game.addLog(tribe.getName() + " resents military presence near their camp ("
                + drop + " relation).");
    }

    private Hex stepToward(Game game, TribeGuard guard, int col, int row) {
        Hex best = null;
        int bestDist = HexGeometry.distance(guard.getCol(), guard.getRow(), col, row);
        for (Hex neighbour : game.getMap().neighbours(game.hexOf(guard))) {
            if (!neighbour.getTerrain().isLand()) {
                continue;
            }
            int d = HexGeometry.distance(neighbour.getCol(), neighbour.getRow(), col, row);
            if (d < bestDist) {
                bestDist = d;
                best = neighbour;
            }
        }
        return best;
    }
}
