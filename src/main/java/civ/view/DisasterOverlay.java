package civ.view;

import civ.model.Hex;
import civ.model.world.DisasterEffect;
import civ.util.HexGeometry;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.swing.Timer;

/** Shake, cracks, rising water or bear flash for a few frames. */
public class DisasterOverlay {

    private DisasterEffect effect;
    private int frame;
    private int shakeX;
    private int shakeY;
    private Timer timer;
    private Runnable onFinished;
    private BiFunction<Integer, Integer, double[]> worldCenter;

    public void play(DisasterEffect effect, Runnable onFinished,
                     BiFunction<Integer, Integer, double[]> worldCenter) {
        stop();
        this.effect = effect;
        this.onFinished = onFinished;
        this.worldCenter = worldCenter;
        this.frame = 0;
        if (effect == null || !effect.isVisible()) {
            finish();
            return;
        }
        timer = new Timer(40, e -> {
            frame++;
            shakeX = (frame % 2 == 0) ? 4 : -4;
            shakeY = (frame % 3 == 0) ? 3 : -3;
            if (frame > 45) {
                finish();
            }
        });
        timer.start();
    }

    public boolean isPlaying() {
        return timer != null && timer.isRunning();
    }

    public int getShakeX() {
        return isPlaying() ? shakeX : 0;
    }

    public int getShakeY() {
        return isPlaying() ? shakeY : 0;
    }

    public void paint(Graphics2D g2, double hexSize, double cameraX, double cameraY, double zoom) {
        if (!isPlaying() || effect == null || worldCenter == null) {
            return;
        }
        String name = effect.getName();
        List<Hex> affected = effect.getAffected();
        for (Hex hex : affected) {
            double[] c = worldCenter.apply(hex.getCol(), hex.getRow());
            int sx = (int) ((c[0] - cameraX) * zoom) + shakeX;
            int sy = (int) ((c[1] - cameraY) * zoom) + shakeY;
            int r = (int) (hexSize * zoom * 0.55);
            if (name.contains("Flood")) {
                g2.setColor(new Color(40, 100, 200, Math.min(200, 40 + frame * 3)));
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
            } else if (name.contains("Earthquake")) {
                g2.setColor(new Color(80, 50, 30, 160));
                g2.drawLine(sx - r, sy, sx + r, sy + r / 2);
                g2.drawLine(sx - r / 2, sy + r, sx + r, sy - r / 3);
            } else {
                g2.setColor(new Color(180, 60, 40, 140));
                g2.fillOval(sx - r / 2, sy - r / 2, r, r);
            }
        }
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void finish() {
        stop();
        effect = null;
        shakeX = 0;
        shakeY = 0;
        Runnable done = onFinished;
        onFinished = null;
        if (done != null) {
            done.run();
        }
    }
}
