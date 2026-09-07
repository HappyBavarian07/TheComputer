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

    public WordIntegerDivider() {
        this.adder = new WordAdderSubtractor();
        this.trialSubtractOut = new Word();
        trialSubtractCarry = new Bit(false);
        trialSubtractOverflow = new Bit(false);
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
