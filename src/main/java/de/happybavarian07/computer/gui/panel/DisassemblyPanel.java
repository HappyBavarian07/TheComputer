package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.controller.WorkbenchController;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Live window of decoded instructions centered on the current PC. */
public final class DisassemblyPanel extends JPanel {
    // rows decoded before the PC and in total; the table scrolls, so the whole window is reachable
    private static final int LEAD_ROWS = 16;
    private static final int WINDOW_ROWS = 96;

    private final WorkbenchController controller;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Address", "Word", "Instruction"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public DisassemblyPanel(WorkbenchController controller) {
        super(new BorderLayout(6, 6));
        this.controller = controller;
        setBorder(BorderFactory.createTitledBorder("Live disassembly"));

        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(420);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refresh(int pc) {
        model.setRowCount(0);
        int base = Math.max(0, pc - (Architecture.INSTRUCTION_BYTES * LEAD_ROWS));
        base = (base / Architecture.INSTRUCTION_BYTES) * Architecture.INSTRUCTION_BYTES;
        int rows = WINDOW_ROWS;
        int selectedRow = -1;

        for (int i = 0; i < rows; i++) {
            int address = base + i * Architecture.INSTRUCTION_BYTES;
            if (address >= Architecture.MEMORY_SIZE_BYTES) {
                break;
            }
            long raw = controller.readMemoryWord(address);
            model.addRow(new Object[]{
                    String.format("0x%04X", address),
                    String.format("0x%016X", raw),
                    controller.getDisassembler().disassemble(raw)
            });
            if (address == pc) {
                selectedRow = i;
            }
        }

        if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
            table.setRowSelectionInterval(selectedRow, selectedRow);
            int row = selectedRow;
            SwingUtilities.invokeLater(() -> table.scrollRectToVisible(table.getCellRect(row, 0, true)));
        }
    }
}
