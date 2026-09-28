package de.happybavarian07.computer.gui.assembly;

import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ctrl+Space completion for the assembly editor: opcodes, operands (context
 * aware per {@link OperandMapping}), directives, and labels found in the
 * current source. Owns its own popup/table so it's a self-contained,
 * reusable component rather than fields scattered across the workbench.
 */
public final class AssemblyAutoComplete {
    private static final List<String> DIRECTIVE_ITEMS = List.of(".word", ".byte", ".ascii", ".org", ".align");
    private static final List<String> SPECIAL_REGISTERS = List.of("pc", "sp", "ir", "flags");
    private static final Pattern LABEL_REFERENCE_PATTERN = Pattern.compile("(?m)^\\s*([A-Za-z_][\\w]*)\\s*:");

    private final JTextPane editor;
    private final Runnable afterAccept;
    private final JPopupMenu popup = new JPopupMenu();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Completion", "Usage", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final javax.swing.JLabel detailLabel = new javax.swing.JLabel();

    public AssemblyAutoComplete(JTextPane editor, Runnable afterAccept) {
        this.editor = editor;
        this.afterAccept = afterAccept;
        installTable();
        installKeyBindings();
    }

    private void installTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setBackground(Theme.BG_SURFACE);
        table.setForeground(Theme.TEXT_BRIGHT);
        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(java.awt.Color.WHITE);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Theme.BG_BASE);
        table.getTableHeader().setForeground(Theme.TEXT);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(380);
        table.setFocusable(false);
        table.getSelectionModel().addListSelectionListener(e -> updateDetail());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    acceptSelection();
                }
            }
        });
        bindTableKey(KeyEvent.VK_ENTER);
        bindTableKey(KeyEvent.VK_TAB);
    }

    private void bindTableKey(int keyCode) {
        table.getInputMap().put(KeyStroke.getKeyStroke(keyCode, 0), "accept-" + keyCode);
        table.getActionMap().put("accept-" + keyCode, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptSelection();
            }
        });
    }

    private void installKeyBindings() {
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "show-completion");
        editor.getActionMap().put("show-completion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                show();
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "accept-completion-or-tab");
        editor.getActionMap().put("accept-completion-or-tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    acceptSelection();
                } else {
                    insertText("    ");
                }
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept-completion-or-enter");
        editor.getActionMap().put("accept-completion-or-enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    acceptSelection();
                } else {
                    insertText("\n");
                }
            }
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshIfVisible();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshIfVisible();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private void insertText(String text) {
        try {
            editor.getDocument().insertString(editor.getCaretPosition(), text, null);
        } catch (Exception ignored) {
            // best-effort; a failed insert here is not worth surfacing as an error dialog
        }
    }

    private void refreshIfVisible() {
        if (!popup.isVisible()) {
            return;
        }
        SwingUtilities.invokeLater(this::show);
    }

    private void show() {
        CompletionContext context = analyzeContext();
        List<Suggestion> suggestions = suggestionsFor(context);
        if (suggestions.isEmpty()) {
            popup.setVisible(false);
            return;
        }

        model.setRowCount(0);
        for (Suggestion suggestion : suggestions) {
            model.addRow(new Object[]{suggestion.text(), suggestion.usage(), suggestion.description()});
        }
        table.setRowSelectionInterval(0, 0);
        popup.removeAll();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.BG_BASE);
        int popupWidth = context.mode() == CompletionMode.OPCODE ? 980 : 1120;
        scrollPane.setPreferredSize(new Dimension(popupWidth, 320));

        detailLabel.setOpaque(true);
        detailLabel.setBackground(Theme.BG_BASE);
        detailLabel.setForeground(Theme.TEXT_SECONDARY);
        detailLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        var panel = new javax.swing.JPanel(new BorderLayout(8, 8));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(detailLabel, BorderLayout.SOUTH);
        popup.add(panel);
        popup.setFocusable(false);
        updateDetail();

        try {
            Rectangle2D caret = editor.modelToView2D(editor.getCaretPosition());
            if (caret != null) {
                popup.show(editor, (int) caret.getX(), (int) caret.getMaxY());
            } else {
                popup.show(editor, 0, editor.getHeight());
            }
        } catch (Exception ex) {
            popup.show(editor, 0, editor.getHeight());
        }
    }

    private void updateDetail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            detailLabel.setText(" ");
            return;
        }
        String text = String.valueOf(model.getValueAt(row, 0));
        String usage = String.valueOf(model.getValueAt(row, 1));
        String description = String.valueOf(model.getValueAt(row, 2));
        detailLabel.setText("<html><b>" + escapeHtml(text) + "</b> &nbsp; " + escapeHtml(usage) + " &nbsp; " + escapeHtml(description) + "</html>");
    }

    private void acceptSelection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            popup.setVisible(false);
            return;
        }
        String insertion = String.valueOf(model.getValueAt(row, 0));
        String prefix = currentPrefix();
        int caret = editor.getCaretPosition();
        int start = caret - prefix.length();
        try {
            var doc = editor.getDocument();
            doc.remove(start, prefix.length());
            doc.insertString(start, insertion, null);
            editor.setCaretPosition(start + insertion.length());
            popup.setVisible(false);
            afterAccept.run();
        } catch (Exception ignored) {
            popup.setVisible(false);
        }
    }

    private String currentPrefix() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        int start = caret;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                start--;
            } else {
                break;
            }
        }
        return text.substring(start, caret);
    }

    // --- context analysis -------------------------------------------------

    private enum CompletionMode {OPCODE, OPERAND, DIRECTIVE, GENERAL}

    private enum OperandRole {NONE, REGISTER, IMMEDIATE}

    private record CompletionContext(CompletionMode mode, OpCode opcode, OperandMapping mapping, int operandIndex) {
    }

    private record Suggestion(String text, String usage, String description) {
    }

    private CompletionContext analyzeContext() {
        String source = editor.getText();
        int caret = editor.getCaretPosition();
        int lineStart = Math.max(0, source.lastIndexOf('\n', Math.max(0, caret - 1)) + 1);
        String linePrefix = source.substring(lineStart, caret);
        int commentIndex = AssemblySyntaxHighlighter.findCommentIndex(linePrefix);
        if (commentIndex >= 0) {
            linePrefix = linePrefix.substring(0, commentIndex);
        }
        String trimmed = linePrefix.stripLeading();
        if (trimmed.isEmpty()) {
            return new CompletionContext(CompletionMode.OPCODE, null, OperandMapping.NONE, 0);
        }
        if (trimmed.startsWith(".")) {
            return new CompletionContext(CompletionMode.DIRECTIVE, null, OperandMapping.NONE, 0);
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length > 0 && tokens[0].endsWith(":")) {
            int labelEnd = trimmed.indexOf(tokens[0]) + tokens[0].length();
            String remainder = trimmed.substring(labelEnd).stripLeading();
            if (remainder.isEmpty()) {
                return new CompletionContext(CompletionMode.OPCODE, null, OperandMapping.NONE, 0);
            }
            trimmed = remainder;
            tokens = trimmed.split("\\s+");
        }

        OpCode opCode = tokens.length > 0 ? OpCode.valueOfNullable(tokens[0].toUpperCase()) : null;
        if (opCode == null) {
            return new CompletionContext(CompletionMode.OPCODE, null, OperandMapping.NONE, 0);
        }

        String afterOpcode = trimmed.substring(tokens[0].length()).trim();
        int operandIndex = 0;
        if (!afterOpcode.isEmpty()) {
            operandIndex = afterOpcode.split(",", -1).length - 1;
            if (trimmed.endsWith(",") || linePrefix.endsWith(",")) {
                operandIndex++;
            }
        }
        return new CompletionContext(CompletionMode.OPERAND, opCode, opCode.operandMapping(), Math.max(0, operandIndex));
    }

    private List<Suggestion> suggestionsFor(CompletionContext context) {
        String prefix = currentPrefix().toLowerCase();
        List<Suggestion> suggestions = new ArrayList<>();
        Set<String> labels = extractLabels(editor.getText());

        switch (context.mode()) {
            case OPCODE -> {
                for (OpCode opCode : OpCode.values()) {
                    String text = opCode.name().toLowerCase();
                    if (prefix.isEmpty() || text.startsWith(prefix)) {
                        suggestions.add(new Suggestion(text, InstructionFormatter.buildSyntax(opCode), InstructionFormatter.describeOpcode(opCode)));
                    }
                }
            }
            case OPERAND -> suggestions.addAll(operandSuggestions(context, labels, prefix));
            case DIRECTIVE -> {
                for (String item : DIRECTIVE_ITEMS) {
                    addIfMatches(suggestions, item, item, directiveDescription(item), prefix);
                }
            }
            case GENERAL -> {
                for (String label : labels) {
                    addIfMatches(suggestions, label, "label", "Label from current source", prefix);
                }
            }
        }

        suggestions.sort((a, b) -> a.text().compareToIgnoreCase(b.text()));
        return suggestions;
    }

    private List<Suggestion> operandSuggestions(CompletionContext context, Set<String> labels, String prefix) {
        List<Suggestion> suggestions = new ArrayList<>();
        OperandRole role = roleFor(context.mapping(), context.operandIndex());
        if (role == OperandRole.REGISTER) {
            for (int i = 0; i < Architecture.GPR_COUNT; i++) {
                addIfMatches(suggestions, "r" + i, "register", "General purpose register", prefix);
            }
            for (String special : SPECIAL_REGISTERS) {
                addIfMatches(suggestions, special, "special register", specialRegisterDescription(special), prefix);
            }
        } else if (role == OperandRole.IMMEDIATE) {
            addIfMatches(suggestions, "imm32", "immediate", "32-bit immediate or label (see docs/ASSEMBLY_GUIDE.md for the assembler's actual accepted range)", prefix);
            for (String label : labels) {
                addIfMatches(suggestions, label, "label", "Label from current source", prefix);
            }
        }
        return suggestions;
    }

    private void addIfMatches(List<Suggestion> suggestions, String text, String usage, String description, String prefix) {
        if (prefix.isEmpty() || text.toLowerCase().startsWith(prefix)) {
            suggestions.add(new Suggestion(text, usage, description));
        }
    }

    private OperandRole roleFor(OperandMapping mapping, int operandIndex) {
        String syntax = InstructionFormatter.operandSyntax(mapping);
        if (syntax.isEmpty()) {
            return OperandRole.NONE;
        }
        String[] parts = syntax.split(", ");
        if (operandIndex < 0 || operandIndex >= parts.length) {
            return OperandRole.NONE;
        }
        return switch (parts[operandIndex]) {
            case "rd", "rs1", "rs2" -> OperandRole.REGISTER;
            case "imm32", "offset32" -> OperandRole.IMMEDIATE;
            default -> OperandRole.NONE;
        };
    }

    private String directiveDescription(String item) {
        return switch (item) {
            case ".word" -> "Emit a 4-byte word.";
            case ".byte" -> "Emit one byte.";
            case ".ascii" -> "Emit a string as bytes.";
            case ".org" -> "Move the output address.";
            case ".align" -> "Pad with zeros to the next multiple of N (a power of two). Instructions must be 8-byte aligned.";
            default -> "";
        };
    }

    private String specialRegisterDescription(String name) {
        return switch (name) {
            case "pc" -> "Program counter (not a usable instruction operand today)";
            case "sp" -> "Stack pointer (usable in mov, addi and subi only)";
            case "ir" -> "Instruction register (not a usable instruction operand today)";
            case "flags" -> "Flag register view (not a usable instruction operand today)";
            default -> "";
        };
    }

    private Set<String> extractLabels(String source) {
        Set<String> labels = new LinkedHashSet<>();
        if (source == null || source.isBlank()) {
            return labels;
        }
        Matcher matcher = LABEL_REFERENCE_PATTERN.matcher(source);
        while (matcher.find()) {
            labels.add(matcher.group(1));
        }
        return labels;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
