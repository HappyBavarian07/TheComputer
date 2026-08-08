package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 18:13
 */
class WordAdderSubtractorTest {
    private WordAdderSubtractor adder;
    private Word inA;
    private Word inB;
    private Word outResult;
    private Bit outCarry;
    private Bit outOverflow;

    @BeforeEach
    void setUp() {
        adder = new WordAdderSubtractor();
        inA = new Word(0);
        inB = new Word(0);
        outResult = new Word(0);
        outCarry = new Bit(false);
        outOverflow = new Bit(false);
    }

    @Test
    void testBasicAddition() {
        inA.set(15);
        inB.set(10);
        adder.execute(inA, inB, false, outResult, outCarry, outOverflow);
        assertEquals(25, outResult.getAsInt(), "Result is wrong.");
        assertFalse(outCarry.getAsBool(), "Two's Complement Carry Bit is wrong.");
        assertFalse(outOverflow.getAsBool(), "Overflow Bit is wrong.");
    }

    @Test
    void testBasicSubtraction() {
        inA.set(25);
        inB.set(10);
        adder.execute(inA, inB, true, outResult, outCarry, outOverflow);
        assertEquals(15, outResult.getAsInt(), "Result is wrong.");
        assertFalse(outOverflow.getAsBool(), "Overflow Bit is wrong.");
    }

    @Test
    void testSelfSubtract() {
        inA.set(123456);
        inB.set(123456);
        adder.execute(inA, inB, true, outResult, outCarry, outOverflow);
        assertEquals(0, outResult.getAsInt(), "Result is wrong.");
        assertFalse(outOverflow.getAsBool(), "Overflow Bit is wrong.");
    }

    @Test
    void testSignedOverflowOnAddition() {
        inA.set(Integer.MIN_VALUE);
        inB.set(1);
        adder.execute(inA, inB, true, outResult, outCarry, outOverflow);
        assertEquals(Integer.MAX_VALUE, outResult.getAsInt(), "Result is wrong.");
        assertTrue(outOverflow.getAsBool(), "Overflow Bit is wrong.");
    }

    @Test
    void testRandomFuzzing() {
        Random random = new Random(42);

        for(int i = 0; i < 1000; i++) {
            inA.set(random.nextInt());
            inB.set(random.nextInt());
            adder.execute(inA, inB, false, outResult, outCarry, outOverflow);
            assertEquals(inA.getAsInt() + inB.getAsInt(), outResult.getAsInt(), "Result is wrong.");
        }
    }

    @Test
    void testRandomMultiAddition() {
        Random random = new Random(42);
        inA.set(random.nextInt(5000));
        int resultTest = inA.getAsInt();

        for(int i = 0; i < 10; i++) {
            inB.set(random.nextInt(5000));
            resultTest += inB.getAsInt();
            adder.execute(inA, inB, false, outResult, outCarry, outOverflow);
            assertEquals(resultTest, outResult.getAsInt(), "Result is wrong. (i = " + i + ", resultTest = " + resultTest + ")");
            inA.set(outResult.getAsArray());
            System.out.println("Result at i = " + i + ": Test = " + resultTest + ", Adder = " + inA.getAsInt() + ")");
        }
        System.out.println("Result final: Test = " + resultTest + ", Adder = " + inA.getAsInt() + ")");
    }
}
