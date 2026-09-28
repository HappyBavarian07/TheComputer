MOVI r1, 10
MOVI r2, 3

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
    ; code so ungefähr:
    ; n == 0: ret 1
    ; n % 2 == 0: half = recursiveExponentSquare(x, n / 2); ret half * half
    ; else: x * recursive_exponent_square(m, n - 1)
    ;

    CMPI r2, 0
    MOVIEQ r3, 1 ; n == 0: ret 1
    RETEQ

    SUBI r2, r2, 1 ; n--
    CALL recursive_exponent_square ; recursion

    ADDI r2, r2, 1 ; undo r2 change
    MUL r3, r3, r1 ; result *= x
    RET