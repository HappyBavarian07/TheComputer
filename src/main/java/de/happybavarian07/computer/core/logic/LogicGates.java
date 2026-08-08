package de.happybavarian07.computer.core.logic;

import de.happybavarian07.computer.core.bit.Bit;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 13:19
 */
public class LogicGates {

    // allocates a new bit as the result
    public static Bit not(Bit in) {
        return new Bit(!in.getAsBool());
    }

    public static Bit and(Bit inA, Bit inB) {
        return new Bit(inA.getAsBool() && inB.getAsBool());
    }

    public static Bit or(Bit inA, Bit inB) {
        return new Bit(inA.getAsBool() || inB.getAsBool());
    }

    public static Bit xor(Bit inA, Bit inB) {
        return new Bit(inA.getAsBool() != inB.getAsBool());
    }

    public static Bit nand(Bit inA, Bit inB) {
        return new Bit(!(inA.getAsBool() && inB.getAsBool()));
    }

    public static Bit nor(Bit inA, Bit inB) {
        return new Bit(!(inA.getAsBool() || inB.getAsBool()));
    }
    // destination based from here to avoid alloc
    /**
     * Writes {@code !in} into {@code result}. No allocation.
     */
    public static void not(Bit in, Bit result) {
        result.set(!in.getAsBool());
    }

    /**
     * Writes {@code a AND b} into {@code result}. No allocation.
     */
    public static void and(Bit inA, Bit inB, Bit result) {
        result.set(inA.getAsBool() && inB.getAsBool());
    }

    /**
     * Writes {@code a OR b} into {@code result}. No allocation.
     */
    public static void or(Bit inA, Bit inB, Bit result) {
        result.set(inA.getAsBool() || inB.getAsBool());
    }

    /**
     * Writes {@code a XOR b} into {@code result}. No allocation.
     */
    public static void xor(Bit inA, Bit inB, Bit result) {
        result.set(inA.getAsBool() != inB.getAsBool());
    }

    /**
     * Writes {@code !(a AND b)} into {@code result}. No allocation.
     */
    public static void nand(Bit inA, Bit inB, Bit result) {
        result.set(!(inA.getAsBool() && inB.getAsBool()));
    }

    /**
     * Writes {@code !(a OR b)} into {@code result}. No allocation.
     */
    public static void nor(Bit inA, Bit inB, Bit result) {
        result.set(!(inA.getAsBool() || inB.getAsBool()));
    }
}
