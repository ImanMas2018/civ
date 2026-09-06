package civ.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Host / Join form. No sockets here — callbacks hand credentials to MainWindow.
 */
public class ConnectPanel extends JPanel {

    private final JTextField username = new JTextField("Player", 16);
    private final JTextField host = new JTextField("127.0.0.1", 16);
    private final JTextField port = new JTextField("5555", 8);
    private final JRadioButton hostMode = new JRadioButton("Host a game", true);
    private final JRadioButton joinMode = new JRadioButton("Join a game");
    private final JLabel status = new JLabel(" ");

    public ConnectPanel(Consumer<Integer> onHost,
                        BiConsumer<String, Integer> onJoin,
                        Runnable onBack) {
        setBackground(new Color(30, 40, 55));
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Multiplayer", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        ButtonGroup modes = new ButtonGroup();
        modes.add(hostMode);
        modes.add(joinMode);
        styleRadio(hostMode);
        styleRadio(joinMode);

        hostMode.addActionListener(e -> updateModeFields());
        joinMode.addActionListener(e -> updateModeFields());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(labeled("Username", username));
        form.add(Box.createVerticalStrut(8));
        form.add(hostMode);
        form.add(joinMode);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Server IP", host));
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Port", port));

        JButton connect = new JButton("Connect");
        connect.setAlignmentX(Component.CENTER_ALIGNMENT);
        connect.setMaximumSize(new Dimension(240, 44));
        connect.setPreferredSize(new Dimension(240, 44));
        connect.addActionListener(e -> {
            status.setText(" ");
            String name = username.getText().trim();
            if (name.isEmpty()) {
                status.setText("Enter a username.");
                return;
            }
            int portNumber;
            try {
                portNumber = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException ex) {
                status.setText("Port must be a number.");
                return;
            }
            if (portNumber < 1 || portNumber > 65535) {
                status.setText("Port must be between 1 and 65535.");
                return;
            }

            if (hostMode.isSelected()) {
                onHost.accept(portNumber);
            } else {
                String ip = host.getText().trim();
                if (ip.isEmpty()) {
                    status.setText("Enter a server IP.");
                    return;
                }
                onJoin.accept(ip, portNumber);
            }
        });

        JButton back = new JButton("Back");
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.setMaximumSize(new Dimension(240, 44));
        back.addActionListener(e -> onBack.run());

        status.setForeground(new Color(255, 180, 180));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        column.add(title);
        column.add(Box.createVerticalStrut(24));
        column.add(form);
        column.add(Box.createVerticalStrut(16));
        column.add(connect);
        column.add(Box.createVerticalStrut(8));
        column.add(back);
        column.add(Box.createVerticalStrut(12));
        column.add(status);

        add(column);
        updateModeFields();
    }

    public String getUsername() {
        return username.getText().trim();
    }

    public void setStatus(String text) {
        status.setText(text == null ? " " : text);
    }

    public void preferJoinMode() {
        joinMode.setSelected(true);
        updateModeFields();
    }

    private void updateModeFields() {
        host.setEnabled(joinMode.isSelected());
    }

    private static void styleRadio(JRadioButton button) {
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static JPanel labeled(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(320, 32));
        JLabel jLabel = new JLabel(label);
        jLabel.setForeground(Color.LIGHT_GRAY);
        jLabel.setPreferredSize(new Dimension(90, 24));
        row.add(jLabel, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }
}
