MOVI r1, 5
MOVI r2, 25

CALL recursive_ggt
HALT


recursive_ggt:
    ; r1 ist a
    ; r2 ist b
    ; r3 ist result
    ; code so ungefähr:
    ; a % b == 0: ret 1
    ; ggt(b, a % b)
    ;

    MOD r4, r1, r2
    CMPI r4, 0
    JMPNE do_recursion ; n == 0: ret 1

    MOV r3, r2

    RET

do_recursion:
    MOV r1, r2        ; r1 = b
    MOV r2, r4        ; r2 = r4 (Rest)

    CALL recursive_ggt ; recursion
    RET