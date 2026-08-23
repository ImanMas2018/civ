package civ.view;

import civ.model.world.Season;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Falling snow in winter, rain/wind streaks in autumn. */
public class SeasonOverlay {

    private static final class Particle {
        double x;
        double y;
        double speed;
        double drift;
        int size;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private Season season = Season.SPRING;
    private int width;
    private int height;

    public void setSeason(Season season) {
        if (this.season != season) {
            this.season = season;
            particles.clear();
            if (season.hasSnow() || season.hasRain()) {
                ensureParticles();
            }
        }
    }

    public void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        if (season.hasSnow() || season.hasRain()) {
            ensureParticles();
        }
    }

    public boolean isActive() {
        return season.hasSnow() || season.hasRain();
    }

    public void tick() {
        if (!isActive() || width <= 0) {
            return;
        }
        ensureParticles();
        for (Particle p : particles) {
            p.y += p.speed;
            p.x += p.drift;
            if (p.y > height) {
                p.y = -4;
                p.x = random.nextInt(width);
            }
            if (p.x < -10) {
                p.x = width + 4;
            }
            if (p.x > width + 10) {
                p.x = -4;
            }
        }
    }

    public void paint(Graphics2D g2) {
        if (!isActive()) {
            return;
        }
        if (season.hasSnow()) {
            g2.setColor(new Color(255, 255, 255, 180));
            for (Particle p : particles) {
                g2.fillOval((int) p.x, (int) p.y, p.size, p.size);
            }
        } else if (season.hasRain()) {
            g2.setColor(new Color(160, 190, 220, 140));
            for (Particle p : particles) {
                g2.drawLine((int) p.x, (int) p.y, (int) (p.x + p.drift * 3), (int) (p.y + 8));
            }
        }
    }

    private void ensureParticles() {
        int target = season.hasSnow() ? 120 : 90;
        while (particles.size() < target) {
            Particle p = new Particle();
            p.x = random.nextInt(Math.max(1, width));
            p.y = random.nextInt(Math.max(1, height));
            if (season.hasSnow()) {
                p.speed = 0.8 + random.nextDouble() * 1.4;
                p.drift = -0.4 + random.nextDouble() * 0.8;
                p.size = 2 + random.nextInt(3);
            } else {
                p.speed = 4.0 + random.nextDouble() * 5.0;
                p.drift = -1.5 - random.nextDouble();
                p.size = 1;
            }
            particles.add(p);
        }
        while (particles.size() > target) {
            particles.remove(particles.size() - 1);
        }
    }
}
