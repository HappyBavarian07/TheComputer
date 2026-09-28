MOVI r1, 3
MOVI r2, 4

CALL recursive_multiply
HALT


recursive_multiply:
; r1 is m
; r2 is n
; code roughly:
; n == 0: ret 0
; n <= 0: -m + recursive_multiply(m, n + 1)
; else m + recursive_multiply(m, n - 1)
;
CMPI r2, 0
MOVIEQ r3, 0 ; n == 0: ret 0
RETEQ

MOVIPL r4, 1
MOVIMI r4, -1

SUBIPL r2, r2, 1
ADDIMI r2, r2, 1
CALL recursive_multiply

CMPI r4, 0
JMPPL positive
JMPMI negative

positive:
ADD r3, r3, r1
RET

negative:
SUB r3, r3, r1
RET

