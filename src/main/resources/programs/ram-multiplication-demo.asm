; 64-Bit RAM-heavy multiplication demo
; Factors stored in memory, read through LOADR, and products computed by a
; shared MULTIPLY_PAIR subroutine (CALL/RET) instead of repeating the same
; load/load/mul sequence four times.
;
; NOTE: updated for the current assembler/ISA (2026-09-28).
; Plain "store" was removed in favor of width-specific STOREB/STOREH/
; STOREI/STOREW, and LOADR takes 3 operands (dest, base register, offset)
; per its RD_RS1_OFFSET32 encoding -- a bare 2-operand "loadr rd, rbase"
; no longer parses. See docs/ASSEMBLY_GUIDE.md#8-discrepancies-found-in-the-bundled-examples.
;
; Data addresses live at 2048.. / 4096.. rather than 256.. / 512.. : under
; the old 32-bit ISA (4-byte instructions) this program's code fit in well
; under 256 bytes, so 256.. was safe "data space". Under the current 64-bit
; ISA (8-byte instructions) that assumption no longer holds -- see
; docs/ASSEMBLY_GUIDE.md#8-discrepancies-found-in-the-bundled-examples for the
; corruption that caused. 2048/4096 leave a large margin between code and data.
;
; This program originally repeated its "load two factors, multiply" logic
; four times inline. As of 2026-09-28 (commit 90bf884, "fix(cpu): fix call
; and jmpr"), CALL and RET both work correctly, so it now factors that logic
; into a shared subroutine instead. Calling convention used below is ad hoc
; -- the ISA/assembler do not define one at the instruction-set level, and
; this is deliberately simpler than the full ABI in LANGUAGE_SPECIFICATION.md
; (see docs/ASSEMBLY_GUIDE.md#74-subroutines for both). By this program's own
; convention:
;   in:       r7 = address of factor A, r8 = address of factor B
;   out:      r3 = product
;   clobbers: r1, r2
;   the caller stores r3 whereever it needs the result

movi r1, 3
storew 2048, r1        ; factor 1 = 3
movi r1, 4
storew 2056, r1        ; factor 2 = 4
movi r1, 5
storew 2064, r1        ; factor 3 = 5
movi r1, 6
storew 2072, r1        ; factor 4 = 6
movi r1, 7
storew 2080, r1        ; factor 5 = 7
movi r1, 8
storew 2088, r1        ; factor 6 = 8
movi r1, 9
storew 2096, r1        ; factor 7 = 9
movi r1, 2
storew 2104, r1        ; factor 8 = 2

movi r7, 2048
movi r8, 2056
call multiply_pair
storew 4096, r3         ; 3 * 4 = 12

movi r7, 2064
movi r8, 2072
call multiply_pair
storew 4104, r3         ; 5 * 6 = 30

movi r7, 2080
movi r8, 2088
call multiply_pair
storew 4112, r3         ; 7 * 8 = 56

movi r7, 2096
movi r8, 2104
call multiply_pair
storew 4120, r3         ; 9 * 2 = 18

halt

; --- subroutine: multiply_pair ---
; in: r7 = &A, r8 = &B ; out: r3 = A * B ; clobbers: r1, r2
multiply_pair:
loadr r1, r7, 0
loadr r2, r8, 0
mul r3, r1, r2
ret
