package de.happybavarian07.computer.gui.assembly;

import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.isa.OpCode;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based syntax highlighter for a single {@link JTextPane}. Owns its
 * own re-entrancy guard so a highlight pass triggered mid-highlight (e.g.
 * from a document listener) is coalesced into one follow-up pass instead of
 * recursing.
 */
public final class AssemblySyntaxHighlighter {
    private static final Set<String> OPCODE_NAMES = Arrays.stream(OpCode.values())
            .map(opCode -> opCode.name().toLowerCase())
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));

    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(?:0x[0-9a-fA-F_]+|0b[01_]+|[0-9_]+)\\b");
    private static final Pattern REGISTER_PATTERN = Pattern.compile("\\b(?:r\\d+|pc|sp|ir|flags)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[\\s]*[A-Za-z_][\\w]*:");
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^[\\s]*\\.[A-Za-z_][\\w.]*");
    private static final Pattern OPCODE_PATTERN = Pattern.compile("\\b(" + String.join("|", OPCODE_NAMES) + ")\\b", Pattern.CASE_INSENSITIVE);

    private final JTextPane editor;
    private boolean highlighting;
    private boolean pending;
    private int errorStart = -1;
    private int errorLength = 0;

    public AssemblySyntaxHighlighter(JTextPane editor) {
        this.editor = editor;
    }

    public static boolean looksLikeDirective(String line) {
        return DIRECTIVE_PATTERN.matcher(line).find();
    }

    public static boolean looksLikeRegister(String token) {
        return REGISTER_PATTERN.matcher(token).matches();
    }

    public void setErrorSpan(int start, int length) {
        errorStart = start;
        errorLength = length;
        schedule();
    }

    public void clearErrorSpan() {
        errorStart = -1;
        errorLength = 0;
        schedule();
    }

    public void schedule() {
        if (highlighting) {
            pending = true;
            return;
        }
        SwingUtilities.invokeLater(this::highlight);
    }

    private void highlight() {
        if (highlighting) {
            return;
        }
        highlighting = true;
        try {
            StyledDocument doc = editor.getStyledDocument();
            SimpleAttributeSet base = new SimpleAttributeSet();
            StyleConstants.setForeground(base, Theme.TEXT_BRIGHT);
            StyleConstants.setBold(base, false);
            doc.setCharacterAttributes(0, doc.getLength(), base, true);

            String text = editor.getText();
            int offset = 0;
            for (String line : text.split("\n", -1)) {
                int lineLength = line.length();
                int commentIndex = findCommentIndex(line);
                int codeEnd = commentIndex >= 0 ? commentIndex : lineLength;

                if (commentIndex >= 0) {
                    applyStyle(doc, offset + commentIndex, lineLength - commentIndex, Theme.SYN_COMMENT, false);
                }

                String code = line.substring(0, codeEnd);
                applyPattern(doc, offset, code, LABEL_PATTERN, Theme.SYN_LABEL, true);
                applyPattern(doc, offset, code, DIRECTIVE_PATTERN, Theme.SYN_DIRECTIVE, true);
                applyPattern(doc, offset, code, OPCODE_PATTERN, Theme.SYN_OPCODE, true);
                applyPattern(doc, offset, code, REGISTER_PATTERN, Theme.SYN_REGISTER, true);
                applyPattern(doc, offset, code, NUMBER_PATTERN, Theme.SYN_NUMBER, false);
                applyPattern(doc, offset, code, STRING_PATTERN, Theme.SYN_STRING, false);

                offset += lineLength + 1;
            }
            if (errorStart >= 0 && errorLength > 0) {
                applyErrorStyle(doc, errorStart, errorLength);
            }
        } finally {
            highlighting = false;
            if (pending) {
                pending = false;
                schedule();
            }
        }
    }

    private void applyPattern(StyledDocument doc, int baseOffset, String text, Pattern pattern, java.awt.Color foreground, boolean bold) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            applyStyle(doc, baseOffset + matcher.start(), matcher.end() - matcher.start(), foreground, bold);
        }
    }

    private void applyStyle(StyledDocument doc, int start, int length, java.awt.Color foreground, boolean bold) {
        if (length <= 0) {
            return;
        }
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, foreground);
        StyleConstants.setBold(set, bold);
        doc.setCharacterAttributes(start, length, set, false);
    }

    private void applyErrorStyle(StyledDocument doc, int start, int length) {
        if (start < 0 || length <= 0) {
            return;
        }
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, Theme.ERROR_FG);
        StyleConstants.setBackground(set, Theme.ERROR_BG);
        StyleConstants.setBold(set, true);
        StyleConstants.setUnderline(set, true);
        doc.setCharacterAttributes(start, length, set, false);
    }

    public static int findCommentIndex(String line) {
        int semicolon = line.indexOf(';');
        int hash = line.indexOf('#');
        int slashSlash = line.indexOf("//");
        int index = -1;
        if (semicolon >= 0) {
            index = semicolon;
        }
        if (hash >= 0 && (index < 0 || hash < index)) {
            index = hash;
        }
        if (slashSlash >= 0 && (index < 0 || slashSlash < index)) {
            index = slashSlash;
        }
        return index;
    }
}
