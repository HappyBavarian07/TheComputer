package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.exceptions.isa.IllegalConditionException;

import java.util.Objects;

/*
 * @Author HappyBavarian07
 * @Date September 06, 2026 | 20:33
 */
public enum Condition {
    // Name(Binary Value, Aliases), // Meaning, Flag Condition Formula
    AL(0x0, new String[]{"ALWAYS"}, (Z, N, C, V) -> true), // Always execute, true
    EQ(0x1, new String[]{"Z"}, (Z, N, C, V) -> Z.getAsBool()), // Equal/Zero, Z == 1
    NE(0x2, new String[]{"NZ"}, (Z, N, C, V) -> !Z.getAsBool()), // Not Equal/Non Zero, Z == 0
    CS(0x3, new String[]{"HS"}, (Z, N, C, V) -> C.getAsBool()), // Carry Set/Unsigned higher or same, C == 1
    CC(0x4, new String[]{"LO"}, (Z, N, C, V) -> !C.getAsBool()), // Carry clear/Unsigned lower, C == 0
    MI(0x5, new String[]{"NEG"}, (Z, N, C, V) -> N.getAsBool()), // Negative, N == 1
    PL(0x6, new String[]{"POS"}, (Z, N, C, V) -> !N.getAsBool()), // Positive or zero, N == 0
    VS(0x7, new String[]{""}, (Z, N, C, V) -> V.getAsBool()), // Overflow set, V == 1
    VC(0x8, new String[]{""}, (Z, N, C, V) -> !V.getAsBool()), // Overflow clear, V == 0
    HI(0x9, new String[]{""}, (Z, N, C, V) -> C.getAsBool() && !Z.getAsBool()), // Unsigned strictly higher, C == 1 & Z == 0
    LS(0xA, new String[]{""}, (Z, N, C, V) -> !C.getAsBool() || Z.getAsBool()), // Unsigned lower or same, C == 0 | Z == 1
    GE(0xB, new String[]{""}, (Z, N, C, V) -> N.getAsBool() == V.getAsBool()), // Signed greater then or equal, N == V
    LT(0xC, new String[]{""}, (Z, N, C, V) -> N.getAsBool() != V.getAsBool()), // Signed stricly less then, N != V
    GT(0xD, new String[]{""}, (Z, N, C, V) -> !Z.getAsBool() && N.getAsBool() == V.getAsBool()), // Signed stricly greater then, Z == 0 & N == V
    LE(0xE, new String[]{""}, (Z, N, C, V) -> Z.getAsBool() || N.getAsBool() != V.getAsBool()), // Signed less then or equal, Z == 1 | N != V
    NV(0xF, new String[]{"NEVER"}, (Z, N, C, V) -> false), // Never execute (No-Op), false
    ;

    private final Number binaryValue;
    private final String[] aliases;
    private final ConditionFlagFormula formula;

    Condition(int binaryValue, String[] aliases, ConditionFlagFormula formula) {
        this.binaryValue = binaryValue;
        this.aliases = aliases;
        this.formula = formula;
    }

    public Number binaryValue() {
        return binaryValue;
    }

    public String[] aliases() {
        return aliases;
    }

    public boolean test(Bit Z, Bit N, Bit C, Bit V) {
        return this.formula.evaluate(Z, N, C, V);
    }

    public static Condition fromBinaryValue(Number binaryValue) {
        int target = binaryValue.intValue();
        for (Condition cond : values()) {
            if (cond.binaryValue.intValue() == target) return cond;
        }
        throw new IllegalConditionException("Invalid Condition value received.");
    }

    public static Condition valueOfSafe(String value) {
        if (value == null || value.isEmpty()) return AL;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return AL;
        }
    }
}
