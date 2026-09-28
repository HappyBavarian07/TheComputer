package de.happybavarian07.computer.cpu;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.profiler.ExecutionProfiler;
import de.happybavarian07.computer.exceptions.stack.StackOverflowException;
import de.happybavarian07.computer.isa.Condition;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.util.Architecture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.happybavarian07.computer.cpu.profiler.ExecutionProfiler;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 21:56
 */
class CpuTest {
    private Cpu cpu;
    private Word wordBuffer;
    private Address addressBuffer;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
        wordBuffer = new Word();
        addressBuffer = new Address();
    }

    private void writeInstruction(
            int byteAddress,
            Condition cond,
            OpCode opCode,
            int regDest,
            int regSource1,
            int regSource2,
            int immediate
    ) {
        addressBuffer.set(byteAddress);

        long rawInstruction =
                ((opCode.binaryValue().longValue() & 0xFFL) << 56)
                        | ((cond.binaryValue().longValue() & 0x0FL) << 52)
                        | (((long) regDest & 0x3FL) << 46)
                        | (((long) regSource1 & 0x3FL) << 40)
                        | (((long) regSource2 & 0x3FL) << 34)
                        | (immediate & 0xFFFFFFFFL);

        wordBuffer.set(rawInstruction);
        cpu.getSystemBus().writeWord(addressBuffer, wordBuffer);
    }

    private void writeInstruction(
            int byteAddress,
            OpCode opCode,
            int regDest,
            int regSource1,
            int regSource2,
            int immediate
    ) {
        writeInstruction(
                byteAddress,
                Condition.AL,
                opCode,
                regDest,
                regSource1,
                regSource2,
                immediate
        );
    }

    private void writeInstruction(
            int byteAddress,
            OpCode opCode,
            int regDest,
            int regSource1,
            int immediate
    ) {
        writeInstruction(
                byteAddress,
                Condition.AL,
                opCode,
                regDest,
                regSource1,
                0,
                immediate
        );
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
        writeInstruction(0x0010, OpCode.ADD, 1, 1, 2, 0);
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
        writeInstruction(0x0010, OpCode.ADD, 3, 1, 2, 0);
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
        writeInstruction(0x0010, OpCode.SUB, 3, 1, 2, 0);
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
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 10);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 20);

        writeInstruction(
                0x0010,
                Condition.EQ,
                OpCode.ADD,
                3,
                1,
                2,
                0
        );

        writeInstruction(0x0018, OpCode.HALT, 0, 0, 0);

        cpu.run();

        assertTrue(cpu.isHalted());

        cpu.getRegisterFile().read(3, wordBuffer);
        assertEquals(0, wordBuffer.getAsInt());
    }

    @Test
    void testStackOverflowTrap() {
        cpu.getSpecialRegisters()
                .getSP()
                .set(Architecture.STACK_LIMIT_ADDRESS + 2);

        writeInstruction(0x0000, OpCode.PUSH, 0, 1, 0);

        assertThrows(
                StackOverflowException.class,
                () -> cpu.step()
        );

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

    @Test
    void testCoreThroughputBenchmark() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       CPU THROUGHPUT BENCHMARK");
        System.out.println("========================================");

        BenchmarkResult tightLoop = runTightLoopBenchmark();
        printBenchmark("Tight Loop", tightLoop);

        BenchmarkResult mixedWorkload = runMixedWorkloadBenchmark();
        printBenchmark("Mixed ISA", mixedWorkload);

        runProfilerBenchmark();
    }

    private BenchmarkResult runTightLoopBenchmark() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 1);
        writeInstruction(0x0008, OpCode.ADDI, 2, 2, 1);
        writeInstruction(0x0010, OpCode.JMP, 0, 0, 0x0008);

        return benchmarkCpu(2_000);
    }

    private BenchmarkResult runMixedWorkloadBenchmark() {
        cpu.reset();

        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 10);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 3);
        writeInstruction(0x0010, OpCode.ADD, 3, 1, 2, 0);
        writeInstruction(0x0018, OpCode.SUB, 4, 3, 2, 0);
        writeInstruction(0x0020, OpCode.XOR, 5, 4, 1, 0);
        writeInstruction(0x0028, OpCode.OR, 6, 5, 2, 0);
        writeInstruction(0x0030, OpCode.AND, 7, 6, 1, 0);
        writeInstruction(0x0038, OpCode.ADDI, 3, 3, 1);
        writeInstruction(0x0040, OpCode.SUBI, 4, 4, 1);
        writeInstruction(0x0048, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0050, OpCode.JMP, 0, 0, 0x0000);

        return benchmarkCpu(2_000);
    }

    private BenchmarkResult benchmarkCpu(long measurementMillis) {
        cpu.getProfiler().reset();
        cpu.enableProfiler();

        long warmupEnd = System.nanoTime() + 2_000_000_000L;

        while (System.nanoTime() < warmupEnd) {
            cpu.step();
        }

        cpu.getProfiler().startMeasurement();

        long deadlineNs = System.nanoTime() + measurementMillis * 1_000_000L;

        while (System.nanoTime() < deadlineNs) {
            cpu.step();
        }

        cpu.getProfiler().stopMeasurement();
        cpu.disableProfiler();

        long instructions = cpu.getProfiler().getTotalInstructions();
        long elapsedNs = cpu.getProfiler().getTotalElapsedNs();
        double instructionsPerSecond = cpu.getProfiler().getInstructionsPerSecond();

        return new BenchmarkResult(
                instructions,
                elapsedNs,
                instructionsPerSecond
        );
    }

    private void runProfilerBenchmark() {
        cpu.reset();

        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 10);
        writeInstruction(0x0008, OpCode.MOVI, 2, 0, 3);
        writeInstruction(0x0010, OpCode.ADD, 3, 1, 2, 0);
        writeInstruction(0x0018, OpCode.SUB, 4, 3, 2, 0);
        writeInstruction(0x0020, OpCode.XOR, 5, 4, 1, 0);
        writeInstruction(0x0028, OpCode.OR, 6, 5, 2, 0);
        writeInstruction(0x0030, OpCode.AND, 7, 6, 1, 0);
        writeInstruction(0x0038, OpCode.ADDI, 3, 3, 1);
        writeInstruction(0x0040, OpCode.SUBI, 4, 4, 1);
        writeInstruction(0x0048, OpCode.NOP, 0, 0, 0);
        writeInstruction(0x0050, OpCode.JMP, 0, 0, 0x0000);

        ExecutionProfiler profiler = cpu.getProfiler();

        profiler.reset();
        cpu.enableProfiler();

        profiler.startMeasurement();

        long deadlineNs = System.nanoTime() + 1_000_000_000L;

        while (System.nanoTime() < deadlineNs) {
            cpu.step();
        }

        profiler.stopMeasurement();

        cpu.disableProfiler();

        System.out.println();
        profiler.printSummary();
    }

    private void printBenchmark(String name, BenchmarkResult result) {
        double mips = result.instructionsPerSecond / 1_000_000.0;

        System.out.printf(
                "[BENCHMARK] %-12s %,.0f instructions/sec (%s, %.3f MIPS)%n",
                name,
                result.instructionsPerSecond,
                ExecutionProfiler.humanReadableRate(result.instructionsPerSecond),
                mips
        );
    }

    private record BenchmarkResult(
            long instructions,
            long elapsedNs,
            double instructionsPerSecond
    ) {
    }

    @Test
    void testMoviNegativeSignExtendsToFull64Bits() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, -5);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);
        cpu.run();
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals(-5L, wordBuffer.getAsLong());
    }

    @Test
    void testAddiNegativeImmediateSubtracts() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 5);
        writeInstruction(0x0008, OpCode.ADDI, 2, 1, -1);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);
        cpu.run();
        cpu.getRegisterFile().read(2, wordBuffer);
        assertEquals(4L, wordBuffer.getAsLong());
    }

    @Test
    void testAddiKeepsTopBitImmediateSigned() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, 0);
        writeInstruction(0x0008, OpCode.ADDI, 2, 1, 0x80000000);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);
        cpu.run();
        cpu.getRegisterFile().read(2, wordBuffer);
        assertEquals(-2147483648L, wordBuffer.getAsLong());
    }

    @Test
    void testAndiZeroExtendsTopBitMask() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, -1);
        writeInstruction(0x0008, OpCode.ANDI, 2, 1, 0xFFFFFFFF);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);
        cpu.run();
        cpu.getRegisterFile().read(2, wordBuffer);
        assertEquals(0xFFFFFFFFL, wordBuffer.getAsLong());
    }

    @Test
    void testCmpiNegativeImmediateSetsFlags() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, -3);
        writeInstruction(0x0008, OpCode.CMPI, 1, 0, -3);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);
        cpu.run();
        assertTrue(cpu.getSpecialRegisters().getFlagZBit().getAsBool());
    }

    @Test
    void testMovReadsStackPointer() {
        writeInstruction(0x0000, OpCode.MOV, 1, Architecture.SP_REGISTER_INDEX, 0);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);
        cpu.run();
        cpu.getRegisterFile().read(1, wordBuffer);
        assertEquals((long) Architecture.STACK_BASE_ADDRESS, wordBuffer.getAsLong());
    }

    @Test
    void testAddiOnStackPointerAllocatesFrame() {
        writeInstruction(0x0000, OpCode.ADDI, Architecture.SP_REGISTER_INDEX, Architecture.SP_REGISTER_INDEX, -16);
        writeInstruction(0x0008, OpCode.HALT, 0, 0, 0);
        cpu.run();
        assertEquals(Architecture.STACK_BASE_ADDRESS - 16, cpu.getSpecialRegisters().getSP().getAsInt());
    }

    @Test
    void testMovWritesStackPointer() {
        writeInstruction(0x0000, OpCode.MOVI, 1, 0, Architecture.STACK_BASE_ADDRESS - 64);
        writeInstruction(0x0008, OpCode.MOV, Architecture.SP_REGISTER_INDEX, 1, 0);
        writeInstruction(0x0010, OpCode.HALT, 0, 0, 0);
        cpu.run();
        assertEquals(Architecture.STACK_BASE_ADDRESS - 64, cpu.getSpecialRegisters().getSP().getAsInt());
    }
}