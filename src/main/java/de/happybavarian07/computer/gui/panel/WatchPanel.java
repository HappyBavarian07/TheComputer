package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.gui.controller.WorkbenchController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Ad hoc watch expressions: registers (r0.., pc, sp, ir, flags) or raw addresses. */
public final class WatchPanel extends JPanel {
    private final WorkbenchController controller;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Target", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField watchField = new JTextField("r0, r1, sp, 0x0000, 0x0008", 28);

    public WatchPanel(WorkbenchController controller) {
        super(new BorderLayout(6, 6));
        this.controller = controller;
        setBorder(BorderFactory.createTitledBorder("Watch"));

        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        watchField.setToolTipText("Comma or space separated registers and addresses");
        watchField.addActionListener(e -> refresh());
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> refresh());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Watch:"));
        controls.add(watchField);
        controls.add(applyButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refresh() {
        model.setRowCount(0);
        String[] tokens = watchField.getText() == null ? new String[0] : watchField.getText().split("[,\\s]+");
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalized = token.trim();
            Long value = controller.resolveWatchValue(normalized);
            model.addRow(new Object[]{
                    normalized,
                    value == null ? "invalid" : Long.toUnsignedString(value),
                    value == null ? "invalid" : String.format("0x%016X", value)
            });
        }
    }
}
