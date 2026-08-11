package de.happybavarian07.computer.assembler.parser.model.statement;

import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DirectiveStatement extends Statement {
    private final String name;
    private final List<Operand> arguments;

    public DirectiveStatement(String name, List<Operand> arguments, SourceSpan span) {
        super(StatementKind.DIRECTIVE, span);
        this.name = Objects.requireNonNull(name, "name");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public String name() {
        return name;
    }

    public List<Operand> arguments() {
        return arguments;
    }
}
