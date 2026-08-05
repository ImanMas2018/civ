package civ.util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Plays a looping background soundtrack and adjusts volume (0..100).
 * Missing or unreadable music files are ignored so the game still runs.
 */
public class MusicPlayer {

    private Clip clip;

    public void play(String resourcePath) {
        try {
            InputStream raw = getClass().getResourceAsStream(resourcePath);
            if (raw == null) {
                System.out.println("No music file at " + resourcePath + " — running silently.");
                return;
            }
            AudioInputStream stream =
                    AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            clip = AudioSystem.getClip();
            clip.open(stream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception e) {
            System.out.println("Could not play music: " + e.getMessage());
        }
    }

    /** @param volume volume from 0 (mute) to 100 (full) */
    public void setVolume(int volume) {
        if (clip == null) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (volume <= 0) {
            gain.setValue(gain.getMinimum());
            return;
        }
        float decibels = (float) (20.0 * Math.log10(volume / 100.0));
        float clamped = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels));
        gain.setValue(clamped);
    }
}
