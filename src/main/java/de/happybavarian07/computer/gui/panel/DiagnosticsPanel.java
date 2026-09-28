package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Live assembler pipeline status (tokens/parsed/resolved/encoded) below the editor. */
public final class DiagnosticsPanel extends JPanel {
    private final JLabel stageLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JLabel tokensLabel = new JLabel();
    private final JLabel parsedLabel = new JLabel();
    private final JLabel resolvedLabel = new JLabel();
    private final JLabel encodedLabel = new JLabel();

    public DiagnosticsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Assembler diagnostics"));

        stageLabel.setOpaque(true);
        stageLabel.setBackground(Theme.BG_CHIP);
        stageLabel.setForeground(Theme.TEXT);
        stageLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        messageLabel.setOpaque(true);
        messageLabel.setBackground(Theme.BG_SURFACE);
        messageLabel.setForeground(Theme.TEXT_SECONDARY);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel cards = new JPanel(new GridLayout(2, 2, 8, 8));
        cards.add(metricCard("Tokens", tokensLabel));
        cards.add(metricCard("Parsed", parsedLabel));
        cards.add(metricCard("Resolved", resolvedLabel));
        cards.add(metricCard("Encoded", encodedLabel));

        JPanel summary = new JPanel(new BorderLayout(8, 8));
        summary.add(stageLabel, BorderLayout.NORTH);
        summary.add(messageLabel, BorderLayout.CENTER);

        add(cards, BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
    }

    private JPanel metricCard(String title, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Theme.TITLE);

        valueLabel.setOpaque(true);
        valueLabel.setBackground(Theme.BG_SURFACE);
        valueLabel.setForeground(Theme.TEXT_BRIGHT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    public void showReady(int tokens, int parsed, int resolved, int encoded, String message) {
        stageLabel.setText("Ready");
        tokensLabel.setText(Integer.toString(tokens));
        parsedLabel.setText(Integer.toString(parsed));
        resolvedLabel.setText(Integer.toString(resolved));
        encodedLabel.setText(Integer.toString(encoded));
        messageLabel.setText("<html>" + escapeHtml(message) + "</html>");
    }

    public void showIdle(String message) {
        stageLabel.setText("Idle");
        tokensLabel.setText("0");
        parsedLabel.setText("0");
        resolvedLabel.setText("0");
        encodedLabel.setText("0");
        messageLabel.setText("<html>" + escapeHtml(message) + "</html>");
    }

    public void showError(String stage, String summary, String message) {
        stageLabel.setText(stage);
        tokensLabel.setText("0");
        parsedLabel.setText("0");
        resolvedLabel.setText("0");
        encodedLabel.setText("0");
        messageLabel.setText("<html>" + escapeHtml(summary) + "<br>" + escapeHtml(message) + "</html>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
