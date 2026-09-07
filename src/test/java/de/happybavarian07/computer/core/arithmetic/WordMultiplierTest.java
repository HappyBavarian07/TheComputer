package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date September 04, 2026
 */
class WordMultiplierTest {
    private WordMultiplier wordMultiplier;
    private Word inA, inB, outLow;
    private Bit overflowFlag;

    @BeforeEach
    void setUp() {
        wordMultiplier = new WordMultiplier();
        inA = new Word();
        inB = new Word();
        outLow = new Word();
        overflowFlag = new Bit(false);
    }

    @Test
    void executeZeroTest() {
        // 0 * 12345 = 0
        inA.set(0);
        inB.set(12345);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(0, outLow.getAsInt());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeOneTest() {
        // 1 * 12345 = 12345
        inA.set(1);
        inB.set(12345);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(12345, outLow.getAsInt());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeSimpleTest() {
        // 10 * 5 = 50
        inA.set(10);
        inB.set(5);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(50, outLow.getAsInt());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeSquareTest() {
        // 123 * 123 = 15129
        inA.set(123);
        inB.set(123);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(15129, outLow.getAsInt());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeLargeValuesTest() {
        // 1,000,000 * 1,000,000 = 1,000,000,000,000
        inA.set(1_000_000);
        inB.set(1_000_000);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(1_000_000_000_000L, outLow.getAsLong());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeCommutativeTest() {
        // a * b = b * a
        inA.set(123456);
        inB.set(789);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        long resultAB = outLow.getAsLong();
        boolean overflowAB = overflowFlag.getAsBool();

        inA.set(789);
        inB.set(123456);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(resultAB, outLow.getAsLong());
        assertEquals(overflowAB, overflowFlag.getAsBool());
    }

    @Test
    void executeMaxSafeLongMultiplicationTest() {
        // 3,000,000,000 * 3 = 9,000,000,000
        inA.set(3_000_000_000L);
        inB.set(3);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(9_000_000_000L, outLow.getAsLong());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executeOverflowTest() {
        // 2^32 * 2^32 = 2^64
        // Low 64 bits = 0
        inA.set(4_294_967_296L);
        inB.set(4_294_967_296L);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(0, outLow.getAsLong());
        assertTrue(overflowFlag.getAsBool());
    }

    @Test
    void executeOverflowWithNonZeroLowPartTest() {
        // (2^32 + 1)^2 = 2^64 + 2^33 + 1
        // Low 64 bits = 2^33 + 1
        long value = 4_294_967_297L;

        inA.set(value);
        inB.set(value);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(8_589_934_593L, outLow.getAsLong());
        assertTrue(overflowFlag.getAsBool());
    }

    @Test
    void executeMultiplyByZeroTest() {
        // x * 0 = 0
        inA.set(987654321);
        inB.set(0);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(0, outLow.getAsLong());
        assertFalse(overflowFlag.getAsBool());
    }

    @Test
    void executePowerOfTwoTest() {
        // 2^20 * 2^20 = 2^40
        inA.set(1L << 20);
        inB.set(1L << 20);

        wordMultiplier.execute(inA, inB, outLow, overflowFlag);

        assertEquals(1L << 40, outLow.getAsLong());
        assertFalse(overflowFlag.getAsBool());
    }
}