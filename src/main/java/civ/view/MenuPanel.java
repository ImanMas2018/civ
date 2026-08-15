package civ.view;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

/**
 * Main menu: Start, Settings (music volume), Exit (with confirmation).
 */
public class MenuPanel extends JPanel {

    public MenuPanel(MainWindow window) {
        setBackground(new Color(30, 40, 55));
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("CIV", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 64));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton start = makeButton("Start");
        JButton settings = makeButton("Settings");
        JButton exit = makeButton("Exit");

        start.addActionListener(e -> window.startNewGame());
        settings.addActionListener(e -> showSettings(window));
        exit.addActionListener(e -> confirmExit());

        column.add(title);
        column.add(Box.createVerticalStrut(40));
        column.add(start);
        column.add(Box.createVerticalStrut(12));
        column.add(settings);
        column.add(Box.createVerticalStrut(12));
        column.add(exit);

        add(column);
    }

    private JButton makeButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(240, 44));
        button.setPreferredSize(new Dimension(240, 44));
        button.setFocusPainted(false);
        return button;
    }

    private void showSettings(MainWindow window) {
        JSlider slider = new JSlider(0, 100, 60);
        slider.setMajorTickSpacing(25);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(e -> window.getMusic().setVolume(slider.getValue()));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new JLabel("Music volume"));
        content.add(slider);

        JDialog dialog = new JDialog(window, "Settings", true);
        dialog.setContentPane(content);
        dialog.setSize(360, 160);
        dialog.setLocationRelativeTo(window);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }

    private void confirmExit() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Do you really want to quit the game?",
                "Exit",
                JOptionPane.YES_NO_OPTION);

        if (answer == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
