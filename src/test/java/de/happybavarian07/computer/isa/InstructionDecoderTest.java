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
        assertEquals(0, instruction.regDestIndex());
        assertEquals(0, instruction.regSourceIndex());
        assertEquals(0, instruction.immediateAddr());
    }

    @Test
    void testDecodeAddInstruction() {
        // OpCode.ADD = 0x04 (bits 31-26)
        // Rd = 5 (bits 25-21)
        // Rs = 10 (bits 20-16)
        // Immediate = 0x1234 (bits 15-0)
        int encoded = (0x10 << 26) | (5 << 21) | (10 << 16) | 0x1234;
        word.set(encoded);

        decoder.decode(word, instruction);

        assertEquals(OpCode.ADD, instruction.opCode());
        assertEquals(5, instruction.regDestIndex());
        assertEquals(10, instruction.regSourceIndex());
        assertEquals(0x1234, instruction.immediateAddr());
    }

    @Test
    void testDecodeAllOpcodes() {
        for (OpCode op : OpCode.values()) {
            int encoded = (op.binaryValue() << 26) | (31 << 21) | (15 << 16) | 0xFFFF;
            word.set(encoded);

            decoder.decode(word, instruction);

            assertEquals(op, instruction.opCode());
            assertEquals(31, instruction.regDestIndex());
            assertEquals(15, instruction.regSourceIndex());
            assertEquals(0xFFFF, instruction.immediateAddr());
        }
    }

    @Test
    void testInvalidOpcodeThrows() {
        // 0x1F (31) is unmapped in OpCode enum
        int invalidEncoded = (0x1F << 26);
        word.set(invalidEncoded);

        assertThrows(IllegalInstructionException.class, () -> decoder.decode(word, instruction));
    }
}
