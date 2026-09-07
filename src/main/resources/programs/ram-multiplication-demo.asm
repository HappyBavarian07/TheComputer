; RAM-heavy multiplication demo
; Factors are stored in memory, read back through LOADR, and the
; products are written to a separate result area.

load r1, 3
store 256, r1        ; 3
load r1, 4
store 260, r1        ; 4
load r1, 5
store 264, r1        ; 5
load r1, 6
store 268, r1        ; 6
load r1, 7
store 272, r1        ; 7
load r1, 8
store 276, r1        ; 8
load r1, 9
store 280, r1        ; 9
load r1, 2
store 284, r1        ; 2

load r5, 1           ; decrement value for the loops

load r7, 256
loadr r1, r7         ; multiplicand = 3
load r7, 260
loadr r2, r7         ; multiplier = 4
load r3, 0
mov r4, r2
mul_34:
add r3, r1
sub r4, r5
jnz mul_34
store 512, r3        ; 3 x 4 = 12

load r7, 264
loadr r1, r7         ; multiplicand = 5
load r7, 268
loadr r2, r7         ; multiplier = 6
load r3, 0
mov r4, r2
mul_56:
add r3, r1
sub r4, r5
jnz mul_56
store 516, r3        ; 5 x 6 = 30

load r7, 272
loadr r1, r7         ; multiplicand = 7
load r7, 276
loadr r2, r7         ; multiplier = 8
load r3, 0
mov r4, r2
mul_78:
add r3, r1
sub r4, r5
jnz mul_78
store 520, r3        ; 7 x 8 = 56

load r7, 280
loadr r1, r7         ; multiplicand = 9
load r7, 284
loadr r2, r7         ; multiplier = 2
load r3, 0
mov r4, r2
mul_92:
add r3, r1
sub r4, r5
jnz mul_92
store 524, r3        ; 9 x 2 = 18

halt
