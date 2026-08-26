package civ.view;

import civ.controller.SaveController;
import civ.model.Game;
import civ.model.save.SaveSlotInfo;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

public class SaveLoadPanel extends JPanel {

    private static final Color BG = new Color(28, 32, 42);
    private static final Color TEXT = Color.WHITE;

    private final SaveController saveController;
    private final Game game;
    private final boolean allowSave;
    private final Runnable onClose;
    private final Runnable onMainMenu;
    private final JPanel list = new JPanel();

    public SaveLoadPanel(SaveController saveController, Game game,
                         boolean allowSave, Runnable onClose) {
        this(saveController, game, allowSave, onClose, null);
    }

    public SaveLoadPanel(SaveController saveController, Game game,
                         boolean allowSave, Runnable onClose, Runnable onMainMenu) {
        this.saveController = saveController;
        this.game = game;
        this.allowSave = allowSave;
        this.onClose = onClose;
        this.onMainMenu = onMainMenu;

        setLayout(new BorderLayout(8, 8));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel(allowSave ? "Pause — Save / Load" : "Load Game");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        JButton close = new JButton(allowSave ? "Resume" : "Back");
        close.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        south.add(close);
        if (onMainMenu != null) {
            JButton menu = new JButton("Main Menu");
            menu.addActionListener(e -> {
                int answer = JOptionPane.showConfirmDialog(this,
                        "Return to the main menu?",
                        "Main Menu",
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    if (onClose != null) {
                        onClose.run();
                    }
                    onMainMenu.run();
                }
            });
            south.add(menu);
        }
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        list.removeAll();
        List<SaveSlotInfo> slots = saveController.listSlots();
        for (SaveSlotInfo slot : slots) {
            list.add(rowFor(slot));
            list.add(Box.createVerticalStrut(8));
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel rowFor(SaveSlotInfo slot) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel info = new JLabel(slot.describe());
        info.setForeground(TEXT);
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));
        row.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        if (allowSave && slot.getKind() == SaveSlotInfo.Kind.MANUAL && game != null) {
            JButton save = new JButton("Save");
            String lock = game.isSaveLocked() ? game.saveLockReason() : null;
            save.setEnabled(lock == null);
            save.setToolTipText(lock);
            save.addActionListener(e -> doSave(slot));
            buttons.add(save);
        }

        JButton load = new JButton("Load");
        load.setEnabled(slot.getStatus() == SaveSlotInfo.Status.OK);
        load.addActionListener(e -> doLoad(slot));
        buttons.add(load);

        if (slot.getStatus() == SaveSlotInfo.Status.DAMAGED
                || slot.getStatus() == SaveSlotInfo.Status.OK) {
            JButton delete = new JButton("Delete");
            delete.addActionListener(e -> doDelete(slot));
            buttons.add(delete);
        }

        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void doSave(SaveSlotInfo slot) {
        if (game == null) {
            return;
        }
        if (game.isSaveLocked()) {
            JOptionPane.showMessageDialog(this, game.saveLockReason());
            return;
        }
        if (slot.getStatus() == SaveSlotInfo.Status.OK) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Overwrite " + slot.getSlotName() + "?",
                    "Overwrite save",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        String name = JOptionPane.showInputDialog(this, "Save name:",
                slot.getDisplayName() == null ? "Save" : slot.getDisplayName());
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        try {
            saveController.save(game, slot.getSlotName(), name.trim());
            refresh();
            JOptionPane.showMessageDialog(this, "Saved.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void doLoad(SaveSlotInfo slot) {
        if (game != null) {
            Object[] options = {"Save first", "Load without saving", "Cancel"};
            int answer = JOptionPane.showOptionDialog(this,
                    "Loading will replace the current game. Unsaved progress will be lost.",
                    "Load game",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (answer == 2 || answer == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (answer == 0) {
                if (game.isSaveLocked()) {
                    JOptionPane.showMessageDialog(this, game.saveLockReason());
                    return;
                }
                String name = JOptionPane.showInputDialog(this, "Save name before load:", "Quick save");
                if (name == null || name.trim().isEmpty()) {
                    return;
                }
                try {
                    saveController.save(game, "slot1", name.trim());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
                    return;
                }
            }
        }
        try {
            Game loaded = saveController.load(slot.getSlotName());
            if (onClose != null) {
                onClose.run();
            }
            saveController.openLoadedGame(loaded);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load this save. It may be damaged.\n" + ex.getMessage());
            refresh();
        }
    }

    private void doDelete(SaveSlotInfo slot) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + slot.getSlotName() + "?",
                "Delete save",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            saveController.deleteSlot(slot.getSlotName());
            refresh();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }
}
