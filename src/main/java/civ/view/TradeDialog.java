package civ.view;

import civ.model.Game;
import civ.model.ResourceType;
import civ.model.trade.BazaarTrade;
import civ.model.trade.TradeRate;
import civ.model.trade.TradingPostTrade;
import civ.model.trade.TribeTrade;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeType;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/** Lets the player pick sell/receive resources and confirm a trade. */
public final class TradeDialog {

    private TradeDialog() {
    }

    public static void showBazaar(Component parent, Game game, Runnable onDone) {
        List<TradeRate> rates = new ArrayList<>();
        rates.add(new BazaarTrade(1));
        rates.add(new BazaarTrade(2));
        rates.add(new BazaarTrade(3));
        show(parent, game, "Bazaar", rates, null, onDone);
    }

    public static void showTradingPost(Component parent, Game game, Runnable onDone) {
        List<TradeRate> rates = new ArrayList<>();
        rates.add(new TradingPostTrade());
        show(parent, game, "Trading Post", rates, null, onDone);
    }

    public static void showTribe(Component parent, Game game, Tribe tribe, Runnable onDone) {
        if (tribe.getType() == TribeType.WARRIOR) {
            return;
        }
        List<TradeRate> rates = new ArrayList<>();
        rates.add(new TribeTrade(tribe));
        show(parent, game, "Trade with " + tribe.getName(), rates, tribe, onDone);
    }

    private static void show(Component parent, Game game, String title,
                             List<TradeRate> rates, Tribe tribe, Runnable onDone) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        root.setBackground(new Color(32, 36, 46));

        JComboBox<TradeRate> rateBox = new JComboBox<>(rates.toArray(new TradeRate[0]));
        JComboBox<ResourceType> sellBox = new JComboBox<>(ResourceType.values());
        JComboBox<ResourceType> receiveBox = new JComboBox<>(receiveOptions(tribe));
        JSpinner amount = new JSpinner(new SpinnerNumberModel(10, 1, 9999, 1));
        JLabel preview = label("—");

        style(rateBox);
        style(sellBox);
        style(receiveBox);
        amount.setMaximumSize(new Dimension(120, 28));
        amount.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable refresh = () -> {
            TradeRate rate = (TradeRate) rateBox.getSelectedItem();
            if (rate == null) {
                return;
            }
            if (rate.getFixedAmount() > 0) {
                amount.setValue(rate.getFixedAmount());
                amount.setEnabled(false);
            } else {
                amount.setEnabled(true);
            }
            int sold = (Integer) amount.getValue();
            ResourceType sell = (ResourceType) sellBox.getSelectedItem();
            ResourceType receive = (ResourceType) receiveBox.getSelectedItem();
            int got = rate.convert(sold);
            preview.setText("Sell " + sold + " " + sell.getLabel()
                    + " → get " + got + " " + receive.getLabel()
                    + " (" + rate.getPercent() + "%)");
        };
        rateBox.addActionListener(e -> refresh.run());
        sellBox.addActionListener(e -> refresh.run());
        receiveBox.addActionListener(e -> refresh.run());
        amount.addChangeListener(e -> refresh.run());
        refresh.run();

        root.add(label("Rate"));
        root.add(rateBox);
        root.add(Box.createVerticalStrut(8));
        root.add(label("Sell"));
        root.add(sellBox);
        root.add(Box.createVerticalStrut(8));
        root.add(label("Receive"));
        root.add(receiveBox);
        root.add(Box.createVerticalStrut(8));
        root.add(label("Amount"));
        root.add(amount);
        root.add(Box.createVerticalStrut(8));
        root.add(preview);
        root.add(Box.createVerticalStrut(12));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton confirm = new JButton("Confirm");
        JButton cancel = new JButton("Cancel");
        confirm.addActionListener(e -> {
            TradeRate rate = (TradeRate) rateBox.getSelectedItem();
            ResourceType sell = (ResourceType) sellBox.getSelectedItem();
            ResourceType receive = (ResourceType) receiveBox.getSelectedItem();
            int sold = (Integer) amount.getValue();
            if (sell == receive) {
                preview.setText("Pick two different resources.");
                return;
            }
            if (!game.getTradeService().canTrade(game, rate, sell, sold)) {
                preview.setText("Cannot trade (cap, stock, or already traded this turn).");
                return;
            }
            game.getTradeService().trade(game, rate, sell, receive, sold);
            dialog.dispose();
            if (onDone != null) {
                onDone.run();
            }
        });
        cancel.addActionListener(e -> dialog.dispose());
        buttons.add(confirm);
        buttons.add(cancel);
        root.add(buttons);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static ResourceType[] receiveOptions(Tribe tribe) {
        if (tribe == null || tribe.getType() == TribeType.TRADER) {
            return ResourceType.values();
        }
        if (tribe.getType() == TribeType.MOUNTAIN) {
            return new ResourceType[] {ResourceType.STONE, ResourceType.IRON};
        }
        if (tribe.getType() == TribeType.FARMER || tribe.getType() == TribeType.COASTAL) {
            return new ResourceType[] {ResourceType.FOOD};
        }
        return ResourceType.values();
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(220, 225, 235));
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void style(JComboBox<?> box) {
        box.setMaximumSize(new Dimension(260, 28));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}
