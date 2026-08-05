package civ;

import civ.view.MainWindow;
import javax.swing.SwingUtilities;

/**
 * Program entry point. Swing windows must be created on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
