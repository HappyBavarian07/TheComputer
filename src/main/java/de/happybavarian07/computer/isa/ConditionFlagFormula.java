package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.core.bit.Bit;

/*
 * @Author HappyBavarian07
 * @Date September 06, 2026 | 20:46
 */
@FunctionalInterface
public interface ConditionFlagFormula {
    boolean evaluate(Bit Z, Bit N, Bit C, Bit V);
}
