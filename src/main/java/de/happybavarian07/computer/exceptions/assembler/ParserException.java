package de.happybavarian07.computer.exceptions.assembler;

public class ParserException extends RuntimeException {
    private final String filePath;
    private final int line;
    private final int column;

    public ParserException(String filePath, int line, int column, String message) {
        super(String.format("%s:%d:%d: error: %s", filePath, line, column, message));
        this.filePath = filePath;
        this.line = line;
        this.column = column;
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
}
