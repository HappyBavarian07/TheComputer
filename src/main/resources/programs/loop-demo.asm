; 64-Bit Nested Loop Demo
; Outer loop x Inner loop matrix multiplication / accumulation
;
; NOTE: updated for the current assembler/ISA (2026-09-28).
; "jnz" is not a valid mnemonic (see
; docs/ASSEMBLY_GUIDE.md#54-condition-suffixes-how-conditional-jumps-are-actually-spelled);
; the condition suffix must attach to a real opcode name, so "not equal"
; jumps are written JMPNE (or JMPNZ). Plain "store" was also removed in
; favor of width-specific STOREB/STOREH/STOREI/STOREW. See
; docs/ASSEMBLY_GUIDE.md#8-discrepancies-found-in-the-bundled-examples.
;
; The result address was also moved from 0x20 to 0x200: this program's 11
; instructions occupy bytes 0..87 under the current 8-byte-instruction ISA,
; so 0x20 (=32) landed inside the code itself (see
; docs/ASSEMBLY_GUIDE.md#discrepancies-found-in-the-bundled-examples).

movi r1, 4           ; outer counter
movi r2, 0           ; product accumulator
movi r3, 1           ; step

outer:
movi r4, 3           ; inner counter

inner:
add r2, r2, r1       ; accumulator += outer counter
sub r4, r4, r3       ; inner counter--
jmpne inner

sub r1, r1, r3       ; outer counter--
jmpne outer

storew 0x200, r2     ; store accumulated sum (64-bit) to RAM 0x200
halt
