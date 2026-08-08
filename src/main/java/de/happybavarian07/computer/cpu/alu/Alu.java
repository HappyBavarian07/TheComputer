package de.happybavarian07.computer.cpu.alu;

import de.happybavarian07.computer.core.arithmetic.WordAdderSubtractor;
import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.logic.LogicGates;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 19:20
 */
public class Alu {
    private final int WORD_SIZE = Architecture.WORD_BITS;
    private final WordAdderSubtractor wordAdderSubtractor;
    private final Bit scratchAdderCarry;
    private final Bit scratchAdderOverflow;

    public Alu() {
        wordAdderSubtractor = new WordAdderSubtractor();
        scratchAdderCarry = new Bit(false);
        scratchAdderOverflow = new Bit(false);
    }

    // OpCode (for now): ADD, SUB, AND, OR, XOR, NOT, SHL, SHR
    public void execute(Word inA, Word inB, AluOp aluOp, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        switch (aluOp) {
            case ADD -> add(inA, inB, outResult, flagZ, flagN, flagC, flagV);
            case SUB -> sub(inA, inB, outResult, flagZ, flagN, flagC, flagV);
            case AND -> and(inA, inB, outResult, flagZ, flagN, flagC, flagV);
            case OR -> or(inA, inB, outResult, flagZ, flagN, flagC, flagV);
            case XOR -> xor(inA, inB, outResult, flagZ, flagN, flagC, flagV);
            case NOT -> not(inA, outResult, flagZ, flagN, flagC, flagV);
            case SHL -> shl(inA, outResult, flagZ, flagN, flagC, flagV);
            case SHR -> shr(inA, outResult, flagZ, flagN, flagC, flagV);
        }
    }

    public void add(Word inA, Word inB, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        wordAdderSubtractor.execute(inA, inB, false, outResult, flagC, flagV);
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
    }

    public void sub(Word inA, Word inB, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        wordAdderSubtractor.execute(inA, inB, true, outResult, flagC, flagV);
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
    }

    public void and(Word inA, Word inB, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        for (int i = 0; i < WORD_SIZE; i++) {
            LogicGates.and(inA.get(i), inB.get(i), outResult.get(i));
        }
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagC.set(false);
        flagV.set(false);
    }

    public void or(Word inA, Word inB, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        for (int i = 0; i < WORD_SIZE; i++) {
            LogicGates.or(inA.get(i), inB.get(i), outResult.get(i));
        }
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagC.set(false);
        flagV.set(false);
    }

    public void xor(Word inA, Word inB, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        for (int i = 0; i < WORD_SIZE; i++) {
            LogicGates.xor(inA.get(i), inB.get(i), outResult.get(i));
        }
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagC.set(false);
        flagV.set(false);
    }

    public void not(Word inA, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        for (int i = 0; i < WORD_SIZE; i++) {
            LogicGates.not(inA.get(i), outResult.get(i));
        }
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagC.set(false);
        flagV.set(false);
    }

    public void shl(Word inA, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        flagC.set(inA.get(0).getAsBool());

        for (int i = 1; i < WORD_SIZE; i++) {
            outResult.set(i - 1, inA.get(i));
        }

        outResult.set(WORD_SIZE - 1, false);
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagV.set(false);
    }

    public void shr(Word inA, Word outResult, Bit flagZ, Bit flagN, Bit flagC, Bit flagV) {
        flagC.set(inA.get(WORD_SIZE - 1).getAsBool());

        for (int i = 0; i < WORD_SIZE - 1; i++) {
            outResult.set(i + 1, inA.get(i));
        }

        outResult.set(0, false);
        updateZeroFlag(outResult, flagZ);
        updateNegativeFlag(outResult, flagN);
        flagV.set(false);
    }

    public void updateZeroFlag(Word result, Bit flagZ) {
        for (Bit b : result.getAsArray()) {
            if (b.getAsBool()) {
                flagZ.set(false);
                return;
            }
        }
        flagZ.set(true);
    }

    public void updateNegativeFlag(Word result, Bit flagN) {
        flagN.set(result.get(0).getAsBool());
    }
}
