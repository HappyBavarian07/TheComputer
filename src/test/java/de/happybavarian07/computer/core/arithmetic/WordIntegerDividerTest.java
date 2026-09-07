package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * @Author HappyBavarian07
 * @Date September 04, 2026 | 00:11
 */
class WordIntegerDividerTest {
    private WordIntegerDivider wordDivider;
    private Word inA, inB, outQuotient, outRemainder;

    @BeforeEach
    void setUp() {
        wordDivider = new WordIntegerDivider();
        inA = new Word();
        inB = new Word();
        outQuotient = new Word();
        outRemainder = new Word();
    }

    @Test
    void executeSimpleTest() {
        // 10 / 5 = 2
        inA.set(10);
        inB.set(5);
        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(2, outQuotient.getAsInt(), "Result Quotient didnt match expected");
    }

    @Test
    void executeRemainderTest() {
        // 15 / 6 = 2 remainder 3
        inA.set(15);
        inB.set(6);
        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(2, outQuotient.getAsInt(), "Result Quotient didnt match expected");
        assertEquals(3, outRemainder.getAsInt(), "Result Remainder didnt match expected");
    }

    @Test
    void executeExactDivisionTest() {
        // 100 / 10 = 10 remainder 0
        inA.set(100);
        inB.set(10);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(10, outQuotient.getAsInt());
        assertEquals(0, outRemainder.getAsInt());
    }

    @Test
    void executeOneAsDivisorTest() {
        // 37 / 1 = 37 remainder 0
        inA.set(37);
        inB.set(1);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(37, outQuotient.getAsInt());
        assertEquals(0, outRemainder.getAsInt());
    }

    @Test
    void executeSmallerDividendTest() {
        // 3 / 7 = 0 remainder 3
        inA.set(3);
        inB.set(7);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(0, outQuotient.getAsInt());
        assertEquals(3, outRemainder.getAsInt());
    }

    @Test
    void executeDividendEqualsDivisorTest() {
        // 12 / 12 = 1 remainder 0
        inA.set(12);
        inB.set(12);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(1, outQuotient.getAsInt());
        assertEquals(0, outRemainder.getAsInt());
    }

    @Test
    void executeRemainderOneTest() {
        // 10 / 3 = 3 remainder 1
        inA.set(10);
        inB.set(3);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(3, outQuotient.getAsInt());
        assertEquals(1, outRemainder.getAsInt());
    }

    @Test
    void executePrimeNumbersTest() {
        // 17 / 5 = 3 remainder 2
        inA.set(17);
        inB.set(5);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(3, outQuotient.getAsInt());
        assertEquals(2, outRemainder.getAsInt());
    }

    @Test
    void executePowerOfTwoTest() {
        // 128 / 8 = 16 remainder 0
        inA.set(128);
        inB.set(8);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(16, outQuotient.getAsInt());
        assertEquals(0, outRemainder.getAsInt());
    }

    @Test
    void executeLargeRemainderTest() {
        // 29 / 15 = 1 remainder 14
        inA.set(29);
        inB.set(15);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(1, outQuotient.getAsInt());
        assertEquals(14, outRemainder.getAsInt());
    }

    @Test
    void executeZeroDividendTest() {
        // 0 / 10 = 0 remainder 0
        inA.set(0);
        inB.set(10);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(0, outQuotient.getAsInt());
        assertEquals(0, outRemainder.getAsInt());
    }

    @Test
    void executeDividendOneLessThanDivisorTest() {
        // 15 / 16 = 0 remainder 15
        inA.set(15);
        inB.set(16);

        wordDivider.execute(inA, inB, outQuotient, outRemainder);

        assertEquals(0, outQuotient.getAsInt());
        assertEquals(15, outRemainder.getAsInt());
    }
}