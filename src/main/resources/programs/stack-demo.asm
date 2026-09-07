; 64-Bit Stack Demo
; Demonstrates pushing and popping 64-bit values to/from stack

movi r1, 123456
push r1
movi r1, 0           ; clear r1
pop r2              ; r2 gets 123456
halt

