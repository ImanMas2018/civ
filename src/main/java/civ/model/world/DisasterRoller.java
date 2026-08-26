package civ.model.world;

import civ.model.Game;
import civ.model.event.GameEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DisasterRoller {

    private final List<Disaster> all;
    private final Random random;

    public DisasterRoller(Random random) {
        this.random = random;
        this.all = Arrays.asList(
                new Earthquake(),
                new Flood(),
                new BearAttack());
    }

    public DisasterRoller(List<Disaster> all, Random random) {
        this.all = all;
        this.random = random;
    }

    public void maybeStrike(Game game) {
        if (random.nextInt(100) >= 5) {
            return;
        }

        List<Disaster> possible = new ArrayList<>();
        for (Disaster disaster : all) {
            if (disaster.canHappen(game)) {
                possible.add(disaster);
            }
        }
        if (possible.isEmpty()) {
            return;
        }

        Disaster chosen = possible.get(random.nextInt(possible.size()));
        chosen.apply(game);
        game.getBus().publish(GameEvent.DISASTER_HAPPENED, game.getLastDisaster());
    }
}
