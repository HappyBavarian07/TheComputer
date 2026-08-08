package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.logic.LogicGates;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 16:22
 */
public final class HalfAdder {
    public void execute(Bit inA, Bit inB, Bit outSum, Bit outCarry) {
        LogicGates.xor(inA, inB, outSum);
        LogicGates.and(inA, inB, outCarry);
    }
}
