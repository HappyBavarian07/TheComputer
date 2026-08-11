package de.happybavarian07.computer.assembler.parser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Program {
    private final String sourcePath;
    private final List<Statement> statements;
    private SourceSpan span;

    public Program(String sourcePath) {
        this.sourcePath = sourcePath;
        this.statements = new ArrayList<>();
    }

    public void addStatement(Statement statement) {
        statements.add(Objects.requireNonNull(statement, "statement"));
    }

    public List<Statement> statements() {
        return Collections.unmodifiableList(statements);
    }

    public String sourcePath() {
        return sourcePath;
    }

    public SourceSpan span() {
        return span;
    }

    public void setSpan(SourceSpan span) {
        this.span = span;
    }
}
