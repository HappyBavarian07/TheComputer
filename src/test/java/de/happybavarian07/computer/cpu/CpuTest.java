package de.happybavarian07.computer.cpu;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.memory.ram.RamBusDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 21:56
 */
class CpuTest {
    private Cpu cpu;
    private RamBusDevice ramBusDevice;
    private Word wordBuffer;
    private Address addressBuffer;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
        wordBuffer = new Word();
        addressBuffer = new Address();
    }

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource, int immediate) {
        addressBuffer.set(byteAddress);
        int rawInstruction = (opCode.binaryValue() << 26) | ((regDest & 0x1F) << 21) | ((regSource & 0x1F) << 16) | (immediate & 0xFFFF);
        wordBuffer.set(rawInstruction);
        cpu.getSystemBus().write(addressBuffer, wordBuffer);
    }

    @Test
    void testSingleStepMovAndHalt() {
        writeInstruction(0x0000, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0004, OpCode.HALT, 0, 0, 0);

        assertEquals(0, cpu.getSpecialRegisters().getPC().getAsInt());
        assertFalse(cpu.isHalted());

        cpu.step();
        assertEquals(4, cpu.getSpecialRegisters().getPC().getAsInt());
        assertFalse(cpu.isHalted());

        cpu.step();
        assertTrue(cpu.isHalted());
    }

    @Test
    void testRunProgram() {
        addressBuffer.set(0x0100);
        wordBuffer.set(0x42);
        cpu.getSystemBus().write(addressBuffer, wordBuffer);

        writeInstruction(0x0000, OpCode.LOAD, 1, 0, 0x0100);
        writeInstruction(0x0004, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals(0x42, wordBuffer.getAsInt());
    }

    @Test
    void testAddOperation() {
        addressBuffer.set(0x0100);
        wordBuffer.set(10);
        cpu.getSystemBus().write(addressBuffer, wordBuffer);

        addressBuffer.set(0x0104);
        wordBuffer.set(20);
        cpu.getSystemBus().write(addressBuffer, wordBuffer);

        writeInstruction(0x0000, OpCode.LOAD, 1, 0, 0x0100);
        writeInstruction(0x0004, OpCode.LOAD, 2, 0, 0x0104);
        writeInstruction(0x0008, OpCode.ADD, 1, 2, 0);
        writeInstruction(0x000C, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals(30, wordBuffer.getAsInt());
    }

    @Test
    void testJumpZeroLoop() {
        writeInstruction(0x0000, OpCode.JMP, 0, 0, 0x0008);
        writeInstruction(0x0004, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        assertEquals(0x0008, cpu.getSpecialRegisters().getPC().getAsInt());
    }

    @Test
    void testReset() {
        writeInstruction(0x0000, OpCode.HALT, 0, 0, 0);
        cpu.step();
        assertTrue(cpu.isHalted());

        cpu.reset();
        assertFalse(cpu.isHalted());
        assertEquals(0, cpu.getSpecialRegisters().getPC().getAsInt());
    }
}
