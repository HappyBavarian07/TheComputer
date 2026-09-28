package de.happybavarian07.computer.gui.dialog;

import de.happybavarian07.computer.gui.assembly.ConditionReference;
import de.happybavarian07.computer.gui.assembly.InstructionFormatter;
import de.happybavarian07.computer.gui.assembly.OpcodeReference;
import de.happybavarian07.computer.gui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Master-detail reference for both opcodes and condition codes: a
 * filterable list on the left, full verified detail on the right.
 * Replaces an earlier version that crammed a one-line description into a
 * table cell (truncated, and not enough room to say anything useful) --
 * this shows the same per-instruction facts documented in
 * docs/ASSEMBLY_GUIDE.md §5-§6, not a shorter rewrite of them.
 */
public final class InstructionReferenceDialog {
    private InstructionReferenceDialog() {
    }

    public static void open(JFrame owner) {
        JFrame frame = new JFrame("Instruction reference");
        frame.setSize(1150, 680);
        frame.setLocationRelativeTo(owner);
        frame.getContentPane().setBackground(Theme.BG_BASE);
        frame.setLayout(new BorderLayout());

        List<Object> entries = new ArrayList<>();
        entries.addAll(OpcodeReference.ALL);
        entries.addAll(ConditionReference.ALL);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Name", "Category", "Syntax"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Object entry : entries) {
            model.addRow(new Object[]{nameOf(entry), categoryOf(entry), syntaxOf(entry)});
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(Theme.MONO);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JEditorPane detail = new JEditorPane();
        detail.setContentType("text/html");
        detail.setEditable(false);
        detail.setBackground(Theme.BG_SURFACE);
        detail.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            detail.setText(renderDetail(entries.get(modelRow)));
            detail.setCaretPosition(0);
        });

        JTextField searchField = new JTextField();
        searchField.setToolTipText("Filter by name or category (e.g. \"MI\", \"condition\", \"memory\")");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void applyFilter() {
                String text = searchField.getText();
                if (text == null || text.isBlank()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                }
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.getViewport().setBackground(Theme.BG_SURFACE);
        detailScroll.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, detailScroll);
        split.setResizeWeight(0.4);
        split.setBorder(BorderFactory.createEmptyBorder());

        JLabel footer = new JLabel("<html><body style='padding:6px 12px;'>" + escapeHtml(OpcodeReference.IMMEDIATE_RANGE_NOTE) + "</body></html>");
        footer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        footer.setForeground(Theme.TEXT_FAINT);

        frame.add(split, BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);

        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
        frame.setVisible(true);
    }

    private static String nameOf(Object entry) {
        if (entry instanceof OpcodeReference op) {
            return op.opCode().name();
        }
        ConditionReference cond = (ConditionReference) entry;
        return cond.condition().name();
    }

    private static String categoryOf(Object entry) {
        if (entry instanceof OpcodeReference op) {
            return op.category();
        }
        return "Condition codes";
    }

    private static String syntaxOf(Object entry) {
        if (entry instanceof OpcodeReference op) {
            return InstructionFormatter.buildSyntax(op.opCode());
        }
        ConditionReference cond = (ConditionReference) entry;
        return "<opcode>" + cond.condition().name().toLowerCase();
    }

    private static String renderDetail(Object entry) {
        if (entry instanceof OpcodeReference op) {
            return renderOpcodeDetail(op);
        }
        return renderConditionDetail((ConditionReference) entry);
    }

    private static String renderOpcodeDetail(OpcodeReference ref) {
        StringBuilder html = new StringBuilder();
        appendHeader(html, ref.opCode().name(), ref.category());
        appendCodeBlock(html, InstructionFormatter.buildSyntax(ref.opCode()));
        appendField(html, "Effect", ref.effect());
        appendField(html, "Flags updated", ref.flags());
        appendExample(html, ref.example());
        appendNotes(html, ref.notes());
        html.append("</body></html>");
        return html.toString();
    }

    private static String renderConditionDetail(ConditionReference ref) {
        StringBuilder html = new StringBuilder();
        appendHeader(html, ref.condition().name(), "Condition codes");
        appendCodeBlock(html, "<opcode>" + ref.condition().name().toLowerCase() + "   (e.g. jmp" + ref.condition().name().toLowerCase() + ", add" + ref.condition().name().toLowerCase() + ", ...)");
        appendField(html, "Meaning", ref.meaning());
        appendField(html, "Flag formula", ref.formula());
        if (!"—".equals(ref.aliases())) {
            appendField(html, "Aliases", ref.aliases() + "  (e.g. jmp" + ref.aliases().toLowerCase() + " also works)");
        }
        appendExample(html, ref.example());
        appendNotes(html, ref.notes());
        html.append("</body></html>");
        return html.toString();
    }

    private static void appendHeader(StringBuilder html, String title, String category) {
        String accent = hex(Theme.TITLE);
        String text = hex(Theme.TEXT_BRIGHT);
        String secondary = hex(Theme.TEXT_SECONDARY);
        html.append("<html><body style='font-family:sans-serif; color:").append(text).append(";'>");
        html.append("<div style='font-size:20px; font-weight:bold; color:").append(accent).append(";'>")
                .append(escapeHtml(title)).append("</div>");
        html.append("<div style='color:").append(secondary).append("; margin-bottom:10px;'>")
                .append(escapeHtml(category)).append("</div>");
    }

    private static void appendCodeBlock(StringBuilder html, String code) {
        String codeBg = hex(Theme.BG_CHIP);
        html.append("<div style='background:").append(codeBg).append("; padding:6px 10px; font-family:monospace; margin-bottom:12px;'>")
                .append(multiline(code)).append("</div>");
    }

    private static void appendField(StringBuilder html, String label, String value) {
        html.append("<p><b>").append(escapeHtml(label)).append("</b><br>").append(escapeHtml(value)).append("</p>");
    }

    private static void appendExample(StringBuilder html, String example) {
        String codeBg = hex(Theme.BG_CHIP);
        html.append("<p><b>Example</b><br>")
                .append("<span style='font-family:monospace; background:").append(codeBg).append("; padding:2px 6px;'>")
                .append(multiline(example)).append("</span></p>");
    }

    private static void appendNotes(StringBuilder html, String notes) {
        if (notes == null || notes.isBlank()) {
            return;
        }
        String noteBg = hex(new Color(56, 46, 30));
        String noteFg = hex(new Color(224, 196, 150));
        html.append("<div style='background:").append(noteBg).append("; color:").append(noteFg)
                .append("; padding:8px 10px; margin-top:10px; border-radius:4px;'>")
                .append(escapeHtml(notes)).append("</div>");
    }

    private static String multiline(String value) {
        return escapeHtml(value).replace("\n", "<br>");
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
