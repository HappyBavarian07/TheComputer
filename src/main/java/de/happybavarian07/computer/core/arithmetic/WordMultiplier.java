package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 31, 2026 | 16:44
 */
public class WordMultiplier {

    private final Bit adderCarry;
    private final Bit adderOverflow;
    private final Word high;
    private final Word low;
    private final WordAdderSubtractor adder;
    private final Word signedHigh;

    public WordMultiplier() {
        this.high = new Word();
        this.low = new Word();
        this.adder = new WordAdderSubtractor();
        this.signedHigh = new Word();
        this.adderCarry = new Bit(false);
        this.adderOverflow = new Bit(false);
    }

    // inA = Multiplicand, inB = Multiplier. produces full 128-bit product split into high, low.
    public void execute(Word inA, Word inB, Word outProduct, Bit overflowFlag) {
        high.set(0);
        low.set(inB); // the multiplier rides in the low half and is consumed bit by bit

        for (int step = 0; step < Architecture.WORD_BITS; step++) {
            if (low.get(Architecture.WORD_BITS - 1).getAsBool()) {
                adder.execute(high, inA, false, high, adderCarry, adderOverflow);
            } else {
                adderCarry.set(false);
            }

            // shift the chain adderCarry : high : low right by one bit
            boolean highLsb = high.get(Architecture.WORD_BITS - 1).getAsBool(); // capture before high is shifted WORD_BITS - 1 is LSB while 0 is MSB
            shiftRightInto(low, highLsb);
            shiftRightInto(high, adderCarry.getAsBool());
        }

        outProduct.set(low);
        overflowFlag.set(!isZero(high)); // product exceeds 64 bits when any high bit is set aka overflow
    }

    // Two's complement multiply. The low 64 bits equal the unsigned product's low half, so the result word is the same;
    // only the overflow flag differs: it is set when the signed 128-bit product does not fit in 64 bits.
    // signed high = unsigned high - (a < 0 ? b : 0) - (b < 0 ? a : 0); it must be the sign extension of the low half.
    public void executeSigned(Word inA, Word inB, Word outProduct, Bit overflowFlag) {
        execute(inA, inB, outProduct, overflowFlag);
        signedHigh.set(high);
        if (inA.get(0).getAsBool()) {
            adder.execute(signedHigh, inB, true, signedHigh, adderCarry, adderOverflow);
        }
        if (inB.get(0).getAsBool()) {
            adder.execute(signedHigh, inA, true, signedHigh, adderCarry, adderOverflow);
        }

        boolean lowSign = outProduct.get(0).getAsBool();
        boolean overflow = false;
        for (int i = 0; i < Architecture.WORD_BITS; i++) {
            if (signedHigh.get(i).getAsBool() != lowSign) {
                overflow = true;
                break;
            }
        }
        overflowFlag.set(overflow);
    }

    // logical right shift by one; incomingMsb enters at bit index 0 (MSB)
    private void shiftRightInto(Word word, boolean incomingMsb) {
        for (int i = Architecture.WORD_BITS - 1; i > 0; i--) {
            word.set(i, word.get(i - 1).getAsBool());
        }
        word.set(0, incomingMsb);
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
