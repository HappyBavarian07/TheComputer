; 64-Bit Sum Loop Demo
; Calculates sum of 1..5 using 3-operand addition and loop counter

movi r1, 5           ; loop counter
movi r2, 0           ; accumulator
movi r3, 1           ; step

loop:
add r2, r2, r1       ; accumulator = accumulator + counter
sub r1, r1, r3       ; counter = counter - 1
jnz loop

store 256, r2        ; store result (15) to RAM address 256
halt

