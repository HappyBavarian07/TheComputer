; 64-Bit RAM-heavy multiplication demo
; Factors stored in memory, read through LOADR, and products written to result area.

movi r1, 3
store 256, r1        ; factor 1 = 3
movi r1, 4
store 264, r1        ; factor 2 = 4
movi r1, 5
store 272, r1        ; factor 3 = 5
movi r1, 6
store 280, r1        ; factor 4 = 6
movi r1, 7
store 288, r1        ; factor 5 = 7
movi r1, 8
store 296, r1        ; factor 6 = 8
movi r1, 9
store 304, r1        ; factor 7 = 9
movi r1, 2
store 312, r1        ; factor 8 = 2

; 1. Compute 3 * 4 via hardware MUL
movi r7, 256
loadr r1, r7         ; r1 = 3
movi r7, 264
loadr r2, r7         ; r2 = 4
mul r3, r1, r2       ; r3 = 3 * 4 = 12
store 512, r3

; 2. Compute 5 * 6 via hardware MUL
movi r7, 272
loadr r1, r7         ; r1 = 5
movi r7, 280
loadr r2, r7         ; r2 = 6
mul r3, r1, r2       ; r3 = 5 * 6 = 30
store 520, r3

; 3. Compute 7 * 8 via hardware MUL
movi r7, 288
loadr r1, r7         ; r1 = 7
movi r7, 296
loadr r2, r7         ; r2 = 8
mul r3, r1, r2       ; r3 = 7 * 8 = 56
store 528, r3

; 4. Compute 9 * 2 via hardware MUL
movi r7, 304
loadr r1, r7         ; r1 = 9
movi r7, 312
loadr r2, r7         ; r2 = 2
mul r3, r1, r2       ; r3 = 9 * 2 = 18
store 536, r3

halt

