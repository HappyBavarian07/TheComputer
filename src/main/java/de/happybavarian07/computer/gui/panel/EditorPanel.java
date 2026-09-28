package de.happybavarian07.computer.gui.panel;

import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.exceptions.assembler.LexerException;
import de.happybavarian07.computer.exceptions.assembler.ParserException;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import de.happybavarian07.computer.gui.assembly.AssemblyAutoComplete;
import de.happybavarian07.computer.gui.assembly.AssemblySyntaxHighlighter;
import de.happybavarian07.computer.gui.assembly.InstructionFormatter;
import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.isa.OpCode;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The assembly source editor: text pane, line-number gutter with a current
 * PC arrow, live syntax highlighting, Ctrl+Space completion, and inline
 * assembler diagnostics (parses/resolves/encodes on every edit so errors
 * show before you hit "Assemble & Load").
 */
public final class EditorPanel extends JPanel {
    private static final Pattern DIRECTIVE_LINE = Pattern.compile("^[\\s]*\\.[A-Za-z_][\\w.]*");

    private final JTextPane sourceEditor = new JTextPane();
    private final JTextArea gutter = new JTextArea();
    private final AssemblySyntaxHighlighter highlighter;
    private final AssemblyAutoComplete autoComplete;
    private final DiagnosticsPanel diagnosticsPanel = new DiagnosticsPanel();
    private final Map<Integer, SourceSpan> sourceSpanByAddress = new HashMap<>();

    private boolean diagnosticsPending;
    private boolean diagnosticsDirty;
    private Runnable onSourceChanged = () -> {
    };

    public EditorPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Assembly source"));

        sourceEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sourceEditor.setBackground(Theme.BG_DEEP);
        sourceEditor.setForeground(Theme.TEXT_BRIGHT);
        sourceEditor.setCaretColor(Theme.ACCENT);
        sourceEditor.setOpaque(true);
        sourceEditor.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        sourceEditor.setToolTipText("");
        ToolTipManager.sharedInstance().registerComponent(sourceEditor);

        highlighter = new AssemblySyntaxHighlighter(sourceEditor);
        autoComplete = new AssemblyAutoComplete(sourceEditor, () -> {
            highlighter.schedule();
            scheduleDiagnosticsRefresh();
        });

        sourceEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void changed() {
                highlighter.schedule();
                scheduleDiagnosticsRefresh();
                onSourceChanged.run();
            }
        });
        sourceEditor.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sourceEditor.setToolTipText(tooltipForPosition(e.getPoint()));
            }
        });

        gutter.setEditable(false);
        gutter.setFocusable(false);
        gutter.setBackground(Theme.BG_BASE);
        gutter.setForeground(Theme.TEXT_FAINT);
        gutter.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        gutter.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        gutter.setText(" ");
        gutter.setColumns(7);
        gutter.setRows(1);
        gutter.setOpaque(true);

        // A JTextPane placed directly as a JScrollPane's viewport view is a
        // Scrollable that tracks the viewport's width by default, i.e. it
        // wraps. That desyncs the gutter (which counts logical lines) from
        // what's actually on screen whenever a line is long enough to wrap.
        // Wrapping it in a plain (non-Scrollable) panel sidesteps that
        // negotiation entirely: the panel reports the editor's natural,
        // unconstrained preferred width, and the viewport scrolls
        // horizontally instead of forcing a wrap.
        JPanel noWrapHost = new JPanel(new BorderLayout());
        noWrapHost.add(sourceEditor, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(noWrapHost);
        scrollPane.getViewport().setBackground(Theme.BG_BASE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setRowHeaderView(gutter);

        add(scrollPane, BorderLayout.CENTER);
        add(diagnosticsPanel, BorderLayout.SOUTH);

        refreshDiagnostics();
    }

    public void setOnSourceChanged(Runnable listener) {
        this.onSourceChanged = listener;
    }

    public String getSource() {
        return sourceEditor.getText();
    }

    public void setSource(String text) {
        sourceEditor.setText(text);
    }

    public JTextPane getEditorComponent() {
        return sourceEditor;
    }

    /** Moves the gutter arrow to whatever source line maps to {@code pc}, if any. */
    public void highlightExecutionPointer(int pc) {
        SourceSpan span = sourceSpanByAddress.get(pc);
        gutter.setText(buildGutterText(span == null ? 0 : span.startLine()));
    }

    private String buildGutterText(int activeLine) {
        String source = sourceEditor.getText();
        if (source == null || source.isEmpty()) {
            return " ";
        }
        String[] lines = source.split("\\R", -1);
        int width = Integer.toString(lines.length).length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            builder.append(lineNumber == activeLine ? "→" : " ");
            builder.append(' ');
            builder.append(String.format("%" + width + "d", lineNumber));
            builder.append('\n');
        }
        return builder.toString();
    }

    private void scheduleDiagnosticsRefresh() {
        if (diagnosticsPending) {
            diagnosticsDirty = true;
            return;
        }
        diagnosticsPending = true;
        diagnosticsDirty = false;
        SwingUtilities.invokeLater(() -> {
            try {
                refreshDiagnostics();
            } finally {
                diagnosticsPending = false;
                if (diagnosticsDirty) {
                    scheduleDiagnosticsRefresh();
                }
            }
        });
    }

    private void refreshDiagnostics() {
        String source = sourceEditor.getText();
        if (source == null || source.isBlank()) {
            diagnosticsPanel.showIdle("No assembly source loaded.");
            highlighter.clearErrorSpan();
            return;
        }

        try {
            IndexedLexer lexer = new IndexedLexer();
            lexer.reset(source, "workbench.asm");
            List<Token> tokens = lexer.tokenizeAll();

            DefaultParser parser = new DefaultParser(new IndexedLexer());
            parser.reset(source, "workbench.asm");
            Program program = parser.parse();

            ResolvedProgram resolvedProgram = new SymbolResolver().resolve(program);
            EncodedProgram encodedProgram = new AssemblerEncoder().encodeProgram(resolvedProgram);

            sourceSpanByAddress.clear();
            for (ResolvedStatement statement : resolvedProgram.statements()) {
                SourceSpan span = statement.sourceStatement().span();
                if (span != null) {
                    sourceSpanByAddress.put(statement.address(), span);
                }
            }

            diagnosticsPanel.showReady(tokens.size(), program.statements().size(), resolvedProgram.statements().size(), encodedProgram.words().size(), "Source parses and encodes cleanly.");
            highlighter.clearErrorSpan();
        } catch (LexerException ex) {
            sourceSpanByAddress.clear();
            reportError("Lexer error", "Tokenization failed", ex.getMessage(), ex.getLine(), ex.getColumn(), null);
        } catch (ParserException ex) {
            sourceSpanByAddress.clear();
            reportError("Parser error", "Parsing failed", ex.getMessage(), ex.getLine(), ex.getColumn(), null);
        } catch (ResolutionException ex) {
            sourceSpanByAddress.clear();
            reportError("Resolver error", "Resolution failed", ex.getMessage(), ex.getLine(), ex.getColumn(), ex.getSpan());
        } catch (EncodingException ex) {
            sourceSpanByAddress.clear();
            reportError("Encoder error", "Encoding failed", ex.getMessage(), ex.getLine(), ex.getColumn(), ex.getSpan());
        } catch (RuntimeException ex) {
            sourceSpanByAddress.clear();
            reportError("Assembler error", "Diagnostics unavailable", ex.getMessage(), -1, -1, null);
        }
    }

    private void reportError(String stage, String summary, String message, int line, int column, SourceSpan span) {
        diagnosticsPanel.showError(stage, summary, message);
        String source = sourceEditor.getText();
        int start;
        int length;
        if (span != null) {
            start = offsetForLineColumn(source, span.startLine(), span.startColumn());
            int endOffset = offsetForLineColumn(source, span.endLine(), span.endColumn());
            length = Math.max(1, endOffset - start);
        } else if (line > 0 && column > 0) {
            start = offsetForLineColumn(source, line, column);
            length = Math.max(1, lineEndOffset(source, line) - start);
        } else {
            highlighter.clearErrorSpan();
            return;
        }
        highlighter.setErrorSpan(start, length);
    }

    private int offsetForLineColumn(String source, int line, int column) {
        if (source == null || source.isEmpty() || line <= 0 || column <= 0) {
            return -1;
        }
        int currentLine = 1;
        int offset = 0;
        while (currentLine < line && offset < source.length()) {
            int newline = source.indexOf('\n', offset);
            if (newline < 0) {
                return source.length();
            }
            offset = newline + 1;
            currentLine++;
        }
        return Math.min(source.length(), offset + Math.max(0, column - 1));
    }

    private int lineEndOffset(String source, int line) {
        if (source == null || source.isEmpty() || line <= 0) {
            return -1;
        }
        int currentLine = 1;
        int offset = 0;
        while (currentLine < line && offset < source.length()) {
            int newline = source.indexOf('\n', offset);
            if (newline < 0) {
                return source.length();
            }
            offset = newline + 1;
            currentLine++;
        }
        int newline = source.indexOf('\n', offset);
        return newline < 0 ? source.length() : newline;
    }

    private String tooltipForPosition(java.awt.Point point) {
        int offset = sourceEditor.viewToModel2D(point);
        if (offset < 0) {
            return null;
        }
        String source = sourceEditor.getText();
        if (source == null || source.isEmpty()) {
            return null;
        }
        int lineStart = source.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        int lineEnd = source.indexOf('\n', offset);
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        String line = source.substring(lineStart, lineEnd).trim();
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
            return null;
        }
        if (line.endsWith(":")) {
            return "Label";
        }
        if (DIRECTIVE_LINE.matcher(line).find()) {
            return "Directive";
        }
        String opToken = line.split("\\s+", 2)[0].replace(":", "");
        OpCode opCode = OpCode.valueOfNullable(opToken.toUpperCase());
        if (opCode != null) {
            return opCode.name().toLowerCase() + " - " + InstructionFormatter.buildSyntax(opCode) + " - " + InstructionFormatter.describeOpcode(opCode);
        }
        if (AssemblySyntaxHighlighter.looksLikeRegister(opToken)) {
            return "Register " + opToken.toLowerCase();
        }
        return null;
    }
}
