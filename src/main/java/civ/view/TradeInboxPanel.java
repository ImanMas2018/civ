package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.trade.TradeOffer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TradeInboxPanel extends JPanel {

    private final Game game;
    private final GameController controller;

    public TradeInboxPanel(Game game, GameController controller) {
        this.game = game;
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        rebuild();
    }

    public void rebuild() {
        removeAll();
        Player me = game.getViewpointPlayer();

        JLabel inboxTitle = new JLabel("Incoming offers");
        inboxTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        inboxTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(inboxTitle);
        add(Box.createVerticalStrut(6));

        List<TradeOffer> inbox = game.getTradeOffers().pendingFor(me);
        if (inbox.isEmpty()) {
            add(body("No pending offers."));
        } else {
            for (TradeOffer offer : inbox) {
                add(offerRow(offer, true));
            }
        }

        add(Box.createVerticalStrut(12));
        JLabel outTitle = new JLabel("Your outgoing offers");
        outTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        outTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(outTitle);
        add(Box.createVerticalStrut(6));

        List<TradeOffer> outgoing = game.getTradeOffers().pendingFrom(me);
        if (outgoing.isEmpty()) {
            add(body("None."));
        } else {
            for (TradeOffer offer : outgoing) {
                add(offerRow(offer, false));
            }
        }

        revalidate();
        repaint();
    }

    private JPanel offerRow(TradeOffer offer, boolean incoming) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Player from = game.getPlayer(offer.getFromPlayerId());
        Player to = game.getPlayer(offer.getToPlayerId());
        String who = incoming
                ? "From " + (from == null ? "?" : from.getName())
                : "To " + (to == null ? "?" : to.getName());
        row.add(body(who + " — offer " + describe(offer.getOffered())
                + " for " + describe(offer.getRequested())));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (incoming) {
            JButton accept = new JButton("Accept");
            accept.setEnabled(controller.isMyTurn());
            accept.setToolTipText(controller.isMyTurn() ? null : "It is not your turn.");
            accept.addActionListener(e -> controller.replyTrade(offer.getId(), true));
            JButton reject = new JButton("Reject");
            reject.setEnabled(controller.isMyTurn());
            reject.setToolTipText(controller.isMyTurn() ? null : "It is not your turn.");
            reject.addActionListener(e -> controller.replyTrade(offer.getId(), false));
            buttons.add(accept);
            buttons.add(Box.createHorizontalStrut(6));
            buttons.add(reject);
        } else {
            JButton cancel = new JButton("Cancel");
            cancel.setEnabled(controller.isMyTurn());
            cancel.setToolTipText(controller.isMyTurn() ? null : "It is not your turn.");
            cancel.addActionListener(e -> controller.cancelTrade(offer.getId()));
            buttons.add(cancel);
        }
        row.add(buttons);
        row.add(Box.createVerticalStrut(8));
        return row;
    }

    private static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String describe(Map<ResourceType, Integer> amounts) {
        List<String> parts = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            int amount = amounts.getOrDefault(type, 0);
            if (amount > 0) {
                parts.add(amount + " " + type.getLabel());
            }
        }
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    public static void showInbox(Component parent, Game game, GameController controller) {
        TradeInboxPanel panel = new TradeInboxPanel(game, controller);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(460, 320));
        JOptionPane.showMessageDialog(parent, scroll, "Trade inbox", JOptionPane.PLAIN_MESSAGE);
    }

    public static void showNewOffer(Component parent, Game game, GameController controller) {
        Player me = game.getViewpointPlayer();
        List<Player> others = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            if (player.getId() != me.getId() && player.isAlive()) {
                others.add(player);
            }
        }
        if (others.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No other players to trade with.");
            return;
        }

        JComboBox<String> targets = new JComboBox<>();
        for (Player player : others) {
            targets.addItem(player.getName());
        }

        Map<ResourceType, JSpinner> offerSpinners = new EnumMap<>(ResourceType.class);
        Map<ResourceType, JSpinner> askSpinners = new EnumMap<>(ResourceType.class);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Trade with:"));
        form.add(targets);
        form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("You offer:"));
        for (ResourceType type : ResourceType.values()) {
            int max = me.getEmpire().getStock().available(type);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Math.max(0, max), 1));
            offerSpinners.put(type, spinner);
            JPanel line = new JPanel();
            line.add(new JLabel(type.getLabel() + " (avail " + max + ")"));
            line.add(spinner);
            form.add(line);
        }
        form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("You ask for:"));
        for (ResourceType type : ResourceType.values()) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
            askSpinners.put(type, spinner);
            JPanel line = new JPanel();
            line.add(new JLabel(type.getLabel()));
            line.add(spinner);
            form.add(line);
        }

        int answer = JOptionPane.showConfirmDialog(parent, form, "New trade offer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Player target = others.get(targets.getSelectedIndex());
        controller.sendTradeOffer(
                target.getId(),
                (Integer) offerSpinners.get(ResourceType.FOOD).getValue(),
                (Integer) offerSpinners.get(ResourceType.WOOD).getValue(),
                (Integer) offerSpinners.get(ResourceType.STONE).getValue(),
                (Integer) offerSpinners.get(ResourceType.IRON).getValue(),
                (Integer) askSpinners.get(ResourceType.FOOD).getValue(),
                (Integer) askSpinners.get(ResourceType.WOOD).getValue(),
                (Integer) askSpinners.get(ResourceType.STONE).getValue(),
                (Integer) askSpinners.get(ResourceType.IRON).getValue());
    }
}
