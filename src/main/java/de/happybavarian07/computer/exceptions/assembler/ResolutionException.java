package de.happybavarian07.computer.exceptions.assembler;

import de.happybavarian07.computer.assembler.parser.model.SourceSpan;

import java.util.Objects;

public class ResolutionException extends RuntimeException {
    private final String filePath;
    private final int line;
    private final int column;
    private final SourceSpan span;

    public ResolutionException(String filePath, int line, int column, String message) {
        this(filePath, line, column, null, message);
    }

    public ResolutionException(SourceSpan span, String message) {
        this(span.filePath(), span.startLine(), span.startColumn(), span, message);
    }

    public ResolutionException(String filePath, int line, int column, SourceSpan span, String message) {
        super(formatMessage(filePath, line, column, message));
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.line = line;
        this.column = column;
        this.span = span;
    }

    private static String formatMessage(String filePath, int line, int column, String message) {
        String detail = message == null || message.isBlank() ? "resolution failed" : message;
        return String.format("%s:%d:%d: error: resolution failure: %s", filePath, line, column, detail);
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

    public SourceSpan getSpan() {
        return span;
    }
}