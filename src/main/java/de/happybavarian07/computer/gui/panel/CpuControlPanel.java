package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.controller.WorkbenchController;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.IntConsumer;

/**
 * Reset/Step/Step N/Run/Stop controls. Buttons fill their grid cell instead
 * of a hardcoded pixel size, so this actually resizes instead of clipping.
 */
public final class CpuControlPanel extends JPanel {
    private final JButton resetButton = new JButton("Reset");
    private final JButton stepButton = new JButton("Step");
    private final JButton stepManyButton = new JButton("Step N");
    private final JButton runButton = new JButton("Run");
    private final JButton stopButton = new JButton("Stop");
    private final JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private final JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(256, 1, 10_000_000, 64));
    private final JCheckBox maxThroughputBox = new JCheckBox("Max");
    private final JLabel clockLabel = new JLabel("Clock: idle");

    public CpuControlPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("CPU controls"));
        for (JButton button : new JButton[]{resetButton, stepButton, stepManyButton, runButton, stopButton}) {
            button.setToolTipText(button.getText());
        }

        JPanel primary = new JPanel(new GridLayout(1, 3, 8, 8));
        primary.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        primary.add(stepButton);
        primary.add(runButton);
        primary.add(stopButton);

        JPanel secondary = new JPanel(new GridLayout(1, 2, 8, 8));
        secondary.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        secondary.add(resetButton);
        secondary.add(stepManyButton);

        JPanel stepConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        stepConfig.add(new JLabel("Step count:"));
        stepConfig.add(stepSpinner);
        stepConfig.add(new JLabel("Steps/tick:"));
        stepConfig.add(speedSpinner);
        maxThroughputBox.setToolTipText("Max throughput: run as many steps as fit in each ~25 ms slice instead of a fixed count per tick");
        maxThroughputBox.addActionListener(e -> speedSpinner.setEnabled(!maxThroughputBox.isSelected()));
        stepConfig.add(maxThroughputBox);

        JPanel southStack = new JPanel();
        southStack.setLayout(new BoxLayout(southStack, BoxLayout.Y_AXIS));
        southStack.add(stepConfig);
        southStack.add(clockLabel);

        JPanel body = new JPanel(new BorderLayout());
        body.add(primary, BorderLayout.NORTH);
        body.add(secondary, BorderLayout.CENTER);
        body.add(southStack, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    public void setResetAction(Runnable action) {
        resetButton.addActionListener(e -> action.run());
    }

    public void setStepAction(Runnable action) {
        stepButton.addActionListener(e -> action.run());
    }

    public void setStepManyAction(IntConsumer action) {
        stepManyButton.addActionListener(e -> action.accept((Integer) stepSpinner.getValue()));
    }

    public void setRunAction(IntConsumer action) {
        runButton.addActionListener(e -> action.accept(stepsPerTick()));
    }

    /** Steps per timer tick, or {@link WorkbenchController#MAX_THROUGHPUT} when the Max box is ticked. */
    public int stepsPerTick() {
        return maxThroughputBox.isSelected() ? WorkbenchController.MAX_THROUGHPUT : (Integer) speedSpinner.getValue();
    }

    public void setStopAction(Runnable action) {
        stopButton.addActionListener(e -> action.run());
    }

    public void setClockText(String text) {
        clockLabel.setText(text);
    }
}
