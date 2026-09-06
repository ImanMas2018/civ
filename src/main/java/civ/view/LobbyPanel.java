package civ.view;

import civ.net.protocol.push.LobbyStateBroadcast;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class LobbyPanel extends JPanel {

    private final DefaultListModel<String> playerModel = new DefaultListModel<>();
    private final JList<String> playerList = new JList<>(playerModel);
    private final JComboBox<MapOption> mapCombo = new JComboBox<>();
    private final JCheckBox readyBox = new JCheckBox("Ready");
    private final JButton startButton = new JButton("Start Game");
    private final JLabel noticeLabel = new JLabel(" ", SwingConstants.LEFT);
    private final ChatPanel chatPanel;

    private final Consumer<Boolean> onReady;
    private final Consumer<String> onSelectMap;
    private final Runnable onStart;
    private final Runnable onLeave;

    private boolean applyingBroadcast;
    private boolean localIsHost;
    private String localName = "";

    public LobbyPanel(ChatPanel chatPanel,
                      Consumer<Boolean> onReady,
                      Consumer<String> onSelectMap,
                      Runnable onStart,
                      Runnable onLeave) {
        this.chatPanel = chatPanel;
        this.onReady = onReady;
        this.onSelectMap = onSelectMap;
        this.onStart = onStart;
        this.onLeave = onLeave;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(36, 46, 62));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Lobby");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);

        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane listScroll = new JScrollPane(playerList);
        listScroll.setPreferredSize(new Dimension(280, 220));
        listScroll.setBorder(BorderFactory.createTitledBorder("Players"));

        mapCombo.setMaximumSize(new Dimension(280, 28));
        mapCombo.addItem(new MapOption("crossroads.map", "Crossroads"));
        mapCombo.addItem(new MapOption("two-rivers.map", "Two Rivers"));
        mapCombo.addItem(new MapOption("highlands.map", "Highlands"));
        mapCombo.addActionListener(e -> {
            if (applyingBroadcast) {
                return;
            }
            MapOption selected = (MapOption) mapCombo.getSelectedItem();
            if (selected != null && localIsHost) {
                onSelectMap.accept(selected.resourceName);
            }
        });

        readyBox.setOpaque(false);
        readyBox.setForeground(Color.WHITE);
        readyBox.addActionListener(e -> {
            if (!applyingBroadcast) {
                onReady.accept(readyBox.isSelected());
            }
        });

        startButton.setEnabled(false);
        startButton.addActionListener(e -> onStart.run());

        JButton leave = new JButton("Leave Lobby");
        leave.addActionListener(e -> onLeave.run());

        noticeLabel.setForeground(new Color(255, 220, 160));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(12));
        left.add(listScroll);
        left.add(Box.createVerticalStrut(12));
        left.add(labeled("Map", mapCombo));
        left.add(Box.createVerticalStrut(8));
        left.add(readyBox);
        left.add(Box.createVerticalStrut(8));
        left.add(startButton);
        left.add(Box.createVerticalStrut(8));
        left.add(leave);
        left.add(Box.createVerticalStrut(8));
        left.add(noticeLabel);

        add(left, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);
    }

    public void setLocalName(String name) {
        this.localName = name == null ? "" : name;
    }

    public void showNotice(String text) {
        noticeLabel.setText(text == null ? " " : text);
        if (text != null && !text.isBlank()) {
            chatPanel.appendNotice(text);
        }
    }

    public void applyState(LobbyStateBroadcast state) {
        applyingBroadcast = true;
        try {
            playerModel.clear();
            localIsHost = false;
            boolean localReady = false;
            boolean allReady = !state.getSeats().isEmpty();

            for (LobbyStateBroadcast.SeatDto seat : state.getSeats()) {
                String label = seat.name;
                if (seat.host) {
                    label += " (Host)";
                }
                label += seat.ready ? " — Ready" : " — Not Ready";
                playerModel.addElement(label);

                if (seat.name.equals(localName)) {
                    localIsHost = seat.host;
                    localReady = seat.ready;
                }
                if (!seat.ready) {
                    allReady = false;
                }
            }

            readyBox.setSelected(localReady);
            mapCombo.setEnabled(localIsHost);
            selectMapQuietly(state.getSelectedMap());

            startButton.setVisible(localIsHost);
            startButton.setEnabled(localIsHost
                    && allReady
                    && state.getSeats().size() >= 2);
        } finally {
            applyingBroadcast = false;
        }
    }

    private void selectMapQuietly(String resourceName) {
        for (int i = 0; i < mapCombo.getItemCount(); i++) {
            MapOption option = mapCombo.getItemAt(i);
            if (option.resourceName.equals(resourceName)) {
                mapCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static JPanel labeled(String label, JComboBox<?> combo) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(280, 32));
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(Color.LIGHT_GRAY);
        row.add(jLabel, BorderLayout.WEST);
        row.add(combo, BorderLayout.CENTER);
        return row;
    }

    private static final class MapOption {
        final String resourceName;
        final String displayName;

        MapOption(String resourceName, String displayName) {
            this.resourceName = resourceName;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
