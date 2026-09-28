package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.controller.WorkbenchController;
import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.gui.util.NumberFormats;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Editable memory viewer. Row structure is only rebuilt when the visible
 * window (base/rows) changes; otherwise existing rows are updated in
 * place, so stepping/running stays cheap even with this tab open.
 */
public final class MemoryPanel extends JPanel {
    private final WorkbenchController controller;
    private final DefaultTableModel memoryModel = new DefaultTableModel(new Object[]{"Address", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }
    };
    private final JTable memoryTable = new JTable(memoryModel);
    private boolean refreshing;

    private final JSpinner baseSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Architecture.MEMORY_SIZE_BYTES - Architecture.INSTRUCTION_BYTES, Architecture.INSTRUCTION_BYTES));
    private final JSpinner rowsSpinner = new JSpinner(new SpinnerNumberModel(64, 4, 256, 4));
    private final JTextField jumpField = new JTextField(10);
    private final JTextField findField = new JTextField(10);

    public MemoryPanel(WorkbenchController controller) {
        super(new BorderLayout(6, 6));
        this.controller = controller;
        setBorder(BorderFactory.createTitledBorder("Memory"));

        memoryTable.setRowHeight(22);
        memoryTable.setFont(Theme.MONO);
        memoryTable.setFillsViewportHeight(true);
        memoryTable.getTableHeader().setReorderingAllowed(false);
        memoryModel.addTableModelListener(e -> {
            if (refreshing) {
                return;
            }
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1 && e.getFirstRow() >= 0) {
                onCellEdited(e.getFirstRow());
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.add(new JLabel("Base:"));
        controls.add(baseSpinner);
        controls.add(new JLabel("Rows:"));
        controls.add(rowsSpinner);
        controls.add(new JLabel("Jump:"));
        controls.add(jumpField);
        JButton jumpButton = new JButton("Go");
        jumpButton.addActionListener(e -> jumpToAddress());
        controls.add(jumpButton);
        controls.add(new JLabel("Find:"));
        controls.add(findField);
        JButton findButton = new JButton("Search");
        findButton.addActionListener(e -> searchValue());
        controls.add(findButton);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        controls.add(refreshButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(memoryTable), BorderLayout.CENTER);
    }

    public void refresh() {
        int base = (Integer) baseSpinner.getValue();
        int rows = (Integer) rowsSpinner.getValue();
        base = (base / Architecture.INSTRUCTION_BYTES) * Architecture.INSTRUCTION_BYTES;
        int maxAddresses = Math.max(0, Math.min(rows, (Architecture.MEMORY_SIZE_BYTES - base) / Architecture.INSTRUCTION_BYTES));

        refreshing = true;
        try {
            if (memoryModel.getRowCount() != maxAddresses) {
                memoryModel.setRowCount(0);
                for (int i = 0; i < maxAddresses; i++) {
                    memoryModel.addRow(new Object[]{"", "", ""});
                }
            }
            for (int i = 0; i < maxAddresses; i++) {
                int address = base + i * Architecture.INSTRUCTION_BYTES;
                long value = controller.readMemoryWord(address);
                memoryModel.setValueAt(String.format("0x%04X", address), i, 0);
                memoryModel.setValueAt(Long.toUnsignedString(value), i, 1);
                memoryModel.setValueAt("0x" + Long.toHexString(value).toUpperCase(), i, 2);
            }
        } finally {
            refreshing = false;
        }
    }

    private void onCellEdited(int row) {
        int base = ((Integer) baseSpinner.getValue() / Architecture.INSTRUCTION_BYTES) * Architecture.INSTRUCTION_BYTES;
        int address = base + row * Architecture.INSTRUCTION_BYTES;
        Object raw = memoryModel.getValueAt(row, 1);
        String normalized = raw == null ? "" : raw.toString().trim();
        try {
            long parsed;
            if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                parsed = Long.parseUnsignedLong(normalized.substring(2), 16);
            } else if (normalized.startsWith("-") || normalized.matches("[0-9]+")) {
                parsed = Long.parseLong(normalized, 10);
            } else {
                parsed = Long.parseUnsignedLong(normalized, 16);
            }
            controller.writeMemoryWord(address, parsed);
        } catch (NumberFormatException ex) {
            refresh();
        }
    }

    private void jumpToAddress() {
        Integer address = NumberFormats.parseFlexibleInteger(jumpField.getText());
        if (address == null) {
            return;
        }
        address = (address / Architecture.INSTRUCTION_BYTES) * Architecture.INSTRUCTION_BYTES;
        address = Math.max(0, Math.min(address, Architecture.MEMORY_SIZE_BYTES - Architecture.INSTRUCTION_BYTES));
        baseSpinner.setValue(address);
        refresh();
    }

    private void searchValue() {
        Integer target = NumberFormats.parseFlexibleInteger(findField.getText());
        if (target == null) {
            return;
        }
        int matchAddress = controller.findMemoryValue(target);
        if (matchAddress >= 0) {
            jumpField.setText(String.format("0x%04X", matchAddress));
            baseSpinner.setValue(matchAddress);
            refresh();
        }
    }
}
