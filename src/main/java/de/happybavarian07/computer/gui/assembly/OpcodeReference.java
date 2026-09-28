package de.happybavarian07.computer.gui.assembly;

import de.happybavarian07.computer.isa.OpCode;

import java.util.List;

/**
 * Rich, per-instruction reference data: what each opcode actually does,
 * which flags it touches, a working example, and any gotcha worth calling
 * out. This mirrors the verified content in {@code docs/ASSEMBLY_GUIDE.md}
 * §6 — every effect/flags/example here was checked against the real
 * assembler and CPU, not just read off the opcode name. Kept as
 * structured data (rather than one long description string) so the
 * reference dialog can show it without truncating in a table cell.
 */
public record OpcodeReference(OpCode opCode, String category, String effect, String flags, String example, String notes) {

    public static final List<OpcodeReference> ALL = List.of(
            // --- System & control ---
            new OpcodeReference(OpCode.NOP, "System & control",
                    "No operation; PC advances by 8 like any other instruction.",
                    "—", "nop", null),
            new OpcodeReference(OpCode.MOV, "System & control",
                    "rd = rs1 (register-to-register copy).",
                    "—", "mov r1, r2", null),
            new OpcodeReference(OpCode.MOVI, "System & control",
                    "rd = imm (direct constant load, no memory access).",
                    "—", "movi r1, 50", null),
            new OpcodeReference(OpCode.HALT, "System & control",
                    "Stops the CPU (isHalted = true). PC does not advance further.",
                    "—", "halt",
                    "Opcode value is 0x03 in the running code. ISA_SPECIFICATION.md's table says 0x05 — that's a stale spec value; the code is ground truth here."),

            // --- Arithmetic ---
            new OpcodeReference(OpCode.ADD, "Arithmetic",
                    "rd = rs1 + rs2",
                    "Z N C V", "add r3, r1, r2", null),
            new OpcodeReference(OpCode.ADDI, "Arithmetic",
                    "rd = rs1 + imm",
                    "Z N C V", "addi r1, r1, 100",
                    "No negative immediates: the lexer has no unary-minus syntax at all. To subtract, use SUB/SUBI."),
            new OpcodeReference(OpCode.SUB, "Arithmetic",
                    "rd = rs1 - rs2",
                    "Z N C V", "sub r4, r1, r2", null),
            new OpcodeReference(OpCode.SUBI, "Arithmetic",
                    "rd = rs1 - imm",
                    "Z N C V", "subi r4, r1, 8", null),
            new OpcodeReference(OpCode.MUL, "Arithmetic",
                    "rd = rs1 * rs2",
                    "Z N V", "mul r5, r1, r2", null),
            new OpcodeReference(OpCode.DIV, "Arithmetic",
                    "rd = rs1 / rs2 (integer division)",
                    "Z N", "div r6, r1, r2",
                    "Does NOT touch C or V — unlike AND/OR/etc. (which explicitly clear them), DIV leaves C/V at whatever a prior instruction last set. Don't branch on C/V right after a DIV expecting them to reflect it."),
            new OpcodeReference(OpCode.MOD, "Arithmetic",
                    "rd = rs1 % rs2",
                    "Z N", "mod r7, r1, r2",
                    "Same C/V caveat as DIV — they're left untouched, not cleared."),
            new OpcodeReference(OpCode.CMP, "Arithmetic",
                    "Computes rd - rs1 and updates flags. Does NOT write a result register — rd here is the minuend, not a destination.",
                    "Z N C V", "cmp r1, r2", null),
            new OpcodeReference(OpCode.CMPI, "Arithmetic",
                    "Computes rd - imm and updates flags. No register write.",
                    "Z N C V", "cmpi r4, 0", null),

            // --- Bitwise & shifts ---
            new OpcodeReference(OpCode.AND, "Bitwise & shifts", "rd = rs1 & rs2", "Z N (clears C, V)", "and r3, r1, r2", null),
            new OpcodeReference(OpCode.ANDI, "Bitwise & shifts", "rd = rs1 & imm", "Z N (clears C, V)", "andi r3, r1, 0xF", null),
            new OpcodeReference(OpCode.OR, "Bitwise & shifts", "rd = rs1 | rs2", "Z N (clears C, V)", "or r3, r1, r2", null),
            new OpcodeReference(OpCode.ORI, "Bitwise & shifts", "rd = rs1 | imm", "Z N (clears C, V)", "ori r3, r1, 1", null),
            new OpcodeReference(OpCode.XOR, "Bitwise & shifts", "rd = rs1 ⊕ rs2", "Z N (clears C, V)", "xor r3, r1, r2", null),
            new OpcodeReference(OpCode.XORI, "Bitwise & shifts", "rd = rs1 ⊕ imm", "Z N (clears C, V)", "xori r3, r1, 1", null),
            new OpcodeReference(OpCode.NOT, "Bitwise & shifts", "rd = ~rs1", "Z N (clears C, V)", "not r2, r1", null),
            new OpcodeReference(OpCode.SHL, "Bitwise & shifts", "rd = rs1 << rs2",
                    "Z N C (clears V)", "shl r2, r1, r3",
                    "Shift amount is masked to WORD_BITS-1 (&63), so shifting by ≥64 is well-defined, not undefined behavior."),
            new OpcodeReference(OpCode.SHLI, "Bitwise & shifts", "rd = rs1 << imm", "Z N C (clears V)", "shli r2, r1, 2", null),
            new OpcodeReference(OpCode.SHR, "Bitwise & shifts", "rd = rs1 >>> rs2 (logical, unsigned)",
                    "Z N C (clears V)", "shr r2, r1, r3",
                    "Logical shift only — SHR never sign-extends, even for negative values."),
            new OpcodeReference(OpCode.SHRI, "Bitwise & shifts", "rd = rs1 >>> imm (logical, unsigned)", "Z N C (clears V)", "shri r2, r1, 2", null),

            // --- Control flow & subroutines ---
            new OpcodeReference(OpCode.JMP, "Control flow & subroutines",
                    "PC = imm. There is no separate JZ/JNZ/JGT opcode — combine JMP with a condition suffix instead (JMPEQ, JMPNE, JMPGT, ...).",
                    "—", "jmp loop  /  jmpne loop", null),
            new OpcodeReference(OpCode.CALL, "Control flow & subroutines",
                    "Pushes PC+8 (the return address) onto the stack, then PC = imm.",
                    "—", "call subroutine",
                    "Was broken from the 64-bit migration until 2026-09-28 (commit 90bf884): jumped to the wrong address because push() clobbered a shared scratch field CALL read afterward. Fixed and verified — safe to use now."),
            new OpcodeReference(OpCode.RET, "Control flow & subroutines",
                    "PC = pop() (returns to the address CALL/CALLR pushed).",
                    "—", "ret",
                    "Was broken (crashed with an IllegalArgumentException on every use) until the same 2026-09-28 fix as CALL. Fixed and verified."),
            new OpcodeReference(OpCode.JMPR, "Control flow & subroutines", "PC = rs1 (register-indirect jump).", "—", "jmpr r9", null),
            new OpcodeReference(OpCode.CALLR, "Control flow & subroutines",
                    "Pushes PC+8, then PC = rs1 (register-indirect call).",
                    "—", "callr r9", null),

            // --- Stack ---
            new OpcodeReference(OpCode.PUSH, "Stack",
                    "SP -= 8; RAM[SP] = rs1 (full 64-bit register).",
                    "—", "push r1",
                    "Raises StackOverflowException if this would push past the stack's lower bound (Architecture.STACK_LIMIT_ADDRESS)."),
            new OpcodeReference(OpCode.POP, "Stack",
                    "rd = RAM[SP]; SP += 8.",
                    "—", "pop r2",
                    "Raises the same StackOverflowException class if popping past the stack's reset top — i.e. an empty-stack pop, despite the exception's name, not just an overflow."),

            // --- Memory ---
            new OpcodeReference(OpCode.LOADB, "Memory", "rd = zero_extend(RAM[imm]) — 8-bit load.", "—", "loadb r1, 0x100", null),
            new OpcodeReference(OpCode.LOADH, "Memory", "rd = zero_extend(RAM[imm]) — 16-bit load.", "—", "loadh r1, 0x100", null),
            new OpcodeReference(OpCode.LOADI, "Memory", "rd = zero_extend(RAM[imm]) — 32-bit load.", "—", "loadi r1, 0x100", null),
            new OpcodeReference(OpCode.LOADW, "Memory", "rd = RAM[imm] — 64-bit load, the full native word.", "—", "loadw r1, 0x100", null),
            new OpcodeReference(OpCode.STOREB, "Memory", "RAM[imm] = rd[7:0] — 8-bit store.", "—", "storeb 0x100, r1", null),
            new OpcodeReference(OpCode.STOREH, "Memory", "RAM[imm] = rd[15:0] — 16-bit store.", "—", "storeh 0x100, r1", null),
            new OpcodeReference(OpCode.STOREI, "Memory", "RAM[imm] = rd[31:0] — 32-bit store.", "—", "storei 0x100, r1", null),
            new OpcodeReference(OpCode.STOREW, "Memory", "RAM[imm] = rd[63:0] — 64-bit store.", "—", "storew 0x100, r1", null),
            new OpcodeReference(OpCode.LOADR, "Memory",
                    "rd = RAM[rs1 + offset] — register-indirect load with an offset.",
                    "—", "loadr r1, r2, 0", null),
            new OpcodeReference(OpCode.STORER, "Memory",
                    "RAM[rs1 + offset] = rd — first operand is the value stored, second is the base register (same operand-role order as LOADR).",
                    "—", "storer r1, r2, 0",
                    "ISA_SPECIFICATION.md's table writes this the other way around (\"RAM[Rd + imm32] = Rs1\"). Verified by direct test that the actual behavior is as stated above — the spec's formula has Rd/Rs1 transposed.")
    );

    /**
     * Shown once, not per-row: every imm32/offset32 operand above is
     * clamped by the assembler to 0..(Architecture.MEMORY_SIZE_BYTES - 1),
     * not the full 0..0xFFFFFFFF the 32-bit field could hold, and negative
     * literals don't parse at all (no unary minus in the lexer). The
     * accepted range grows with RAM size, not with the field width —
     * see docs/ASSEMBLY_GUIDE.md §9 for the exact current bound.
     */
    public static final String IMMEDIATE_RANGE_NOTE =
            "Every imm32/offset32 operand above is clamped by the assembler to 0..(Architecture.MEMORY_SIZE_BYTES − 1), "
            + "not the full 0..0xFFFFFFFF the 32-bit field could hold, and negative literals don't parse at all "
            + "(no unary minus in the lexer). See docs/ASSEMBLY_GUIDE.md §9 for the exact current bound.";
}
