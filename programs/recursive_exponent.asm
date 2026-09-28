MOVI r1, 5
MOVI r2, 25

CALL recursive_exponent
HALT


recursive_exponent:
    ; r1 ist x
    ; r2 ist n
    ; r3 ist result
    ; code so ungefähr:
    ; n == 0: ret 1
    ; x * recursive_exponent(m, n - 1)
    ;

    CMPI r2, 0
    MOVIEQ r3, 1 ; n == 0: ret 1
    RETEQ

    SUBI r2, r2, 1 ; n--
    CALL recursive_exponent ; recursion

    ADDI r2, r2, 1 ; undo r2 change
    MUL r3, r3, r1 ; result *= x
    RET


recursive_exponent_square:
    ; r1 ist x
    ; r2 ist n
    ; r3 ist result
    ; r10 ist scratch fuer den gerade/ungerade Test
    ; code so ungefaehr:
    ; n == 0: ret 1
    ; n % 2 == 0: half = recursiveExponentSquare(x, n / 2); ret half * half
    ; else: x * recursive_exponent_square(x, n - 1)
    ;

    CMPI r2, 0
    MOVIEQ r3, 1 ; n == 0: ret 1
    RETEQ

    ANDI r10, r2, 1 ; Z = 1 wenn n gerade
    JMPNE odd

    even:
        SHRI r2, r2, 1 ; n = n / 2
        CALL recursive_exponent_square ; half
        SHLI r2, r2, 1 ; undo r2 change
        MUL r3, r3, r3 ; result = half * half
        RET

    odd:
        SUBI r2, r2, 1 ; n--
        CALL recursive_exponent_square ; recursion
        ADDI r2, r2, 1 ; undo r2 change
        MUL r3, r3, r1 ; result *= x
        RET