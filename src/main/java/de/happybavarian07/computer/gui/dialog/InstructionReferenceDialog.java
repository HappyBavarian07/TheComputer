package de.happybavarian07.computer.gui.dialog;

import de.happybavarian07.computer.gui.assembly.InstructionFormatter;
import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.isa.OpCode;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Standalone reference window listing every opcode's syntax and description. */
public final class InstructionReferenceDialog {
    private InstructionReferenceDialog() {
    }

    public static void open(JFrame owner) {
        JFrame frame = new JFrame("Instruction reference");
        frame.setSize(980, 620);
        frame.setLocationRelativeTo(owner);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Opcode", "Syntax", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (OpCode opCode : OpCode.values()) {
            model.addRow(new Object[]{opCode.name(), InstructionFormatter.buildSyntax(opCode), InstructionFormatter.describeOpcode(opCode)});
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.BG_BASE);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
