package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.cpu.registers.RegisterFile;
import de.happybavarian07.computer.cpu.registers.SpecialRegisters;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.gui.util.NumberFormats;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** General-purpose registers alongside PC/SP/IR and the flag bits. */
public final class RegisterPanel extends JPanel {
    private final DefaultTableModel registerModel = new DefaultTableModel(new Object[]{"Register", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable registerTable = new JTable(registerModel);

    private final JTextField pcField = readOnlyField();
    private final JTextField spField = readOnlyField();
    private final JTextField irField = readOnlyField();
    private final JLabel flagZ = flagLabel("Z");
    private final JLabel flagN = flagLabel("N");
    private final JLabel flagC = flagLabel("C");
    private final JLabel flagV = flagLabel("V");
    private final JLabel haltedLabel = new JLabel();

    public RegisterPanel() {
        super(new BorderLayout());

        registerTable.setRowHeight(22);
        registerTable.setFillsViewportHeight(true);
        JPanel registersBox = new JPanel(new BorderLayout());
        registersBox.setBorder(BorderFactory.createTitledBorder("General-purpose registers"));
        registersBox.add(new JScrollPane(registerTable), BorderLayout.CENTER);

        JPanel specialBox = new JPanel(new BorderLayout());
        specialBox.setBorder(BorderFactory.createTitledBorder("Special registers"));
        specialBox.add(buildSpecialRegistersView(), BorderLayout.CENTER);
        specialBox.setPreferredSize(new java.awt.Dimension(0, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, specialBox, registersBox);
        split.setResizeWeight(0.0);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildSpecialRegistersView() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        grid.add(new JLabel("PC"));
        grid.add(pcField);
        grid.add(new JLabel("SP"));
        grid.add(spField);
        grid.add(new JLabel("IR"));
        grid.add(irField);

        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        flags.add(flagZ);
        flags.add(flagN);
        flags.add(flagC);
        flags.add(flagV);
        flags.add(new JLabel("State:"));
        flags.add(haltedLabel);

        panel.add(grid, BorderLayout.NORTH);
        panel.add(flags, BorderLayout.SOUTH);
        return panel;
    }

    private static JTextField readOnlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setBackground(Theme.BG_SURFACE);
        field.setForeground(Theme.TEXT_BRIGHT);
        field.setCaretColor(Theme.TEXT_BRIGHT);
        return field;
    }

    private static JLabel flagLabel(String text) {
        JLabel label = new JLabel(text + ": off");
        label.setOpaque(true);
        label.setBackground(Theme.BG_CHIP);
        label.setForeground(Theme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    public void refresh(RegisterFile registerFile, SpecialRegisters special, boolean halted) {
        registerModel.setRowCount(0);
        Word value = new Word();
        for (int i = 0; i < Architecture.GPR_COUNT; i++) {
            registerFile.read(i, value);
            registerModel.addRow(new Object[]{"R" + i, value.getAsLong(), "0x" + Long.toHexString(value.getAsLong()).toUpperCase()});
        }

        pcField.setText(NumberFormats.formatRegisterValue(special.getPC().getAsInt()));
        spField.setText(NumberFormats.formatRegisterValue(special.getSP().getAsInt()));
        irField.setText(NumberFormats.formatRegisterValue(special.getIR().getAsLong()));
        updateFlag(flagZ, special.isZero());
        updateFlag(flagN, special.isNegative());
        updateFlag(flagC, special.isCarry());
        updateFlag(flagV, special.isOverflow());
        haltedLabel.setText(halted ? "HALTED" : "RUNNING");
    }

    private void updateFlag(JLabel label, boolean enabled) {
        String flagName = String.valueOf(label.getText().charAt(0));
        label.setText(flagName + ": " + (enabled ? "on" : "off"));
        label.setBackground(enabled ? Theme.ACCENT_FILL : Theme.BG_CHIP);
    }
}
