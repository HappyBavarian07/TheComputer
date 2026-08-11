package de.happybavarian07.computer.assembler.parser.model.statement;

import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.StatementKind;

import java.util.Objects;

public class LabelStatement extends Statement {
    private final String name;

    public LabelStatement(String name, SourceSpan span) {
        super(StatementKind.LABEL, span);
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }
}
