package de.happybavarian07.computer.assembler.parser.model.statement;

import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InstructionStatement extends Statement {
    private final String opcode;
    private final String condition;
    private final List<Operand> operands;

    public InstructionStatement(String opcode, String condition, List<Operand> operands, SourceSpan span) {
        super(StatementKind.INSTRUCTION, span);
        this.opcode = Objects.requireNonNull(opcode, "opcode");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.operands = List.copyOf(Objects.requireNonNull(operands, "operands"));
    }

    public String opcode() {
        return opcode;
    }

    public String condition() { return condition; }

    public List<Operand> operands() {
        return operands;
    }

    public int operandCount() {
        return operands.size();
    }

    public Operand operandAt(int index) {
        return operands.get(index);
    }
}
