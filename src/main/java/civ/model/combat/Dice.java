package civ.model.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Dice {

    private final Random random;

    public Dice(Random random) {
        this.random = random;
    }

    public List<Integer> roll(int count, int bonus) {
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // No upper clamp: a combat-buffed die may show 7.
            int value = 1 + random.nextInt(6) + bonus;
            results.add(value);
        }
        results.sort(Collections.reverseOrder());
        return results;
    }
}
