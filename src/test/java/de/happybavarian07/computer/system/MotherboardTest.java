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

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource, int immediate) {
        addressBuffer.set(byteAddress);
        int rawInstruction = (opCode.binaryValue() << 26) | ((regDest & 0x1F) << 21) | ((regSource & 0x1F) << 16) | (immediate & 0xFFFF);
        wordBuffer.set(rawInstruction);
        motherboard.getCpu().getSystemBus().write(addressBuffer, wordBuffer);
    }

    @Test
    void testMotherboardPowerOnAndRun() {
        addressBuffer.set(0x0100);
        wordBuffer.set(0x99);
        motherboard.getCpu().getSystemBus().write(addressBuffer, wordBuffer);

        writeInstruction(0x0000, OpCode.LOAD, 1, 0, 0x0100);
        writeInstruction(0x0004, OpCode.HALT, 0, 0, 0);

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
