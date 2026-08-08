package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.logic.LogicGates;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 16:24
 */
public final class FullAdder {
    private Bit scratchAxorB;
    private Bit scratchAandB;
    private Bit scratchCInAndAxorB;

    public FullAdder() {
        scratchAxorB = new Bit(false);
        scratchAandB = new Bit(false);
        scratchCInAndAxorB = new Bit(false);
    }

    /**
     * Accepts two data inputs A and B, plus an incoming carry bit inCarry. Calculates Sum and CarryOut
     * @param inA A Bit
     * @param inB B Bit
     * @param inCarry Carry In Bit
     * @param outSum Sum Bit
     * @param outCarry Carry Out Bit
     */
    public void execute(Bit inA, Bit inB, Bit inCarry, Bit outSum, Bit outCarry) {
        LogicGates.xor(inA, inB, scratchAxorB); // A ^ B
        LogicGates.and(inA, inB, scratchAandB); // A & B

        LogicGates.xor(scratchAxorB, inCarry, outSum); // (A ^ B) ^ Cin
        LogicGates.and(inCarry, scratchAxorB, scratchCInAndAxorB); // Cin & (A ^ B);

        LogicGates.or(scratchAandB, scratchCInAndAxorB, outCarry); // (A ^ B) \/ (Cin & (A ^ B))
    }
}
