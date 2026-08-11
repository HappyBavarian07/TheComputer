package de.happybavarian07.computer.exceptions.assembler;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 13:03
 */
public class LexerException extends RuntimeException {
    private final String filePath;
    private final int line;
    private final int column;
    private final String snippet; // source line
    private final int snippetColumn; // start column


    public LexerException(String filePath, int line, int column, String snippet, int snippetColumn, String message) {
        this(filePath, line, column, snippet, snippetColumn, message, 4);
    }

    public LexerException(String filePath, int line, int column, String snippet, int snippetColumn, String message, int tabWidth) {
        super(String.format("%s:%d:%d: error: %s%n  %s%n  %s",
                filePath, line, column, message, snippet,
                computePointer(snippet, snippetColumn, column, tabWidth)));
        this.filePath = filePath;
        this.line = line;
        this.column = column;
        this.snippet = snippet;
        this.snippetColumn = snippetColumn;
    }

    private static String computePointer(String snippet, int snippetColumn, int errorColumn, int tabWidth) {
        int visual = 0;
        int charsToConsume = Math.max(0, errorColumn - snippetColumn);
        int consumed = 0;
        for (int i = 0; i < snippet.length() && consumed < charsToConsume; i++) {
            char c = snippet.charAt(i);
            if (c == '\t') {
                visual += tabWidth - (visual % tabWidth);
            } else {
                visual += 1;
            }
            consumed++;
        }
        return " ".repeat(visual) + "^";
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getSnippetColumn() {
        return snippetColumn;
    }
}
