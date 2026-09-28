package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.controller.WorkbenchController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Perf tooling, kept separate from the primary run controls so it doesn't
 * compete for space with the buttons someone reaches for on every step.
 */
public final class ToolsPanel extends JPanel {
    private final JLabel coreClockLabel = new JLabel("Core: --");

    public ToolsPanel(WorkbenchController controller) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Tools"));

        JButton benchButton = new JButton("Benchmark core");
        benchButton.addActionListener(e -> {
            coreClockLabel.setText("Core: benchmarking...");
            controller.benchmarkCoreAsync(result -> coreClockLabel.setText(
                    String.format("Core: %,.0f Hz%s (%.2f MIPS, %,d steps in %.2f s)",
                            result.hz(), result.capped() ? " (loop capped)" : "", result.mips(), result.steps(), result.elapsedSeconds())));
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        row.add(benchButton);
        row.add(coreClockLabel);
        add(row, BorderLayout.NORTH);
    }
}
