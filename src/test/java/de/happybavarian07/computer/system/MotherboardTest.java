package de.happybavarian07.computer.system;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.isa.OpCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 23:17
 */
class MotherboardTest {
    private Motherboard motherboard;
    private Address addressBuffer;
    private Word wordBuffer;

    @BeforeEach
    void setUp() {
        motherboard = new Motherboard();
        addressBuffer = new Address();
        wordBuffer = new Word();
        motherboard.powerOn();
    }

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource1, int regSource2, int immediate) {
        addressBuffer.set(byteAddress);
        long rawInstruction = ((opCode.binaryValue().longValue() & 0xFFL) << 56)
                | ((regDest & 0x3FL) << 46)
                | ((regSource1 & 0x3FL) << 40)
                | ((regSource2 & 0x3FL) << 34)
                | (immediate & 0xFFFFFFFFL);
        wordBuffer.set(rawInstruction);
        motherboard.getCpu().getSystemBus().writeWord(addressBuffer, wordBuffer);
    }

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource1, int immediate) {
        writeInstruction(byteAddress, opCode, regDest, regSource1, 0, immediate);
    }

    @Test
    void testMotherboardPowerOnAndRun() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 0x99);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);

        motherboard.runSystem();

        assertTrue(motherboard.getCpu().isHalted());
        motherboard.getCpu().getRegisterFile().read(1, wordBuffer);
        assertEquals(0x99, wordBuffer.getAsInt());
    }

    @Test
    void testMotherboardReset() {
        writeInstruction(0x0000, OpCode.HALT, 0, 0, 0);
        motherboard.stepSystem();
        assertTrue(motherboard.getCpu().isHalted());

        motherboard.reset();
        assertFalse(motherboard.getCpu().isHalted());
        assertEquals(0, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
    }
}
