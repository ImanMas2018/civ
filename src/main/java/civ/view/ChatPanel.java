package civ.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Written once and reused in the lobby and (later) the in-game HUD.
 * Sends chat text through a callback so this panel never imports civ.net.
 */
public class ChatPanel extends JPanel {

    private final JTextArea transcript = new JTextArea();
    private final JTextField input = new JTextField();
    private final Consumer<String> onSend;

    public ChatPanel(Consumer<String> onSend) {
        this.onSend = onSend;
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Chat"));

        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setFont(new Font("SansSerif", Font.PLAIN, 13));
        transcript.setBackground(new Color(248, 248, 248));

        add(new JScrollPane(transcript), BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        input.addActionListener(event -> {
            String text = input.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            onSend.accept(text);
            input.setText("");
        });
    }

    /** Called on the EDT by the message dispatcher. */
    public void append(String time, String sender, String text) {
        transcript.append("[" + time + "] " + sender + ": " + text + "\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    public void appendNotice(String text) {
        transcript.append("* " + text + "\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    public void clear() {
        transcript.setText("");
    }
}
