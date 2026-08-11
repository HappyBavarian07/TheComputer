package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.InstructionWordEncoder;
import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.OperandKind;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InstructionWordEncoderTest {

    @Test
    void reg_out_of_range_throws() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        // instruction statement stub
        InstructionStatement stmt = new InstructionStatement("MOV", List.of(), new SourceSpan("file",1,1,1,3));

        Operand op = new Operand(OperandKind.REGISTER, "r99", null, new SourceSpan("file",1,1,1,3));
        ResolvedOperand ro = new ResolvedOperand(op, OperandKind.REGISTER, op.text(), 99);
        ResolvedStatement rs = new ResolvedStatement(stmt, 0, List.of(ro, ro));

        assertThrows(EncodingException.class, () -> encoder.encode(rs));
    }

    @Test
    void imm_out_of_range_throws() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        InstructionStatement stmt = new InstructionStatement("LOAD", List.of(), new SourceSpan("file",1,1,1,4));

        Operand opReg = new Operand(OperandKind.REGISTER, "r1", null, new SourceSpan("file",1,1,1,4));
        ResolvedOperand roReg = new ResolvedOperand(opReg, OperandKind.REGISTER, opReg.text(), 1);
        Operand opImm = new Operand(OperandKind.NUMBER, "70000", 70000, new SourceSpan("file",1,1,1,8));
        ResolvedOperand roImm = new ResolvedOperand(opImm, OperandKind.NUMBER, opImm.text(), 70000);

        ResolvedStatement rs = new ResolvedStatement(stmt, 0, List.of(roReg, roImm));

        assertThrows(EncodingException.class, () -> encoder.encode(rs));
    }
}
