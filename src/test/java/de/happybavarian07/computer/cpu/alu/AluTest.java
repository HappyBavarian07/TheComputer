package de.happybavarian07.computer.cpu.alu;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.exceptions.core.arithmetic.ZeroDivisionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AluTest {
    private Alu alu;
    private Word inA;
    private Word inB;
    private Word outResult;
    private Bit flagZ, flagN, flagC, flagV;


    @BeforeEach
    void setUp() {
        alu = new Alu();
        inA = new Word(0);
        inB = new Word(0);
        outResult = new Word(0);
        flagZ = new Bit(false);
        flagN = new Bit(false);
        flagC = new Bit(false);
        flagV = new Bit(false);
    }

    @Test
    void testAddBasic() {
        inA.set(15);
        inB.set(35);
        alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(50, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testAddOverflow() {
        inA.set(Long.MAX_VALUE);
        inB.set(1);
        alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(Long.MIN_VALUE, outResult.getAsLong(), "Result is wrong.");
        assertFlags(false, true, false, true);
    }

    @Test
    void testSubBasic() {
        inA.set(25);
        inB.set(15);
        alu.execute(inA, inB, AluOp.SUB, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(10, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, true, false);
    }

    @Test
    void testSubZero() {
        inA.set(100);
        inB.set(100);
        alu.execute(inA, inB, AluOp.SUB, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, true, false);
    }

    @Test
    void testAnd() {
        inA.set(0x00FF00FF);
        inB.set(0x0F0F0F0F);
        alu.execute(inA, inB, AluOp.AND, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0x000F000F, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testOr() {
        inA.set(0x0000000000FF0000L);
        inB.set(0x00000000F0000F0FL);
        alu.execute(inA, inB, AluOp.OR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0xF0FF0F0FL, outResult.getAsLong(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testXor() {
        inA.set(0xFFFFFFFFL);
        inB.set(0xFFFFFFFFL);
        alu.execute(inA, inB, AluOp.XOR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, false, false);
    }

    @Test
    void testNot() {
        inA.set(0);
        alu.execute(inA, inB, AluOp.NOT, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(-1L, outResult.getAsLong(), "Result is wrong.");
        assertFlags(false, true, false, false);
    }

    @Test
    void testShlBasic() {
        inA.set(1);
        inB.set(1);
        alu.execute(inA, inB, AluOp.SHL, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(2, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShlCarry() {
        inA.set(0x8000000000000000L);
        inB.set(1);
        alu.execute(inA, inB, AluOp.SHL, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsLong(), "Result is wrong.");
        assertFlags(true, false, true, false);
    }

    @Test
    void testShrBasic() {
        inA.set(2);
        inB.set(1);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(1, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShrMsb() {
        inA.set(0x8000000000000000L);
        inB.set(1);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0x4000000000000000L, outResult.getAsLong(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShrCarry() {
        inA.set(1);
        inB.set(1);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, true, false);
    }

    @Test
    void testRandomFuzzing() {
        Random random = new Random(42);
        for (AluOp op : AluOp.values()) {
            if (op.equals(AluOp.SHL) || op.equals(AluOp.SHR) || op.equals(AluOp.NOP)) continue;
            for (int i = 0; i < 1000; i++) {
                inA.set(random.nextLong());
                inB.set(random.nextLong());
                alu.execute(inA, inB, op, outResult, flagZ, flagN, flagC, flagV);
                long expected = 0;
                switch (op) {
                    case ADD -> expected = inA.getAsLong() + inB.getAsLong();
                    case SUB -> expected = inA.getAsLong() - inB.getAsLong();
                    case MUL -> expected = inA.getAsLong() * inB.getAsLong();
                    case DIV -> {
                        if (inB.getAsLong() == 0) continue;
                        expected = inA.getAsLong() / inB.getAsLong(); // signed, truncates toward zero
                    }
                    case MOD -> {
                        if (inB.getAsLong() == 0) continue;
                        expected = inA.getAsLong() % inB.getAsLong(); // signed, sign of the dividend
                    }
                    case AND -> expected = inA.getAsLong() & inB.getAsLong();
                    case OR -> expected = inA.getAsLong() | inB.getAsLong();
                    case XOR -> expected = inA.getAsLong() ^ inB.getAsLong();
                    case NOT -> expected = ~inA.getAsLong();
                }
                assertEquals(expected, outResult.getAsLong(), op + " is wrong for inA = " + inA.getAsLong() + ", inB = " + inB.getAsLong());
            }
        }
    }

    private void assertFlags(boolean z, boolean n, boolean c, boolean v) {
        assertEquals(z, flagZ.getAsBool(), "Z-Flag is wrong.");
        assertEquals(n, flagN.getAsBool(), "N-Flag is wrong.");
        assertEquals(c, flagC.getAsBool(), "C-Flag is wrong.");
        assertEquals(v, flagV.getAsBool(), "V-Flag is wrong.");
    }

    @Test
    void testAluThroughputBenchmark() {
        inA.set(0x0123456789ABCDEFL);
        inB.set(0x1111111111111111L);

        // Warm up
        for (int i = 0; i < 50_000; i++) {
            alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        }

        long count = 1_000_000;
        long start = System.nanoTime();
        for (long i = 0; i < count; i++) {
            alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        }
        long elapsedNanos = System.nanoTime() - start;
        double opsPerSec = count / (elapsedNanos / 1_000_000_000.0);
        double megaOps = opsPerSec / 1_000_000.0;

        System.out.printf("[BENCHMARK] 64-Bit ALU Adder Throughput: %,.0f ops/sec (%.2f MOps/sec)%n", opsPerSec, megaOps);
        assertTrue(opsPerSec > 0);
    }

    private static final long MIN = Long.MIN_VALUE;

    private long divide(long a, long b, boolean[] flagsOut) {
        Alu alu = new Alu();
        Word out = new Word();
        Bit z = new Bit(false), n = new Bit(false), c = new Bit(true), v = new Bit(true);
        alu.div(new Word(a), new Word(b), out, z, n, c, v);
        flagsOut[0] = c.getAsBool();
        flagsOut[1] = v.getAsBool();
        return out.getAsLong();
    }

    private long modulo(long a, long b, boolean[] flagsOut) {
        Alu alu = new Alu();
        Word out = new Word();
        Bit z = new Bit(false), n = new Bit(false), c = new Bit(true), v = new Bit(true);
        alu.mod(new Word(a), new Word(b), out, z, n, c, v);
        flagsOut[0] = c.getAsBool();
        flagsOut[1] = v.getAsBool();
        return out.getAsLong();
    }

    @Test
    void testSignedDivisionTruncatesTowardZero() {
        boolean[] f = new boolean[2];
        assertEquals(-3L, divide(-7, 2, f));
        assertEquals(-3L, divide(7, -2, f));
        assertEquals(3L, divide(-7, -2, f));
        assertEquals(3L, divide(7, 2, f));
        assertEquals(0L, divide(0, 5, f));
    }

    @Test
    void testSignedModuloTakesSignOfDividend() {
        boolean[] f = new boolean[2];
        assertEquals(-1L, modulo(-7, 2, f));
        assertEquals(1L, modulo(7, -2, f));
        assertEquals(-1L, modulo(-7, -2, f));
        assertEquals(1L, modulo(7, 2, f));
    }

    @Test
    void testDivAndModClearCarryAndOverflow() {
        boolean[] f = new boolean[2];
        divide(7, 2, f);
        assertFalse(f[0]);
        assertFalse(f[1]);
        modulo(7, 2, f);
        assertFalse(f[0]);
        assertFalse(f[1]);
    }

    @Test
    void testMinDividedByMinusOneWrapsAndSetsOverflow() {
        boolean[] f = new boolean[2];
        assertEquals(MIN, divide(MIN, -1, f));
        assertTrue(f[1]);
        assertEquals(0L, modulo(MIN, -1, f));
        assertFalse(f[1]);
    }

    @Test
    void testDivisionByZeroThrows() {
        boolean[] f = new boolean[2];
        assertThrows(ZeroDivisionException.class, () -> divide(1, 0, f));
        assertThrows(ZeroDivisionException.class, () -> modulo(1, 0, f));
    }

    @Test
    void testMulOverflowFlagIsSignedOverflow() {
        Alu alu = new Alu();
        Word out = new Word();
        Bit z = new Bit(false), n = new Bit(false), c = new Bit(false), v = new Bit(false);
        alu.mul(new Word(-7L), new Word(2L), out, z, n, c, v);
        assertEquals(-14L, out.getAsLong());
        assertFalse(v.getAsBool());
        alu.mul(new Word(-3L), new Word(-3L), out, z, n, c, v);
        assertEquals(9L, out.getAsLong());
        assertFalse(v.getAsBool());
        alu.mul(new Word(Long.MAX_VALUE), new Word(2L), out, z, n, c, v);
        assertTrue(v.getAsBool());
        alu.mul(new Word(MIN), new Word(-1L), out, z, n, c, v);
        assertTrue(v.getAsBool());
        alu.mul(new Word(MIN), new Word(1L), out, z, n, c, v);
        assertFalse(v.getAsBool());
    }
}