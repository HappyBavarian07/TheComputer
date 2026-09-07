# 64-Bit Instruction Set Architecture (ISA) Specification

**Status**: Ground Truth Reference  
**Architecture**: Native 64-bit Word, 32-bit Address Space, Fixed 64-bit (8-byte) Instructions  
**Last Updated**: 2026-09-06

---

## 1. Instruction Bit Layout (64 Bits / 8 Bytes)

Every instruction is fixed-width **64 bits (8 bytes)**, little-endian encoded.

```
 63      56 55  52 51    46 45    40 39    34 33 32 31                             0
+----------+------+--------+--------+--------+-----+--------------------------------+
|  OPCODE  | COND |   Rd   |  Rs1   |  Rs2   | RSV |             IMM32              |
|  (8-bit) |(4-bit)| (6-bit)| (6-bit)| (6-bit)|(2b) |            (32-bit)            |
+----------+------+--------+--------+--------+-----+--------------------------------+
```

### Bit Field Extraction Masks & Shifts
```
OPCODE   = (inst >>> 56) & 0xFFL
COND     = (inst >>> 52) & 0x0FL
Rd       = (inst >>> 46) & 0x3FL
Rs1      = (inst >>> 40) & 0x3FL
Rs2      = (inst >>> 34) & 0x3FL
RESERVED = (inst >>> 32) & 0x03L
IMM32    = (int)(inst & 0xFFFFFFFFL)
```

---

## 2. Condition Codes (`COND`, Bits 55:52)

Every instruction carries a 4-bit condition. If the condition evaluates to `false` against the CPU's `FLAGS` ($Z, N, C, V$), the CPU skips execution and increments $PC \mathrel{+}= 8$.

| Code | Enum | Formula | Description |
| :---: | :--- | :--- | :--- |
| **`0x0`** | **`AL`** | `true` | **Always** (default unconditional execution) |
| **`0x1`** | **`EQ`** | $Z == 1$ | Equal / Zero |
| **`0x2`** | **`NE`** | $Z == 0$ | Not Equal / Non-Zero |
| **`0x3`** | **`CS`** | $C == 1$ | Carry Set / Unsigned Higher or Same (`HS`) |
| **`0x4`** | **`CC`** | $C == 0$ | Carry Clear / Unsigned Lower (`LO`) |
| **`0x5`** | **`MI`** | $N == 1$ | Minus / Negative (`NEG`) |
| **`0x6`** | **`PL`** | $N == 0$ | Plus / Positive or Zero (`POS`) |
| **`0x7`** | **`VS`** | $V == 1$ | Overflow Set |
| **`0x8`** | **`VC`** | $V == 0$ | Overflow Clear |
| **`0x9`** | **`HI`** | $C == 1 \ \& \ Z == 0$ | Unsigned Higher |
| **`0xA`** | **`LS`** | $C == 0 \ \vert \ Z == 1$ | Unsigned Lower or Same |
| **`0xB`** | **`GE`** | $N == V$ | Signed Greater than or Equal |
| **`0xC`** | **`LT`** | $N \ne V$ | Signed Less than |
| **`0xD`** | **`GT`** | $Z == 0 \ \& \ N == V$ | Signed Greater than |
| **`0xE`** | **`LE`** | $Z == 1 \ \vert \ N \ne V$ | Signed Less than or Equal |
| **`0xF`** | **`NV`** | `false` | Never execute (No-op / Reserved) |

---

## 3. Operand Mappings (`OperandMapping`)

| Mapping Enum | Arity | Format Example | Description |
| :--- | :---: | :--- | :--- |
| **`NONE`** | 0 | `nop`, `halt`, `ret` | No operands. |
| **`RD_RS1_RS2`** | 3 | `add r3, r1, r2` | 3 Registers: Destination $R_d$, Source 1 $R_{s1}$, Source 2 $R_{s2}$. |
| **`RD_RS1_IMM32`**| 3 | `addi r1, r1, 100` | 2 Registers + Immediate: $R_d = R_{s1} \text{ OP } \text{imm32}$. |
| **`RD_RS1`** | 2 | `mov r1, r2`, `not r1, r2`, `cmp r1, r2` | 2 Registers. |
| **`RD_IMM32`** | 2 | `loadw r1, 0x1000`, `movi r1, 50` | Register Destination + 32-bit Address/Immediate. |
| **`IMM32_RD`** | 2 | `storew 0x1000, r1` | 32-bit Address + Register Source. |
| **`RD_RS1_OFFSET32`**| 3 | `loadr r1, r2, 0` | Register + Base Pointer Register + 32-bit Offset. |
| **`IMM32_ONLY`** | 1 | `jmp 0x2000`, `call 0x3000` | Target 32-bit Address / Offset. |
| **`RS1_ONLY`** | 1 | `push r1`, `jmpr r1`, `callr r1` | Single Source Register. |
| **`RD_ONLY`** | 1 | `pop r1` | Single Destination Register. |

---

## 4. Master OpCode Table (8-Bit Space: `0x00` – `0xFF`)

### Category 0: System & Movement (`0x00` – `0x0F`)
| Opcode | Hex | Mapping | Arity | Operation | Description |
| :--- | :---: | :--- | :---: | :--- | :--- |
| **`NOP`** | `0x00` | `NONE` | 0 | *No operation* | 1-cycle delay/alignment. |
| **`MOV`** | `0x01` | `RD_RS1` | 2 | $R_d = R_{s1}$ | Register-to-register copy. |
| **`MOVI`** | `0x02` | `RD_IMM32` | 2 | $R_d = \text{imm32}$ | Direct constant load into $R_d$ (no RAM access). |
| **`HALT`** | `0x05` | `NONE` | 0 | `isHalted = true` | Stops CPU execution. |

---

### Category 1: Arithmetic Operations (`0x10` – `0x1F`)
| Opcode | Hex | Mapping | Arity | Operation | Flags Updated |
| :--- | :---: | :--- | :---: | :--- | :---: |
| **`ADD`** | `0x10` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} + R_{s2}$ | $Z, N, C, V$ |
| **`ADDI`** | `0x11` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} + \text{sign\_extend}(\text{imm32})$ | $Z, N, C, V$ |
| **`SUB`** | `0x12` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} - R_{s2}$ | $Z, N, C, V$ |
| **`SUBI`** | `0x13` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} - \text{sign\_extend}(\text{imm32})$ | $Z, N, C, V$ |
| **`MUL`** | `0x14` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \times R_{s2}$ | $Z, N, V$ |
| **`DIV`** | `0x15` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} / R_{s2}$ | $Z, N$ |
| **`MOD`** | `0x16` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \% R_{s2}$ | $Z, N$ |
| **`CMP`** | `0x17` | `RD_RS1` | 2 | Flags on $(R_d - R_{s1})$ | $Z, N, C, V$ |
| **`CMPI`** | `0x18` | `RD_IMM32` | 2 | Flags on $(R_d - \text{imm32})$ | $Z, N, C, V$ |

---

### Category 2: Bitwise & Logic (`0x20` – `0x2F`)
| Opcode | Hex | Mapping | Arity | Operation | Flags Updated |
| :--- | :---: | :--- | :---: | :--- | :---: |
| **`AND`** | `0x20` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \ \& \ R_{s2}$ | $Z, N$ |
| **`ANDI`** | `0x21` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} \ \& \ \text{zero\_extend}(\text{imm32})$ | $Z, N$ |
| **`OR`** | `0x22` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \ \vert \ R_{s2}$ | $Z, N$ |
| **`ORI`** | `0x23` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} \ \vert \ \text{zero\_extend}(\text{imm32})$ | $Z, N$ |
| **`XOR`** | `0x24` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \oplus R_{s2}$ | $Z, N$ |
| **`XORI`** | `0x25` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} \oplus \text{zero\_extend}(\text{imm32})$ | $Z, N$ |
| **`NOT`** | `0x26` | `RD_RS1` | 2 | $R_d = \sim R_{s1}$ | $Z, N$ |
| **`SHL`** | `0x27` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \ll R_{s2}$ | $Z, N, C$ |
| **`SHLI`** | `0x28` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} \ll \text{imm32}$ | $Z, N, C$ |
| **`SHR`** | `0x29` | `RD_RS1_RS2` | 3 | $R_d = R_{s1} \ggg R_{s2}$ | $Z, N, C$ |
| **`SHRI`** | `0x2A` | `RD_RS1_IMM32` | 3 | $R_d = R_{s1} \ggg \text{imm32}$ | $Z, N, C$ |

---

### Category 3: Control Flow & Subroutines (`0x30` – `0x3F`)
| Opcode | Hex | Mapping | Arity | Operation | Description |
| :--- | :---: | :--- | :---: | :--- | :--- |
| **`JMP`** | `0x30` | `IMM32_ONLY` | 1 | $PC = \text{imm32}$ | Direct jump. Handles all conditional jumps (`JZ`, `JNZ`, `JG`, etc.) via `Condition`. |
| **`CALL`** | `0x31` | `IMM32_ONLY` | 1 | $\text{push}(PC + 8); \ PC = \text{imm32}$ | Direct subroutine call. |
| **`RET`** | `0x32` | `NONE` | 0 | $PC = \text{pop}()$ | Subroutine return. |
| **`JMPR`** | `0x33` | `RS1_ONLY` | 1 | $PC = R_{s1}$ | Register-indirect jump. |
| **`CALLR`**| `0x34` | `RS1_ONLY` | 1 | $\text{push}(PC + 8); \ PC = R_{s1}$ | Register-indirect subroutine call. |

---

### Category 4: Stack Operations (`0x40` – `0x4F`)
| Opcode | Hex | Mapping | Arity | Operation | Description |
| :--- | :---: | :--- | :---: | :--- | :--- |
| **`PUSH`** | `0x40` | `RS1_ONLY` | 1 | $SP \mathrel{-}= 8; \ [\text{RAM at } SP] = R_{s1}$ | Push 64-bit register onto stack. |
| **`POP`** | `0x41` | `RD_ONLY` | 1 | $R_d = [\text{RAM at } SP]; \ SP \mathrel{+}= 8$ | Pop 64-bit value from stack into $R_d$. |

---

### Category 5: Memory Operations (`0x50` – `0x5F`)
| Opcode | Hex | Mapping | Arity | Width | Operation |
| :--- | :---: | :--- | :---: | :---: | :--- |
| **`LOADB`** | `0x50` | `RD_IMM32` | 2 | 8-bit | $R_d = \text{RAM}[\text{imm32}]$ (1 byte, zero-extended) |
| **`LOADH`** | `0x51` | `RD_IMM32` | 2 | 16-bit | $R_d = \text{RAM}[\text{imm32}]$ (2 bytes, zero-extended) |
| **`LOADI`** | `0x52` | `RD_IMM32` | 2 | 32-bit | $R_d = \text{RAM}[\text{imm32}]$ (4 bytes, zero-extended) |
| **`LOADW`** | `0x53` | `RD_IMM32` | 2 | 64-bit | **$R_d = \text{RAM}[\text{imm32}]$ (8 bytes / full native Word)** |
| **`STOREB`**| `0x54` | `IMM32_RD` | 2 | 8-bit | $\text{RAM}[\text{imm32}] = R_d[7:0]$ |
| **`STOREH`**| `0x55` | `IMM32_RD` | 2 | 16-bit | $\text{RAM}[\text{imm32}] = R_d[15:0]$ |
| **`STOREI`**| `0x56` | `IMM32_RD` | 2 | 32-bit | $\text{RAM}[\text{imm32}] = R_d[31:0]$ |
| **`STOREW`**| `0x57` | `IMM32_RD` | 2 | 64-bit | **$\text{RAM}[\text{imm32}] = R_d[63:0]$ (8 bytes)** |
| **`LOADR`** | `0x58` | `RD_RS1_OFFSET32` | 3 | 64-bit | $R_d = \text{RAM}[R_{s1} + \text{imm32}]$ |
| **`STORER`**| `0x59` | `RD_RS1_OFFSET32` | 3 | 64-bit | $\text{RAM}[R_d + \text{imm32}] = R_{s1}$ |

---

## 5. Architectural Invariants

1. **Sequential Stride**: All non-branching instructions increment $PC$ by $+8$ (`Architecture.INSTRUCTION_BYTES`).
2. **Zero-Allocation**: No `Bit`, `Word`, or `Address` objects may be allocated on the heap during the fetch-decode-execute cycle.
3. **Bit Ownership**: `FixedWidthBits` containers maintain exclusive ownership over their internal `Bit` arrays. Set/get operations perform deep copies.
4. **Conditional Execution**: Predicates are evaluated globally at Stage 0 of the CPU cycle. If the condition fails, $PC$ advances by $+8$ and execution returns immediately.
