; 64-Bit Math Demo
; Demonstrates 3-operand math, hardware multiplication, and division

movi r1, 42
movi r2, 8
add r3, r1, r2       ; r3 = r1 + r2 = 50
sub r4, r1, r2       ; r4 = r1 - r2 = 34
mul r5, r1, r2       ; r5 = r1 * r2 = 336
div r6, r1, r2       ; r6 = r1 / r2 = 5
mod r7, r1, r2       ; r7 = r1 % r2 = 2
halt

