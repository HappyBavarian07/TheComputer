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

    public WordMultiplier() {
        this.high = new Word();
        this.low = new Word();
        this.adder = new WordAdderSubtractor();
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
