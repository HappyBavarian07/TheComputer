package de.happybavarian07.computer.gui.assembly;

import de.happybavarian07.computer.isa.Condition;

import java.util.List;

/**
 * Reference data for the 4-bit condition every instruction carries (default
 * AL). A mnemonic composes as OPCODE + CONDITION glued directly together
 * (no separator) -- e.g. JMP + MI = "jmpmi" -- which is how conditional
 * jumps work here: there's no separate JZ/JNZ opcode, just JMP with a
 * non-AL condition. Data matches the verified table in
 * docs/ASSEMBLY_GUIDE.md §5.4.
 */
public record ConditionReference(Condition condition, String aliases, String formula, String meaning, String example, String notes) {

    public static final List<ConditionReference> ALL = List.of(
            new ConditionReference(Condition.AL, "ALWAYS", "true", "Always execute (the default when no suffix is given)", "add r3, r1, r2  (implicitly ALWAYS)", null),
            new ConditionReference(Condition.EQ, "Z", "Z == 1", "Equal / zero", "jmpeq label", null),
            new ConditionReference(Condition.NE, "NZ", "Z == 0", "Not equal / non-zero", "jmpne label  (also jmpnz)",
                    "There is no JNZ opcode. \"JNZ\" alone does not parse -- stripping the NZ alias off \"JNZ\" leaves \"J\", not a real opcode. The full mnemonic is JMPNE or JMPNZ."),
            new ConditionReference(Condition.CS, "HS", "C == 1", "Carry set / unsigned ≥ (\"higher or same\")", "jmpcs label  (also jmphs)", null),
            new ConditionReference(Condition.CC, "LO", "C == 0", "Carry clear / unsigned < (\"lower\")", "jmpcc label  (also jmplo)", null),
            new ConditionReference(Condition.MI, "NEG", "N == 1", "Negative (minus)", "cmpi r1, 0\njmpmi label   ; jump if r1 < 0", null),
            new ConditionReference(Condition.PL, "POS", "N == 0", "Positive or zero (plus)", "cmpi r1, 0\njmppl label   ; jump if r1 >= 0", null),
            new ConditionReference(Condition.VS, "—", "V == 1", "Overflow set", "jmpvs label", null),
            new ConditionReference(Condition.VC, "—", "V == 0", "Overflow clear", "jmpvc label", null),
            new ConditionReference(Condition.HI, "—", "C == 1 & Z == 0", "Unsigned strictly greater than", "jmphi label", null),
            new ConditionReference(Condition.LS, "—", "C == 0 | Z == 1", "Unsigned less than or equal", "jmpls label", null),
            new ConditionReference(Condition.GE, "—", "N == V", "Signed greater than or equal", "cmpi r1, 0\njmpge label   ; jump if r1 >= 0", null),
            new ConditionReference(Condition.LT, "—", "N != V", "Signed strictly less than", "cmpi r1, 0\njmplt label   ; jump if r1 < 0",
                    "After comparing against 0 specifically, LT and MI happen to agree (both mean \"negative\"). Against a non-zero value they differ: LT is the correct signed less-than, MI only tells you the raw sign bit of the subtraction result."),
            new ConditionReference(Condition.GT, "—", "Z == 0 & N == V", "Signed strictly greater than", "cmp r1, r2\njmpgt label   ; jump if r1 > r2", null),
            new ConditionReference(Condition.LE, "—", "Z == 1 | N != V", "Signed less than or equal", "cmp r1, r2\njmple label   ; jump if r1 <= r2", null),
            new ConditionReference(Condition.NV, "NEVER", "false", "Never execute (effectively a NOP)", "addnv r3, r1, r2  ; never runs", null)
    );
}
