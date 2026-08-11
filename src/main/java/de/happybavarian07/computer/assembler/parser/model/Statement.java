package de.happybavarian07.computer.assembler.parser.model;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 14:47
 */
public abstract class Statement {
    private final StatementKind kind;
    private final SourceSpan span;

    protected Statement(StatementKind kind, SourceSpan span) {
        this.kind = kind;
        this.span = span;
    }

    public StatementKind kind() {
        return kind;
    }

    public SourceSpan span() {
        return span;
    }

    public boolean isEmpty() { return false; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Statement{");
        sb.append("kind=").append(kind);
        sb.append(", span=").append(span);
        sb.append('}');
        return sb.toString();
    }
}
