load r1, 4
load r2, 0
load r3, 1
outer:
load r4, 3
inner:
add r2, r1
sub r4, r3
jnz inner
sub r1, r3
jnz outer
store 0x20, r2
halt
