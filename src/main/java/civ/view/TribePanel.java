package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.ResourceType;
import civ.model.tribe.AllianceRules;
import civ.model.tribe.Quest;
import civ.model.tribe.QuestStatus;
import civ.model.tribe.Tribe;
import civ.model.tribe.TribeType;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/** Interaction panel for one discovered tribe camp. */
public class TribePanel extends JPanel {

    private static final Color BG = new Color(32, 36, 46);
    private static final Color TITLE = new Color(230, 235, 245);
    private static final Color BODY = new Color(180, 190, 210);

    private final Game game;
    private final Tribe tribe;
    private final GameController controller;

    public TribePanel(Game game, Tribe tribe, GameController controller) {
        this.game = game;
        this.tribe = tribe;
        this.controller = controller;
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        rebuild();
    }

    public void rebuild() {
        removeAll();
        addTitle(tribe.getName());
        addBody("Type: " + tribe.getType().getLabel());
        addBody("Relation: " + tribe.getState().getName()
                + " (" + tribe.getRelation() + ")");
        addBody("Camp: (" + tribe.getCampHex().getCol()
                + ", " + tribe.getCampHex().getRow() + ")"
                + "  HP " + tribe.getCamp().getHp() + "/" + tribe.getCamp().getMaxHp());
        addBody("Tradable: " + tribe.describeTradable());
        addBody("Guards: " + tribe.getGuards().size());
        addBody("Rewards: " + tribe.getType().getRewardBlurb());

        Quest quest = tribe.getQuest();
        if (quest != null) {
            add(Box.createVerticalStrut(8));
            addTitle("Quest");
            addBody(quest.getTitle() + " [" + quest.getStatus() + "]");
            addBody(quest.getDescription());
            if (quest.getStatus() == QuestStatus.ACTIVE || quest.getStatus() == QuestStatus.READY) {
                addBody("Turns left: " + quest.getTurnsLeft()
                        + "  Progress: " + quest.getProgress());
            }
        }

        add(Box.createVerticalStrut(10));
        addTitle("Actions");

        boolean giftOk = tribe.getState().allowsGift();
        addButton("Send gift…", giftOk,
                giftOk ? null : tribe.getState().lockReason(),
                this::gift);

        boolean tradeOk = tribe.getState().allowsTrade()
                && tribe.getType() != TribeType.WARRIOR
                && !game.getTradeTracker().alreadyTradedThisTurn(
                new civ.model.trade.TribeTrade(tribe));
        addButton("Trade…", tradeOk,
                tradeLockReason(tradeOk),
                () -> TradeDialog.showTribe(this, game, tribe, controller::refresh));

        boolean takeOk = tribe.getState().allowsQuest()
                && !tribe.isQuestBlocked()
                && quest != null
                && quest.getStatus() == QuestStatus.AVAILABLE;
        addButton("Take quest", takeOk,
                takeOk ? null : questLockReason(),
                () -> {
                    game.takeQuest(tribe);
                    controller.refresh();
                    rebuild();
                });

        quest = tribe.getQuest();
        if (quest != null) {
            quest.refreshReady(game, tribe);
        }
        boolean deliverOk = quest != null && quest.canDeliver(game, tribe);
        addButton("Deliver quest", deliverOk,
                deliverOk ? null : (quest == null ? "No quest." : quest.deliverBlockedReason(game)),
                () -> {
                    game.deliverQuest(tribe);
                    controller.refresh();
                    rebuild();
                });

        boolean warOk = tribe.getState().allowsWarDeclaration();
        addButton("Declare war", warOk,
                warOk ? null : tribe.getState().lockReason(),
                () -> {
                    int answer = JOptionPane.showConfirmDialog(this,
                            "Declare war on " + tribe.getName() + "? Relation becomes −100.",
                            "War", JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        game.declareWar(tribe);
                        controller.refresh();
                        rebuild();
                    }
                });

        boolean peaceOk = game.canAskPeace(tribe);
        addButton("Ask peace (30f 30w 30i)", peaceOk,
                peaceOk ? null : "Needs enemy state and 30 food, wood and iron.",
                () -> {
                    game.askPeace(tribe);
                    controller.refresh();
                    rebuild();
                });

        boolean allyOk = AllianceRules.canAlly(tribe, game.getTribes());
        addButton("Ask alliance", allyOk,
                allyOk ? null : AllianceRules.lockReason(tribe, game.getTribes()),
                () -> {
                    game.askAlliance(tribe);
                    controller.refresh();
                    rebuild();
                });

        addButton("View rewards", true, null, () -> JOptionPane.showMessageDialog(this,
                tribe.getType().getRewardBlurb(),
                tribe.getName() + " rewards",
                JOptionPane.INFORMATION_MESSAGE));

        revalidate();
        repaint();
    }

    private String tradeLockReason(boolean ok) {
        if (ok) {
            return null;
        }
        if (tribe.getType() == TribeType.WARRIOR) {
            return "The Warrior tribe does not trade resources.";
        }
        if (game.getTradeTracker().alreadyTradedThisTurn(new civ.model.trade.TribeTrade(tribe))) {
            return "Already traded with this tribe this turn.";
        }
        return tribe.getState().lockReason();
    }

    private String questLockReason() {
        if (tribe.isQuestBlocked()) {
            return "Quests blocked for " + tribe.getQuestBlockTurns() + " more turns.";
        }
        if (tribe.getQuest() != null && tribe.getQuest().getStatus() != QuestStatus.AVAILABLE) {
            return "This tribe already has a quest in progress or finished.";
        }
        return tribe.getState().lockReason();
    }

    private void gift() {
        JComboBox<ResourceType> typeBox = new JComboBox<>(ResourceType.values());
        JSpinner amount = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Resource"));
        form.add(typeBox);
        form.add(new JLabel("Amount"));
        form.add(amount);
        int answer = JOptionPane.showConfirmDialog(this, form, "Send gift",
                JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        ResourceType type = (ResourceType) typeBox.getSelectedItem();
        int value = (Integer) amount.getValue();
        if (!game.getEmpire().getStock().canPay(type, value)) {
            JOptionPane.showMessageDialog(this, "Not enough " + type.getLabel() + ".");
            return;
        }
        if (tribe.giftRelationGain(type, value) <= 0) {
            JOptionPane.showMessageDialog(this, "Amount too small to change relation.");
            return;
        }
        game.gift(tribe, type, value);
        controller.refresh();
        rebuild();
    }

    private void addTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TITLE);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(4));
    }

    private void addBody(String text) {
        JLabel label = new JLabel("<html><div style='width:240px'>" + text + "</div></html>");
        label.setForeground(BODY);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(3));
    }

    private void addButton(String text, boolean enabled, String reason, Runnable action) {
        JButton button = new JButton("<html><center>" + text + "</center></html>");
        button.setEnabled(enabled);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMargin(new Insets(6, 8, 6, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        if (!enabled && reason != null) {
            button.setToolTipText(reason);
        }
        button.addActionListener(e -> action.run());
        add(button);
        add(Box.createVerticalStrut(4));
    }
}
