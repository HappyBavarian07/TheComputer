package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.exceptions.isa.IllegalInstructionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 16:36
 */
class InstructionDecoderTest {
    private InstructionDecoder decoder;
    private Word word;
    private Instruction instruction;

    @BeforeEach
    void setUp() {
        decoder = new InstructionDecoder();
        word = new Word(0);
        instruction = new Instruction();
    }

    @Test
    void testDecodeNop() {
        word.set(0);
        decoder.decode(word, instruction);

        assertEquals(OpCode.NOP, instruction.opCode());
        assertEquals(Condition.AL, instruction.condition());
        assertEquals(0, instruction.regDestIndex());
        assertEquals(0, instruction.regSource1Index());
        assertEquals(0, instruction.regSource2Index());
        assertEquals(0, instruction.immediateAddr());
    }

    @Test
    void testDecodeAddInstruction() {
        // OpCode.ADD = 0x10 (bits 63-56)
        // Condition.EQ = 0x01 (bits 55-52)
        // Rd = 5 (bits 51-46)
        // Rs1 = 10 (bits 45-40)
        // Rs2 = 20 (bits 39-34)
        // Reserved = 0 (bits 33-32)
        // Immediate = 0x1234 (bits 31-0)
        long encoded = (0x10L << 56) | (0x01L << 52) | (5L << 46) | (10L << 40) | (20L << 34) | 0x1234L;
        word.set(encoded);

        decoder.decode(word, instruction);

        assertEquals(OpCode.ADD, instruction.opCode());
        assertEquals(Condition.EQ, instruction.condition());
        assertEquals(5, instruction.regDestIndex());
        assertEquals(10, instruction.regSource1Index());
        assertEquals(20, instruction.regSource2Index());
        assertEquals(0x1234, instruction.immediateAddr());
    }

    @Test
    void testDecodeAllOpcodes() {
        for (OpCode op : OpCode.values()) {
            long encoded = (op.binaryValue().longValue() << 56) | (0x0L << 52) | (31L << 46) | (15L << 40) | (10L << 34) | 0xFFFFL;
            word.set(encoded);

            decoder.decode(word, instruction);

            assertEquals(op, instruction.opCode());
            assertEquals(Condition.AL, instruction.condition());
            assertEquals(31, instruction.regDestIndex());
            assertEquals(15, instruction.regSource1Index());
            assertEquals(10, instruction.regSource2Index());
            assertEquals(0xFFFF, instruction.immediateAddr());
        }
    }

    @Test
    void testInvalidOpcodeThrows() {
        // 0xEE is unmapped in OpCode enum
        long invalidEncoded = (0xEEL << 56);
        word.set(invalidEncoded);

        assertThrows(IllegalInstructionException.class, () -> decoder.decode(word, instruction));
    }
}
