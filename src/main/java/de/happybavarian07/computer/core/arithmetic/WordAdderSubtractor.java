package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.logic.LogicGates;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 17:47
 */
public class WordAdderSubtractor {
    private final int WORD_SIZE = Architecture.WORD_BITS; // 32 bits for now
    private final FullAdder[] adders;
    private final Bit[] carries;
    private final Bit[] effectiveB;
    private final Bit scratchSubBit;

    public WordAdderSubtractor() {
        adders = new FullAdder[WORD_SIZE];
        carries = new Bit[WORD_SIZE + 1];
        effectiveB = new Bit[WORD_SIZE];
        for (int i = 0; i < WORD_SIZE; i++) {
            adders[i] = new FullAdder();
            carries[i] = new Bit(false);
            effectiveB[i] = new Bit(false);
        }
        carries[WORD_SIZE] = new Bit(false);
        scratchSubBit = new Bit(false);
    }

    public void execute(Word inA, Word inB, boolean subtract, Word outResult, Bit outCarry, Bit outOverflow) {
        carries[0].set(subtract);
        scratchSubBit.set(subtract);

        // ripple carry from LSB (k=0, arrayIdx 31) to MSB (k=31, arrayIdx 0)
        for (int k = 0; k < WORD_SIZE; k++) {
            int bitIdx = WORD_SIZE - 1 - k; // k=0 is LSB (idx 31), k=31 is MSB (idx 0)

            // selective inversion: B_bit ^ subtract
            LogicGates.xor(inB.get(bitIdx), scratchSubBit, effectiveB[k]);

            // execute full adder k with AIn[k], effB[k], inCarry carries[k] and writes result at bitIdx and outCarry carries[k+1]
            adders[k].execute(inA.get(bitIdx), effectiveB[k], carries[k], outResult.get(bitIdx), carries[k + 1]);
        }

        outCarry.set(carries[WORD_SIZE].getAsBool());
        // signed overflow = inCarry to MSB (carries[31]) XOR outCarry from MSB (carries[32])
        LogicGates.xor(carries[WORD_SIZE - 1], carries[WORD_SIZE], outOverflow);
    }
}
