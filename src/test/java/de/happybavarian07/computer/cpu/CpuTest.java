package de.happybavarian07.computer.cpu;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.isa.Condition;
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

    private void writeInstruction(int byteAddress, Condition cond, OpCode opCode, int regDest, int regSource1, int regSource2, int immediate) {
        addressBuffer.set(byteAddress);
        long rawInstruction = ((opCode.binaryValue().longValue() & 0xFFL) << 56)
                | ((cond.binaryValue().longValue() & 0x0FL) << 52)
                | ((regDest & 0x3FL) << 46)
                | ((regSource1 & 0x3FL) << 40)
                | ((regSource2 & 0x3FL) << 34)
                | (immediate & 0xFFFFFFFFL);
        wordBuffer.set(rawInstruction);
        cpu.getSystemBus().writeWord(addressBuffer, wordBuffer);
    }

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource1, int regSource2, int immediate) {
        writeInstruction(byteAddress, Condition.AL, opCode, regDest, regSource1, regSource2, immediate);
    }

    private void writeInstruction(int byteAddress, OpCode opCode, int regDest, int regSource1, int immediate) {
        writeInstruction(byteAddress, Condition.AL, opCode, regDest, regSource1, 0, immediate);
    }

    @Test
    void testSingleStepMovAndHalt() {
        writeInstruction(0x0000, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);

        assertEquals(0, cpu.getSpecialRegisters().getPC().getAsInt());
        assertFalse(cpu.isHalted());

        cpu.step();
        assertEquals(8, cpu.getSpecialRegisters().getPC().getAsInt());
        assertFalse(cpu.isHalted());

        cpu.step();
        assertTrue(cpu.isHalted());
    }

    @Test
    void testRunProgram() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 0x42);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals(0x42, wordBuffer.getAsInt());
    }

    @Test
    void testImmediateLoadAndStoreProgram() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 42);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 8);
        writeInstruction(0x0010, OpCode.ADD, 1, 1, 2, 0); // r1 = r1 + r2
        writeInstruction(0x0018, OpCode.STOREW, 2, 0, 10);
        writeInstruction(0x0020, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals(50, wordBuffer.getAsInt());

        addressBuffer.set(10);
        wordBuffer.set(0);
        cpu.getSystemBus().readWord(addressBuffer, wordBuffer);
        assertEquals(8, wordBuffer.getAsInt());
    }

    @Test
    void testAddOperation() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 10);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 20);
        writeInstruction(0x0010, OpCode.ADD, 3, 1, 2, 0); // r3 = r1 + r2
        writeInstruction(0x0018, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(3, wordBuffer);
        assertEquals(30, wordBuffer.getAsInt());
    }

    @Test
    void testSubtractOperation() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 5);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 1);
        writeInstruction(0x0010, OpCode.SUB, 3, 1, 2, 0); // r3 = r1 - r2
        writeInstruction(0x0018, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(3, wordBuffer);
        assertEquals(4, wordBuffer.getAsInt());
    }

    @Test
    void testJumpZeroLoop() {
        writeInstruction(0x0000, OpCode.JMP, 0, 0, 0x0010);
        writeInstruction(0x0008, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        assertEquals(0x0010, cpu.getSpecialRegisters().getPC().getAsInt());
    }

    @Test
    void testPushAndPop() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 0xABCD);
        writeInstruction(0x0008, OpCode.PUSH, 0, 1, 0);
        writeInstruction(0x0010, OpCode.POP, 2, 0, 0);
        writeInstruction(0x0018, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(2, wordBuffer);
        assertEquals(0xABCD, wordBuffer.getAsInt());
    }

    @Test
    void testConditionalExecutionSkippedWhenFalse() {
        // r1 = 10, r2 = 20
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 10);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 20);
        // ADDEQ r3, r1, r2 (Z is false, so this should be skipped!)
        writeInstruction(0x0010, Condition.EQ, OpCode.ADD, 3, 1, 2, 0);
        writeInstruction(0x0018, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());
        cpu.getRegisterFile().read(3, wordBuffer);
        assertEquals(0, wordBuffer.getAsInt()); // r3 remains 0 because ADDEQ was skipped
    }

    @Test
    void testStackOverflowTrap() {
        cpu.getSpecialRegisters().getSP().set(de.happybavarian07.computer.util.Architecture.STACK_LIMIT_ADDRESS + 2);
        writeInstruction(0x0000, OpCode.PUSH, 0, 1, 0);

        assertThrows(de.happybavarian07.computer.exceptions.stack.StackOverflowException.class, () -> cpu.step());
        assertTrue(cpu.isHalted());
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
