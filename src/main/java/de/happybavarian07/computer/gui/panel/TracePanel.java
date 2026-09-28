package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

/** Rolling log of the last N executed instructions (address, decoded form, next PC). */
public final class TracePanel extends JPanel {
    private final JTextArea traceArea = new JTextArea(7, 40);

    public TracePanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Instruction trace"));

        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        traceArea.setBackground(Theme.BG_BASE);
        traceArea.setForeground(Theme.TEXT_BRIGHT);
        traceArea.setCaretColor(Theme.TEXT_BRIGHT);
        traceArea.setLineWrap(false);

        add(new JScrollPane(traceArea), BorderLayout.CENTER);
    }

    public void refresh(List<String> entries) {
        StringBuilder builder = new StringBuilder();
        for (String entry : entries) {
            builder.append(entry).append('\n');
        }
        traceArea.setText(builder.toString());
    }
}
