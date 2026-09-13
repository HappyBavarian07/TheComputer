package de.happybavarian07.computer.bench;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.arithmetic.WordAdderSubtractor;
import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.system.Motherboard;
import de.happybavarian07.computer.util.Architecture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Headless throughput harness. Mirrors the GUI "Benchmark core" button so the
 * numbers reflect the CPU core alone (no Swing). Baseline for PERF-001.
 * Run: mvn -Dtest=CoreThroughputBench test
 */
public class CoreThroughputBench {

    private static long insn(long op, long rd, long rs1, long rs2, long imm) {
        return (op << 56) | (rd << 46) | (rs1 << 40) | (rs2 << 34) | (imm & 0xFFFFFFFFL);
    }

    // MOVI r1,0 ; ADDI r1,r1,1 ; JMP 8  -> tight ADDI+JMP loop (never halts, runs to cap)
    private static final long[] LOOP = {
            insn(0x02, 1, 0, 0, 0),   // MOVI r1, 0     @0
            insn(0x11, 1, 1, 0, 1),   // ADDI r1, r1, 1 @8
            insn(0x30, 0, 0, 0, 8),   // JMP 8          @16
    };

    private long runOnce(Motherboard mb, int cap) {
        mb.reset();
        for (int i = 0; i < LOOP.length; i++) {
            mb.getSystemBus().writeWord(new Address(i * Architecture.INSTRUCTION_BYTES), new Word(LOOP[i]));
        }
        mb.getCpu().getSpecialRegisters().getPC().set(0);
        mb.getCpu().getSpecialRegisters().getSP().set(Architecture.STACK_BASE_ADDRESS);
        long steps = 0;
        try {
            while (!mb.getCpu().isHalted() && steps < cap) {
                mb.stepSystem();
                steps++;
            }
        } catch (RuntimeException ex) {
            return steps;
        }
        return steps;
    }

    @Test
    public void cpuStepThroughput() {
        final int cap = 5_000_000;
        Motherboard mb = new Motherboard();
        for (int w = 0; w < 3; w++) runOnce(mb, cap);
        long steps = 0;
        long start = System.nanoTime();
        while (System.nanoTime() - start < 1_000_000_000L) {
            steps += runOnce(mb, cap);
        }
        long elapsed = System.nanoTime() - start;
        double hz = steps / (elapsed / 1e9);
        System.out.printf("%n[BENCH] CPU core: %,.0f steps/sec  (%.3f MIPS)  over %,d steps%n", hz, hz / 1e6, steps);
        assertTrue(steps > 0);
    }

    @Test
    public void adderThroughput() {
        WordAdderSubtractor adder = new WordAdderSubtractor();
        Word a = new Word(123456789L), b = new Word(987654321L), out = new Word();
        Bit carry = new Bit(false), over = new Bit(false);
        for (int i = 0; i < 100_000; i++) adder.execute(a, b, false, out, carry, over);
        long ops = 0;
        long start = System.nanoTime();
        while (System.nanoTime() - start < 1_000_000_000L) {
            for (int i = 0; i < 10_000; i++) adder.execute(a, b, false, out, carry, over);
            ops += 10_000;
        }
        long elapsed = System.nanoTime() - start;
        double opsSec = ops / (elapsed / 1e9);
        System.out.printf("%n[BENCH] 64-bit adder: %,.0f ops/sec  (%.2f MOps/sec)  over %,d ops%n", opsSec, opsSec / 1e6, ops);
        assertTrue(ops > 0);
    }
}
