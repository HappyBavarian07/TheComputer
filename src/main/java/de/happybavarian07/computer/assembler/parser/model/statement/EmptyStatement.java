package de.happybavarian07.computer.assembler.parser.model.statement;

import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.StatementKind;

public class EmptyStatement extends Statement {
    public EmptyStatement(SourceSpan span) {
        super(StatementKind.EMPTY, span);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
