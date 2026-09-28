MOVI r1, 3
MOVI r2, 4

CALL recursive_multiply
HALT


recursive_multiply:
; r1 ist m
; r2 ist n
; r3 ist result
; r10 ist sign store
; code so ungefähr:
; n == 0: ret 0
; n <= 0: -m + recursive_multiply(m, n + 1)
; else m + recursive_multiply(m, n - 1)
;

CMPI r2, 0
MOVIEQ r3, 0 ; n == 0: ret 0
RETEQ

MOVIPL r10, 1 ; sign store PL
MOVIMI r10, -1 ; sign store MI

SUBIPL r2, r2, 1 ; n--
ADDIMI r2, r2, 1 ; n++
CALL recursive_multiply ; recursion

CMPI r10, 0 ; check sign store
JMPPL positive
JMPMI negative

positive:
ADD r3, r3, r1 ; result += m
RET

negative:
SUB r3, r3, r1 ; result -= m
RET

