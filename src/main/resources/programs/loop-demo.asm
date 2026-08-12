load r1, 3
loop:
    jz end
    sub r1, r1
    jmp loop
end:
    halt
