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
        inA.set(Integer.MAX_VALUE);
        inB.set(1);
        alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(Integer.MIN_VALUE, outResult.getAsInt(), "Result is wrong.");
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
        inA.set(0x00FF0000);
        inB.set(0xF0000F0F);
        alu.execute(inA, inB, AluOp.OR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0xF0FF0F0F, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, true, false, false);
    }

    @Test
    void testXor() {
        inA.set(0xFFFFFFFF);
        inB.set(0xFFFFFFFF);
        alu.execute(inA, inB, AluOp.XOR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, false, false);
    }

    @Test
    void testNot() {
        inA.set(0);
        alu.execute(inA, inB, AluOp.NOT, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0xFFFFFFFF, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, true, false, false);
    }

    @Test
    void testShlBasic() {
        inA.set(1);
        alu.execute(inA, inB, AluOp.SHL, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(2, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShlCarry() {
        inA.set(0x80000000);
        alu.execute(inA, inB, AluOp.SHL, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, true, false);
    }

    @Test
    void testShrBasic() {
        inA.set(2);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(1, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShrMsb() {
        inA.set(0x80000000);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0x40000000, outResult.getAsInt(), "Result is wrong.");
        assertFlags(false, false, false, false);
    }

    @Test
    void testShrCarry() {
        inA.set(1);
        alu.execute(inA, inB, AluOp.SHR, outResult, flagZ, flagN, flagC, flagV);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFlags(true, false, true, false);
    }

    @Test
    void testRandomFuzzing() {
        Random random = new Random(42);
        for (AluOp op : AluOp.values()) {
            if (op.equals(AluOp.SHL) || op.equals(AluOp.SHR)) continue;
            for (int i = 0; i < 1000; i++) {
                inA.set(random.nextInt());
                inB.set(random.nextInt());
                alu.execute(inA, inB, op, outResult, flagZ, flagN, flagC, flagV);
                int expected = 0;
                switch (op) {
                    case ADD -> expected = inA.getAsInt() + inB.getAsInt();
                    case SUB -> expected = inA.getAsInt() - inB.getAsInt();
                    case AND -> expected = inA.getAsInt() & inB.getAsInt();
                    case OR -> expected = inA.getAsInt() | inB.getAsInt();
                    case XOR -> expected = inA.getAsInt() ^ inB.getAsInt();
                    case NOT -> expected = ~inA.getAsInt();
                }
                assertEquals(expected, outResult.getAsInt(), op + " is wrong for inA = " + inA.getAsInt() + ", inB = " + inB.getAsInt());
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