; 64-Bit Sum Loop Demo
; Calculates sum of 1..5 using 3-operand addition and loop counter
;
; NOTE: updated for the current assembler/ISA (2026-09-28).
; "jnz" is not a valid mnemonic (see
; docs/ASSEMBLY_GUIDE.md#54-condition-suffixes-how-conditional-jumps-are-actually-spelled);
; the condition suffix must attach to a real opcode name, so "not equal"
; jumps are written JMPNE (or JMPNZ). Plain "store" was also removed in
; favor of width-specific STOREB/STOREH/STOREI/STOREW. See
; docs/ASSEMBLY_GUIDE.md#8-discrepancies-found-in-the-bundled-examples.

movi r1, 5           ; loop counter
movi r2, 0           ; accumulator
movi r3, 1           ; step

loop:
add r2, r2, r1       ; accumulator = accumulator + counter
sub r1, r1, r3       ; counter = counter - 1
jmpne loop

storew 256, r2       ; store result (15) to RAM address 256
halt
