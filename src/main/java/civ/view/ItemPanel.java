package civ.view;

import civ.controller.GameController;
import civ.model.Apothecary;
import civ.model.Building;
import civ.model.Game;
import civ.model.Player;
import civ.model.Unit;
import civ.model.item.Inventory;
import civ.model.item.ItemType;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;

/** Inventory counts plus craft / use actions for the Apothecary. */
public class ItemPanel extends JPanel {

    private final Game game;
    private final GameController controller;

    public ItemPanel(Game game, GameController controller) {
        this.game = game;
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        rebuild();
    }

    public void rebuild() {
        removeAll();
        Player me = game.getViewpointPlayer();
        Inventory inventory = me.getEmpire().getInventory();
        boolean myTurn = controller.isMyTurn();

        JLabel title = new JLabel("Inventory");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        Unit selected = game.getSelected();
        for (ItemType type : ItemType.values()) {
            int count = inventory.count(type);
            JLabel line = new JLabel(type.getLabel() + ": " + count);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(line);

            JButton use = new JButton("Use on selected unit");
            use.setAlignmentX(Component.LEFT_ALIGNMENT);
            boolean canTry = myTurn && count > 0 && selected != null
                    && game.owns(me, selected);
            use.setEnabled(canTry);
            if (!myTurn) {
                use.setToolTipText("It is not your turn.");
            } else if (count <= 0) {
                use.setToolTipText("You do not have that item.");
            } else if (selected == null) {
                use.setToolTipText("Select one of your units first.");
            } else if (!game.owns(me, selected)) {
                use.setToolTipText("You can only use items on your own units.");
            }
            ItemType chosen = type;
            use.addActionListener(e -> controller.useItem(chosen));
            add(use);
            add(Box.createVerticalStrut(6));
        }

        add(Box.createVerticalStrut(8));
        JLabel craftTitle = new JLabel("Apothecary");
        craftTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        craftTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(craftTitle);
        add(Box.createVerticalStrut(6));

        Apothecary shop = findOwnedApothecary(me);
        if (shop == null) {
            JLabel none = new JLabel("Build an Apothecary on plains (Town Hall level 2).");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(none);
        } else {
            JLabel queue = new JLabel(shop.describeQueue());
            queue.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(queue);
            add(Box.createVerticalStrut(4));
            for (ItemType type : ItemType.values()) {
                String reason = game.craftItemRejection(shop, type);
                boolean ok = reason == null && myTurn;
                JButton craft = new JButton("Craft " + type.getLabel()
                        + " (" + type.describeCost() + ")");
                craft.setAlignmentX(Component.LEFT_ALIGNMENT);
                craft.setEnabled(ok);
                craft.setToolTipText(!myTurn ? "It is not your turn." : reason);
                ItemType chosen = type;
                craft.addActionListener(e -> controller.craftItem(shop.getId(), chosen));
                add(craft);
                add(Box.createVerticalStrut(4));
            }
        }

        revalidate();
        repaint();
    }

    private Apothecary findOwnedApothecary(Player me) {
        for (Building building : me.getEmpire().getBuildings()) {
            if (building instanceof Apothecary) {
                return (Apothecary) building;
            }
        }
        return null;
    }

    public static void showDialog(Component parent, Game game, GameController controller) {
        ItemPanel panel = new ItemPanel(game, controller);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(panel);
        scroll.setPreferredSize(new java.awt.Dimension(340, 420));
        javax.swing.JOptionPane.showMessageDialog(
                parent, scroll, "Items", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }
}
