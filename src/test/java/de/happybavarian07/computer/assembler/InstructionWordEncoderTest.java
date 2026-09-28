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
        InstructionStatement stmt = new InstructionStatement("MOV", "AL", List.of(), new SourceSpan("file",1,1,1,3));

        Operand op = new Operand(OperandKind.REGISTER, "r99", null, new SourceSpan("file",1,1,1,3));
        ResolvedOperand ro = new ResolvedOperand(op, OperandKind.REGISTER, op.text(), 99L);
        ResolvedStatement rs = new ResolvedStatement(stmt, 0, List.of(ro, ro));

        assertThrows(EncodingException.class, () -> encoder.encode(rs));
    }

    @Test
    void imm_out_of_range_throws() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        // 2^31 does not fit the SIGNED32 immediate of addi
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("ADDI", 3, 1, 2147483648L)));
    }

    @Test
    void signed_immediate_accepts_full_range_and_encodes_low_32_bits() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        assertEquals(0xFFFFFFFFL, encoder.encode(instruction("ADDI", 3, 1, -1)).rawWord() & 0xFFFFFFFFL);
        assertEquals(0x80000000L, encoder.encode(instruction("ADDI", 3, 1, -2147483648L)).rawWord() & 0xFFFFFFFFL);
        assertEquals(0x7FFFFFFFL, encoder.encode(instruction("ADDI", 3, 1, 2147483647L)).rawWord() & 0xFFFFFFFFL);
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("ADDI", 3, 1, -2147483649L)));
    }

    @Test
    void unsigned_immediate_accepts_zero_to_2_pow_32_minus_1_only() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        assertEquals(0xFFFFFFFFL, encoder.encode(instruction("ANDI", 3, 1, 4294967295L)).rawWord() & 0xFFFFFFFFL);
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("ANDI", 3, 1, -1)));
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("ANDI", 3, 1, 4294967296L)));
    }

    @Test
    void shift_immediate_is_limited_to_0_to_63() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        assertEquals(63L, encoder.encode(instruction("SHLI", 3, 1, 63)).rawWord() & 0xFFFFFFFFL);
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("SHLI", 3, 1, 64)));
        assertThrows(EncodingException.class, () -> encoder.encode(instruction("SHRI", 3, 1, -1)));
    }

    @Test
    void movi_rejects_values_that_do_not_fit_signed_32() {
        InstructionWordEncoder encoder = new InstructionWordEncoder();
        assertThrows(EncodingException.class, () -> encoder.encode(instructionRdImm("MOVI", 1, 0xFFFFFFFFL)));
        assertEquals(0xFFFFFFFBL, encoder.encode(instructionRdImm("MOVI", 1, -5)).rawWord() & 0xFFFFFFFFL);
    }

    private static ResolvedStatement instruction(String mnemonic, long rd, long rs1, long imm) {
        InstructionStatement stmt = new InstructionStatement(mnemonic, "AL", List.of(), new SourceSpan("file", 1, 1, 1, 4));
        SourceSpan span = new SourceSpan("file", 1, 1, 1, 4);
        Operand opRd = new Operand(OperandKind.REGISTER, "r" + rd, null, span);
        Operand opRs1 = new Operand(OperandKind.REGISTER, "r" + rs1, null, span);
        Operand opImm = new Operand(OperandKind.NUMBER, Long.toString(imm), imm, span);
        return new ResolvedStatement(stmt, 0, List.of(
                new ResolvedOperand(opRd, OperandKind.REGISTER, opRd.text(), rd),
                new ResolvedOperand(opRs1, OperandKind.REGISTER, opRs1.text(), rs1),
                new ResolvedOperand(opImm, OperandKind.NUMBER, opImm.text(), imm)));
    }

    private static ResolvedStatement instructionRdImm(String mnemonic, long rd, long imm) {
        InstructionStatement stmt = new InstructionStatement(mnemonic, "AL", List.of(), new SourceSpan("file", 1, 1, 1, 4));
        SourceSpan span = new SourceSpan("file", 1, 1, 1, 4);
        Operand opRd = new Operand(OperandKind.REGISTER, "r" + rd, null, span);
        Operand opImm = new Operand(OperandKind.NUMBER, Long.toString(imm), imm, span);
        return new ResolvedStatement(stmt, 0, List.of(
                new ResolvedOperand(opRd, OperandKind.REGISTER, opRd.text(), rd),
                new ResolvedOperand(opImm, OperandKind.NUMBER, opImm.text(), imm)));
    }
}
