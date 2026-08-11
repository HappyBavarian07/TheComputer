package de.happybavarian07.computer.assembler.parser.model;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 14:50
 */
public record SourceSpan(String filePath,
                         int startLine, int startColumn,
                         int endLine, int endColumn) {
    public boolean contains(int line, int column) {
        if (line < startLine || line > endLine) return false;
        return column >= startColumn && column <= endColumn;
    }
}
