package de.happybavarian07.computer.cpu.alu;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
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
                        expected = Long.divideUnsigned(inA.getAsLong(), inB.getAsLong());
                    }
                    case MOD -> {
                        if (inB.getAsLong() == 0) continue;
                        expected = Long.remainderUnsigned(inA.getAsLong(), inB.getAsLong());
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
}