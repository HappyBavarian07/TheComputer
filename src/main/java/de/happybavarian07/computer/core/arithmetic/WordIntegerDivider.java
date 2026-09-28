package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.exceptions.core.arithmetic.ZeroDivisionException;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date September 03, 2026 | 23:55
 */
public class WordIntegerDivider {
    private final WordAdderSubtractor adder;
    private final Word trialSubtractOut;
    private final Bit trialSubtractCarry;
    private final Bit trialSubtractOverflow;
    private final Word magnitudeA;
    private final Word magnitudeB;
    private final Word zero;

    public WordIntegerDivider() {
        this.adder = new WordAdderSubtractor();
        this.trialSubtractOut = new Word();
        trialSubtractCarry = new Bit(false);
        trialSubtractOverflow = new Bit(false);
        magnitudeA = new Word();
        magnitudeB = new Word();
        zero = new Word();
    }

    public void execute(Word inA, Word inB, Word outQuotient, Word outRemainder) {
        // catch zero division early
        if (isZero(inB)) throw new ZeroDivisionException("tried to divide with 0");
        // reset output
        outQuotient.set(0);
        outRemainder.set(0);

        // go over dividend bits from MSB to LSB
        for (int i = 0; i < Architecture.WORD_BITS; i++) {
            // shift remainder left by one, then pull in next bit of divisor
            shiftLeftInto(outRemainder);
            outRemainder.set(Architecture.WORD_BITS - 1, inA.get(i).getAsBool());
            // trialsubtract the divisor from the remainder
            adder.execute(outRemainder, inB, true, trialSubtractOut, trialSubtractCarry, trialSubtractOverflow);

            // check if it fit and adder reported no borrow
            // set quotient bit at i to true if yes
            if (trialSubtractCarry.getAsBool()) {
                outRemainder.set(trialSubtractOut);
                outQuotient.set(i, true);
            }
        }
    }

    // Two's complement division: quotient truncates toward zero, remainder takes the sign of the dividend.
    // overflowFlag is set only for MIN / -1, whose true quotient 2^63 does not fit (the quotient wraps to MIN).
    public void executeSigned(Word inA, Word inB, Word outQuotient, Word outRemainder, Bit overflowFlag) {
        if (isZero(inB)) throw new ZeroDivisionException("tried to divide with 0");

        boolean negativeA = inA.get(0).getAsBool();
        boolean negativeB = inB.get(0).getAsBool();
        magnitudeA.set(inA);
        magnitudeB.set(inB);
        if (negativeA) negate(magnitudeA);
        if (negativeB) negate(magnitudeB);

        // magnitudes are at most 2^63, so the unsigned divider is exact for them
        execute(magnitudeA, magnitudeB, outQuotient, outRemainder);

        if (negativeA != negativeB) negate(outQuotient);
        if (negativeA) negate(outRemainder);

        // the signs agree, so the quotient must be non-negative; a set sign bit means it wrapped (MIN / -1)
        overflowFlag.set(negativeA == negativeB && outQuotient.get(0).getAsBool());
    }

    private void negate(Word word) {
        zero.set(0);
        adder.execute(zero, word, true, word, trialSubtractCarry, trialSubtractOverflow);
    }

    // logical left shift by one
    private void shiftLeftInto(Word word) {
        for (int i = 0; i < Architecture.WORD_BITS - 1; i++) {
            word.set(i, word.get(i + 1).getAsBool());
        }
        word.set(Architecture.WORD_BITS - 1, false);
    }

    private boolean isZero(Word word) {
        for (int i = 0; i < Architecture.WORD_BITS; i++) {
            if (word.get(i).getAsBool()) {
                return false;
            }
        }
        return true;
    }
}
