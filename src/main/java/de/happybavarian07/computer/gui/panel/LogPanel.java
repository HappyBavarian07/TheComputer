package de.happybavarian07.computer.gui.panel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/** Append-only execution log. */
public final class LogPanel extends JPanel {
    private final JTextArea logArea = new JTextArea();

    public LogPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Execution log"));
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    public void append(String message) {
        logArea.append(message);
        if (!message.endsWith("\n")) {
            logArea.append("\n");
        }
    }
}
