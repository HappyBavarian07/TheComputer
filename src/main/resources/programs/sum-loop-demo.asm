load r1, 5
load r2, 0
load r3, 1
loop:
add r2, r1
sub r1, r3
jnz loop
store 256, r2
halt
