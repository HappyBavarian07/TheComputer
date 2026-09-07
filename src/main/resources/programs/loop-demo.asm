; 64-Bit Nested Loop Demo
; Outer loop x Inner loop matrix multiplication / accumulation

movi r1, 4           ; outer counter
movi r2, 0           ; product accumulator
movi r3, 1           ; step

outer:
movi r4, 3           ; inner counter

inner:
add r2, r2, r1       ; accumulator += outer counter
sub r4, r4, r3       ; inner counter--
jnz inner

sub r1, r1, r3       ; outer counter--
jnz outer

store 0x20, r2       ; store accumulated sum to RAM 0x20
halt

