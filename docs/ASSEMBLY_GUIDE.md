# TheComputer Assembly Language — Reference & Tutorial

**Status:** Describes *working, currently-implemented* behavior only. Every claim in
this document was checked against the source in this repository and, where
practical, against a real run of the assembler and CPU (see
[Validation methodology](#validation-methodology)). Where the code, the
[ISA specification](ISA_SPECIFICATION.md), and the bundled example programs
disagreed, that disagreement is called out explicitly instead of silently
picked between — see [Verified discrepancies](#9-verified-discrepancies-code-vs-spec-vs-examples).

**Audience:** written for someone comfortable with Java who is new to assembly
and CPU internals. Concepts are introduced from first principles; nothing here
assumes prior assembly experience.

**Related documents:**
- [`ISA_SPECIFICATION.md`](ISA_SPECIFICATION.md) — the authoritative bit-layout
  reference this guide is built on top of.
- [`LANGUAGE_SPECIFICATION.md`](LANGUAGE_SPECIFICATION.md) — the
  specification for Bavarian-C/TCLang, the project's planned higher-level
  language, which would compile down to the assembly described here. No
  lexer, parser, or compiler exists for it yet (`LEX-001`, `PAR-001`,
  `COMP-001` are all `TODO` in `docs/tasks.json`); its §6 calling convention
  is built on `CALL`/`RET`, which are now confirmed working — see
  [§9](#9-verified-discrepancies-code-vs-spec-vs-examples), points 2–3.
- [`../ROADMAP.md`](../ROADMAP.md) — the original project plan; superseded on
  bit widths by `ISA_SPECIFICATION.md` (see the discrepancies section).
- [`reviews/2026-09-28-cpu-call-ret-bug.md`](reviews/2026-09-28-cpu-call-ret-bug.md)
  — the `CALL`/`RET` CPU bug writeup referenced in §9: found, written up,
  and fixed by the Developer (commit `90bf884`) the same day.

---

## Table of contents

1. [What a `.asm` program is](#1-what-a-asm-program-is)
2. [The pipeline: source → machine code → execution](#2-the-pipeline-source--machine-code--execution)
3. [How to actually assemble, load, step, and run a program today](#3-how-to-actually-assemble-load-step-and-run-a-program-today)
4. [Architecture, in the terms a programmer needs](#4-architecture-in-the-terms-a-programmer-needs)
5. [Syntax reference](#5-syntax-reference)
6. [Complete instruction reference](#6-complete-instruction-reference)
7. [Worked examples](#7-worked-examples)
8. [Discrepancies found in the bundled examples](#8-discrepancies-found-in-the-bundled-examples)
9. [Verified discrepancies: code vs. spec vs. examples](#9-verified-discrepancies-code-vs-spec-vs-examples)
10. [Current limitations, plainly stated](#10-current-limitations-plainly-stated)
11. [Validation methodology](#validation-methodology)

---

## 1. What a `.asm` program is

A `.asm` file is a plain-text program written in this project's own assembly
language — one line is (usually) one CPU instruction, written with a
human-readable mnemonic (`ADD`, `MOVI`, `JMP`, …) instead of raw bits. It is
*not* x86 or ARM assembly; the instruction set is custom and defined entirely
by this repository (see [`OpCode.java`](../src/main/java/de/happybavarian07/computer/isa/OpCode.java)).

Five example programs ship in
[`src/main/resources/programs/`](../src/main/resources/programs/):
`math-demo.asm`, `sum-loop-demo.asm`, `loop-demo.asm`,
`ram-multiplication-demo.asm`, and `stack-demo.asm`. All five now assemble
and run correctly (three of them needed fixes — see
[§8](#8-discrepancies-found-in-the-bundled-examples)).

## 2. The pipeline: source → machine code → execution

```
 .asm source
      │
      ▼
 ┌─────────┐   tokens    ┌────────┐   AST (Program/   ┌───────────────┐
 │  Lexer  │ ──────────▶ │ Parser │   Statement tree)  │ SymbolResolver│
 │IndexedLexer│          │DefaultParser│ ─────────────▶│ (2-pass:      │
 └─────────┘             └────────┘                    │ labels, then  │
                                                         │ operands)     │
                                                              │
                                                              ▼
                                                     ┌──────────────────┐
                                                     │ AssemblerEncoder │
                                                     │ (instructions →  │
                                                     │  64-bit words,   │
                                                     │  directives →    │
                                                     │  packed bytes)   │
                                                     └──────────────────┘
                                                              │
                                                              ▼
                                                    machine code (64-bit
                                                    words at byte addresses)
                                                              │
                                                              ▼
                                              ┌──────────────────────────┐
                                              │ Cpu.step() / Cpu.run()   │
                                              │ fetch → decode → check   │
                                              │ condition → execute →    │
                                              │ writeback → PC += 8      │
                                              └──────────────────────────┘
```

This mirrors [`ROADMAP.md`](../ROADMAP.md)'s "Phase 15 — Assembler" pipeline
(`Source → Lexer → Parser → Symbol Resolution → Encoding → Machine Code`) and
is implemented under
[`src/main/java/de/happybavarian07/computer/assembler/`](../src/main/java/de/happybavarian07/computer/assembler/):

| Stage | Class |
| :--- | :--- |
| Lexer | [`lexer/impl/IndexedLexer.java`](../src/main/java/de/happybavarian07/computer/assembler/lexer/impl/IndexedLexer.java) |
| Parser | [`parser/DefaultParser.java`](../src/main/java/de/happybavarian07/computer/assembler/parser/DefaultParser.java) |
| Symbol resolution | [`resolver/SymbolResolver.java`](../src/main/java/de/happybavarian07/computer/assembler/resolver/SymbolResolver.java) |
| Encoding | [`encoder/AssemblerEncoder.java`](../src/main/java/de/happybavarian07/computer/assembler/encoder/AssemblerEncoder.java), [`encoder/InstructionWordEncoder.java`](../src/main/java/de/happybavarian07/computer/assembler/encoder/InstructionWordEncoder.java) |
| Execution | [`cpu/Cpu.java`](../src/main/java/de/happybavarian07/computer/cpu/Cpu.java) |

A disassembler exists (ticket `DIS-001`, status `DONE` —
[`disassembler/Disassembler.java`](../src/main/java/de/happybavarian07/computer/disassembler/Disassembler.java),
with round-trip verification per its own ticket's acceptance criteria), but
there is still **no standalone debugger** (ticket `DBG-001`, status `TODO`,
was blocked on `DIS-001`, now unblocked).

## 3. How to actually assemble, load, step, and run a program today

This section is deliberately literal about what exists, because the project
ships no packaged jar (see below).

### 3.1 What exists

- **`AssemblerService`** is the one assembler pipeline (lexer, parser,
  resolver, encoder) and returns the flat byte image of memory from address 0.
  **`AssemblerCli`** ([source](../src/main/java/de/happybavarian07/computer/assembler/cli/AssemblerCli.java))
  is the command line front end for it, with a real `public static void main`
  (flags: `-o/--output`, `-d/--dry-run`, `-v/--verbose`, `-O/--overwrite`,
  `-h/--help`). No packaged jar is configured in `pom.xml`, so run it with
  `java -cp target/classes de.happybavarian07.computer.assembler.cli.AssemblerCli program.asm`.
- **The GUI Workbench**
  ([`gui/WorkbenchFrame.java`](../src/main/java/de/happybavarian07/computer/gui/WorkbenchFrame.java),
  launched via `ComputerWorkbenchLauncher`) is the one ready-to-use tool.
  Every action goes through
  [`WorkbenchController`](../src/main/java/de/happybavarian07/computer/gui/controller/WorkbenchController.java),
  which owns a full `Motherboard` instance and runs the same
  pipeline described above in-process through `AssemblerService` (it does not
  shell out to `AssemblerCli`):
  - **Load example** — loads one of the bundled `.asm` files from
    `src/main/resources/programs/` into the editor.
  - **Load asm** / **Load bin** — open a source file into the editor, or a
    flat binary image straight into RAM.
  - **Save asm** / **Save bin** (`Ctrl+S` / `Ctrl+Shift+S`) — write the
    editor text as `.asm`, or assemble it and write the flat binary image
    without touching the running machine. `Save bin` refuses to write a
    file if the source doesn't assemble. The output is byte-identical to
    `AssemblerCli`'s because both use `AssemblerService`.
  - **Assemble & Load** — assembles the editor text and loads the result
    into the emulated 256 MiB RAM.
  - **Step** / **Step N** — calls `Motherboard.stepSystem()` (→
    `Cpu.step()`) once or N times.
  - **Run** / **Stop** — drives `Motherboard.stepSystem()` on a Swing
    `Timer` until you stop it or the CPU halts.
  - **Reset** — calls `Motherboard.reset()`.
  - **Instructions** — opens a searchable opcode and condition-code
    reference (effects, flags, examples, known gotchas).
- **Tests as usage examples.** `CpuTest`
  ([source](../src/test/java/de/happybavarian07/computer/cpu/CpuTest.java))
  and `AssemblerCliTest`
  ([source](../src/test/java/de/happybavarian07/computer/assembler/cli/AssemblerCliTest.java))
  show the minimal Java needed to assemble and run a program without the GUI.

### 3.2 Assembling and running a program without the GUI

If you want a headless run (e.g. from a test, or your own throwaway `main`),
use the shared facade `AssemblerService` (the same one the GUI and the
command line tool use). It returns the flat byte image of memory from address
0: instructions are 8 little-endian bytes, data directives are byte-precise,
gaps are zero.

```java
byte[] image = new AssemblerService().assemble(sourceText, "program.asm").image();

Motherboard motherboard = new Motherboard();   // owns a full 256 MiB RAM + Cpu
motherboard.powerOn();

Word wordBuf = new Word();
Address addrBuf = new Address();
for (int offset = 0; offset < image.length; offset += 8) {
    long raw = 0;
    for (int i = 0; i < 8 && offset + i < image.length; i++) {
        raw |= ((long) (image[offset + i] & 0xFF)) << (i * 8);
    }
    addrBuf.set(offset);
    wordBuf.set(raw);
    motherboard.getSystemBus().writeWord(addrBuf, wordBuf);
}

motherboard.stepSystem();   // one instruction
// ...or:
motherboard.runSystem();    // run to HALT (or to a fault, see Cpu.getFaultReason())
```

From a shell, `AssemblerCli` now has a real `main`:
`java -cp target/classes de.happybavarian07.computer.assembler.cli.AssemblerCli program.asm -o out.bin`
writes exactly the same image bytes the GUI's "Save bin" writes.

**Heap note:** earlier revisions eagerly allocated the whole address space
as individual bit-level objects, so `new Motherboard()` could throw
`OutOfMemoryError` on a default-heap JVM. That no longer applies on this
branch (`6a033ae` reworked RAM storage): `new Motherboard()` with the full
256 MiB map constructs in a few hundred milliseconds on a default heap. A
throwaway harness can still register a smaller `RamBusDevice` on a
`SystemBus` if it only touches a few address ranges (see `CpuTest`).

## 4. Architecture, in the terms a programmer needs

All of the following is confirmed directly against
[`Architecture.java`](../src/main/java/de/happybavarian07/computer/util/Architecture.java)
and matches `ISA_SPECIFICATION.md`'s header claims:

| Property | Value | Source |
| :--- | :--- | :--- |
| Word size (register / ALU width) | **64-bit** | `Architecture.WORD_BITS` |
| Address width | **32-bit** | `Architecture.ADDRESS_BITS` |
| Byte | 8-bit | `Architecture.BYTE_BITS` |
| Instruction size | **fixed 64-bit (8 bytes)**, little-endian | `Architecture.INSTRUCTION_BYTES = 8` |
| General-purpose registers | **32**, named `r0`–`r31` | `Architecture.GPR_COUNT = 32` |
| Total memory | **256 MiB** (0x0000000–0x0FFFFFFF) | `Architecture.MEMORY_SIZE_BYTES` |
| Reserved region | top 4 KiB (`MEMORY_FREE_END`..`MEMORY_SIZE_BYTES-1`) | nominally for BIOS/MMIO |
| Stack | grows **downward** from `STACK_BASE_ADDRESS` (`MEMORY_FREE_END - 8`), hard floor at `STACK_LIMIT_ADDRESS` (128 MiB, i.e. `MEMORY_SIZE_BYTES / 2`) | `Cpu.push`/`Cpu.pop` |
| Flags | `Z` (zero), `N` (negative), `C` (carry), `V` (overflow) | `SpecialRegisters` |

Every instruction word is a fixed 8 bytes, laid out as (from
`ISA_SPECIFICATION.md`, cross-checked against `InstructionDecoder.java` and
`InstructionWordEncoder.java`):

```
 63      56 55  52 51    46 45    40 39    34 33 32 31                             0
+----------+------+--------+--------+--------+-----+--------------------------------+
|  OPCODE  | COND |   Rd   |  Rs1   |  Rs2   | RSV |             IMM32              |
|  (8-bit) |(4-bit)| (6-bit)| (6-bit)| (6-bit)|(2b) |            (32-bit)            |
+----------+------+--------+--------+--------+-----+--------------------------------+
```

There is **no MMU and no memory-mapped I/O device registered today** —
`MEM-002` (guard pages / segfault traps) and `IO-001` (memory-mapped I/O
ports) are both `TODO` in `docs/tasks.json`. The "reserved top 4 KiB" is
simply unmapped: reading or writing it raises `BusFaultException`, the same
as any other address nothing is registered at.

### Conditional execution

Every instruction — not just branches — carries a 4-bit condition (default
`AL`, "always"). Before executing, the CPU tests the condition against the
current flags; if it's false, the instruction is skipped (treated like a
`NOP`) and `PC` still advances by 8. This is how conditional jumps work: a
conditional jump is just `JMP` with a non-`AL` condition attached — there is
no separate `JZ`/`JNZ`/`JGT` opcode. See
[§5.4](#54-condition-suffixes-how-conditional-jumps-are-actually-spelled).

## 5. Syntax reference

All statements below were exercised directly against the current
`IndexedLexer` / `DefaultParser` / `SymbolResolver` / `InstructionWordEncoder`
(see [Validation methodology](#validation-methodology)).

### 5.1 Labels

```asm
loop:
    add r2, r2, r1
    sub r1, r1, r3
    jmpne loop
```

A label is an identifier immediately followed by `:` with no space, on its
own logical position in the token stream (it can share a line with the next
instruction or sit on its own line — the bundled examples use the latter
style). It defines a symbol equal to the byte address of the *next*
statement. Labels can be used as operands anywhere a 32-bit immediate/address
is expected (jump/call targets, `LOAD*`/`STORE*` addresses, `.word`/`.byte`
values); the resolver substitutes the label's numeric address at assembly
time. Duplicate label names in one file are a resolution error.

### 5.2 Registers

- `r0` – `r31` (case-insensitive: `R0`, `r0` both work) — the 32
  general-purpose registers, each 64 bits wide.
- `sp` is the stack pointer and can be used as an operand of `mov`, `addi`
  and `subi` only (`mov r30, sp`, `mov sp, r30`, `subi sp, sp, 32`).
  Anywhere else it is an assembler error.
- `pc`, `ir` and `flags` are lexically recognized as register-like tokens but
  are **not operands**; using one gives a clear diagnostic (`'pc' cannot be
  used as an operand`). They are only touched implicitly (by
  `JMP`/`CALL`/`RET`/`PUSH`/`POP`/etc.).
- `r0` is an ordinary register, not hardwired to zero.
- There is no `r32` or higher: `Architecture.GPR_COUNT - 1 = 31` is enforced
  by the encoder (`register out of '0..31'`).

### 5.3 Numbers

```asm
movi r1, 42        ; decimal
movi r1, 0x2A       ; hexadecimal
movi r1, 0b101010   ; binary
movi r1, 1_000_000  ; underscores allowed as digit separators
```

Decimal, `0x`/`0X` hex, and `0b`/`0B` binary literals are supported, and a
literal may start with a minus sign directly in front of the digits
(`-5`, `-0x10`). Immediate ranges depend on the opcode:

| Kind | Opcodes | Range |
| :--- | :--- | :--- |
| signed 32-bit (sign-extended) | `movi`, `addi`, `subi`, `cmpi`, `loadr`/`storer` offset | `-2147483648 .. 2147483647` |
| unsigned 32-bit (zero-extended) | `andi`, `ori`, `xori` | `0 .. 4294967295` |
| shift amount | `shli`, `shri` | `0 .. 63` |
| address | `jmp`, `call`, absolute `load*`/`store*` | `0 .. 4294967295` |

Out-of-range values are assembler errors, e.g. `movi r1, 0xFFFFFFFF` (does
not fit signed 32-bit; build large constants with `movi`, `shli`, `ori`).

### 5.4 Condition suffixes (how conditional jumps are actually spelled)

A mnemonic is either a bare opcode name (implicit `AL`, always-execute) or an
opcode name with a **condition suffix** appended directly, no separator:
`OPCODE` + `CONDITION`. The parser
([`DefaultParser.splitMnemonic`](../src/main/java/de/happybavarian07/computer/assembler/parser/DefaultParser.java))
strips a known `Condition` enum name or alias off the *end* of the mnemonic
and checks whether what's left is a real opcode name. This is why `jnz` does
**not** work (see [§8](#8-discrepancies-found-in-the-bundled-examples)) —
stripping `NZ` from `JNZ` leaves `J`, and there is no opcode named `J`, only
`JMP`.

| Condition | Aliases | Meaning | Example suffixed mnemonic |
| :---: | :--- | :--- | :--- |
| `AL` | `ALWAYS` | always (default, no suffix needed) | `ADD` |
| `EQ` | `Z` | zero / equal | `JMPEQ` |
| `NE` | `NZ` | not zero / not equal | `JMPNE`, `JMPNZ` |
| `CS` | `HS` | carry set / unsigned ≥ | `JMPCS`, `JMPHS` |
| `CC` | `LO` | carry clear / unsigned < | `JMPCC`, `JMPLO` |
| `MI` | `NEG` | negative | `JMPMI` |
| `PL` | `POS` | positive or zero | `JMPPL` |
| `VS` | | overflow set | `JMPVS` |
| `VC` | | overflow clear | `JMPVC` |
| `HI` | | unsigned > | `JMPHI` |
| `LS` | | unsigned ≤ | `JMPLS` |
| `GE` | | signed ≥ | `JMPGE` |
| `LT` | | signed < | `JMPLT` |
| `GT` | | signed > | `JMPGT` |
| `LE` | | signed ≤ | `JMPLE` |
| `NV` | `NEVER` | never (effectively a NOP) | `JMPNV` |

Any opcode can take a condition suffix, not just `JMP` — e.g. `ADDEQ r3, r1,
r2` (used directly in `CpuTest`) only executes if `Z` is set. `JMPNE`,
`JMPNZ`, `JMPLE`, and `JMPGT` were all confirmed to assemble and execute
correctly for this document (see [§7](#7-worked-examples)).

### 5.5 Operand order

Operand order is fixed per opcode by its `OperandMapping`
([source](../src/main/java/de/happybavarian07/computer/assembler/encoder/model/OperandMapping.java)):

| Mapping | Operands (in source order) | Example |
| :--- | :--- | :--- |
| `NONE` | *(none)* | `nop`, `halt`, `ret` |
| `RD_RS1_RS2` | `rd, rs1, rs2` | `add r3, r1, r2` |
| `RD_RS1_IMM32` | `rd, rs1, imm` | `addi r1, r1, 100` |
| `RD_RS1` | `rd, rs1` | `mov r1, r2`, `cmp r1, r2` |
| `RD_IMM32` | `rd, imm` | `movi r1, 50`, `loadw r1, 0x1000` |
| `IMM32_RD` | `imm, rd` | `storew 0x1000, r1` |
| `RD_RS1_OFFSET32` | `rd, rs1, offset` | `loadr r1, r2, 0` |
| `IMM32_ONLY` | `imm` | `jmp 0x2000` |
| `RS1_ONLY` | `rs1` | `push r1`, `jmpr r1` |
| `RD_ONLY` | `rd` | `pop r1` |

`CMP rd, rs1` computes `rd - rs1` and updates flags (it does **not** write a
result register) — the first operand is the *minuend*, matching the
"`RAM[Rd + imm32] = Rs1`"-style operand-role convention used elsewhere, i.e.
"first operand named" is not always "operand written to."

### 5.6 Comments

`;`, `#`, and `//` all start a line comment that runs to end of line.

### 5.7 Directives

Five directives are implemented in
[`SymbolResolver`](../src/main/java/de/happybavarian07/computer/assembler/resolver/SymbolResolver.java)
and [`DirectiveDataEmitter`](../src/main/java/de/happybavarian07/computer/assembler/encoder/DirectiveDataEmitter.java).

| Directive | Effect |
| :--- | :--- |
| `.org <addr>` | Sets the location counter to `<addr>` for subsequent statements. |
| `.word <n1>, <n2>, ...` | Emits each value as a 4-byte little-endian word (`-2147483648 .. 4294967295`). No alignment requirement. |
| `.byte <n1>, <n2>, ...` | Emits each value as a single byte. No alignment requirement. |
| `.ascii "text"` | Emits the byte-encoded string (with `\n`, `\t`, `\\`, `\"`, `\'`, `\0` escapes), one byte per character, no implicit terminator. |
| `.align <n>` | Pads with zeros up to the next multiple of `<n>` (a power of two, 1..4096). |

Data directives are byte-precise. **Instructions must start at an address
divisible by 8**; otherwise resolution fails with `instruction at address N
is not 8-byte aligned; use .align 8 or .org before it`. After odd-sized data,
put `.align 8` before the next instruction. Two statements that write the same
byte are an error (`overlapping output at address N`).

**Validated, alignment-safe example:**

```asm
.org 0x100
data_block:
.word 100
.byte 1, 2, 3
.ascii "hi"
.align 8
halt
```

---

## 6. Complete instruction reference

Bit values below are read directly from
[`OpCode.java`](../src/main/java/de/happybavarian07/computer/isa/OpCode.java)
(ground truth) — where this disagrees with `ISA_SPECIFICATION.md`, it's noted
inline and also collected in [§9](#9-verified-discrepancies-code-vs-spec-vs-examples).
Every example instruction below assembles successfully today.

### 6.1 System & control

| Instr | Opcode | Operands | Effect | Example |
| :--- | :---: | :--- | :--- | :--- |
| `NOP` | `0x00` | *(none)* | No-op; `PC += 8`. | `nop` |
| `MOV` | `0x01` | `rd, rs1` | `rd = rs1` | `mov r1, r2` |
| `MOVI` | `0x02` | `rd, imm` | `rd = imm` (no memory access) | `movi r1, 50` |
| `HALT` | **`0x03`** | *(none)* | `isHalted = true`; `PC` does **not** advance further. | `halt` |

### 6.2 Arithmetic (all set `Z`, `N`; see per-row notes for `C`/`V`)

| Instr | Opcode | Operands | Effect | Flags | Example |
| :--- | :---: | :--- | :--- | :--- | :--- |
| `ADD` | `0x10` | `rd, rs1, rs2` | `rd = rs1 + rs2` | `Z N C V` | `add r3, r1, r2` |
| `ADDI` | `0x11` | `rd, rs1, imm` | `rd = rs1 + imm` (signed imm) | `Z N C V` | `addi r1, r1, -1` |
| `SUB` | `0x12` | `rd, rs1, rs2` | `rd = rs1 - rs2` | `Z N C V` | `sub r4, r1, r2` |
| `SUBI` | `0x13` | `rd, rs1, imm` | `rd = rs1 - imm` | `Z N C V` | `subi r4, r1, 8` |
| `MUL` | `0x14` | `rd, rs1, rs2` | `rd = rs1 * rs2` (two's complement) | `Z N V` (`V` = signed overflow) | `mul r5, r1, r2` |
| `DIV` | `0x15` | `rd, rs1, rs2` | `rd = rs1 / rs2` (signed, truncates toward zero) | `Z N`, clears `C`/`V` (`V` set for `MIN / -1`) | `div r6, r1, r2` |
| `MOD` | `0x16` | `rd, rs1, rs2` | `rd = rs1 % rs2` (signed, sign of the dividend) | `Z N`, clears `C`/`V` | `mod r7, r1, r2` |
| `CMP` | `0x17` | `rd, rs1` | flags on `rd - rs1` (no write) | `Z N C V` | `cmp r1, r2` |
| `CMPI` | `0x18` | `rd, imm` | flags on `rd - imm` (no write) | `Z N C V` | `cmpi r4, 0` |

> **Signed division and faults.** `-7 / 2 = -3`, `-7 % 2 = -1`,
> `7 / -2 = -3`, `7 % -2 = 1`. Dividing by zero does not throw out of
> `Cpu.step()`: the CPU halts on the faulting instruction (PC is not
> advanced) and `Cpu.getFaultReason()` says why (the GUI logs it).

### 6.3 Bitwise & shifts

| Instr | Opcode | Operands | Effect | Flags | Example |
| :--- | :---: | :--- | :--- | :--- | :--- |
| `AND` | `0x20` | `rd, rs1, rs2` | `rd = rs1 & rs2` | `Z N`, clears `C V` | `and r3, r1, r2` |
| `ANDI` | `0x21` | `rd, rs1, imm` | `rd = rs1 & imm` | `Z N`, clears `C V` | `andi r3, r1, 0xF` |
| `OR` | `0x22` | `rd, rs1, rs2` | `rd = rs1 \| rs2` | `Z N`, clears `C V` | `or r3, r1, r2` |
| `ORI` | `0x23` | `rd, rs1, imm` | `rd = rs1 \| imm` | `Z N`, clears `C V` | `ori r3, r1, 1` |
| `XOR` | `0x24` | `rd, rs1, rs2` | `rd = rs1 ⊕ rs2` | `Z N`, clears `C V` | `xor r3, r1, r2` |
| `XORI` | `0x25` | `rd, rs1, imm` | `rd = rs1 ⊕ imm` | `Z N`, clears `C V` | `xori r3, r1, 1` |
| `NOT` | `0x26` | `rd, rs1` | `rd = ~rs1` | `Z N`, clears `C V` | `not r2, r1` |
| `SHL` | `0x27` | `rd, rs1, rs2` | `rd = rs1 << rs2` | `Z N C`, clears `V` | `shl r2, r1, r3` |
| `SHLI` | `0x28` | `rd, rs1, imm` | `rd = rs1 << imm` | `Z N C`, clears `V` | `shli r2, r1, 2` |
| `SHR` | `0x29` | `rd, rs1, rs2` | `rd = rs1 >>> rs2` (logical) | `Z N C`, clears `V` | `shr r2, r1, r3` |
| `SHRI` | `0x2A` | `rd, rs1, imm` | `rd = rs1 >>> imm` (logical) | `Z N C`, clears `V` | `shri r2, r1, 2` |

Shift amounts are masked to `WORD_BITS - 1` (`& 63`), so shifting by ≥64 is
well-defined (equivalent to shifting by `amount % 64`), not undefined
behavior. Shifts are logical (unsigned), not arithmetic — `SHR` never
sign-extends.

### 6.4 Control flow & subroutines

| Instr | Opcode | Operands | Effect | Example |
| :--- | :---: | :--- | :--- | :--- |
| `JMP` | `0x30` | `imm` | `PC = imm` (combined with a condition suffix for conditional jumps) | `jmp loop`, `jmpne loop` |
| `CALL` | `0x31` | `imm` | pushes `PC + 8`, then `PC = imm` | `call subroutine` |
| `RET` | `0x32` | *(none)* | `PC = pop()` | `ret` |
| `JMPR` | `0x33` | `rs1` | `PC = rs1` | `jmpr r9` |
| `CALLR` | `0x34` | `rs1` | pushes `PC + 8`, then `PC = rs1` | `callr r9` |

`CALL` and `RET` were confirmed broken as of the ISA-002 migration (`CALL`
jumped to the wrong address, `RET` crashed) and were fixed and re-verified
on 2026-09-28, commit `90bf884` ("fix(cpu): fix call and jmpr") — see
[§9](#9-verified-discrepancies-code-vs-spec-vs-examples), points 2–3, and
[`reviews/2026-09-28-cpu-call-ret-bug.md`](reviews/2026-09-28-cpu-call-ret-bug.md)
for the full history. Both are confirmed working now, per
[§7.4](#74-subroutines).

### 6.5 Stack

| Instr | Opcode | Operands | Effect | Example |
| :--- | :---: | :--- | :--- | :--- |
| `PUSH` | `0x40` | `rs1` | `SP -= 8; RAM[SP] = rs1` (64-bit) | `push r1` |
| `POP` | `0x41` | `rd` | `rd = RAM[SP]; SP += 8` | `pop r2` |

Both raise `StackOverflowException` — the *same* exception class is used for
"pushed past the 128 MiB stack floor" and "popped past the reset stack top,"
i.e. both stack-overflow and stack-underflow conditions.

### 6.6 Memory

| Instr | Opcode | Operands | Width | Effect | Example |
| :--- | :---: | :--- | :---: | :--- | :--- |
| `LOADB` | `0x50` | `rd, imm` | 8-bit | `rd = zero_extend(RAM[imm])` | `loadb r1, 0x100` |
| `LOADH` | `0x51` | `rd, imm` | 16-bit | `rd = zero_extend(RAM[imm])` | `loadh r1, 0x100` |
| `LOADI` | `0x52` | `rd, imm` | 32-bit | `rd = zero_extend(RAM[imm])` | `loadi r1, 0x100` |
| `LOADW` | `0x53` | `rd, imm` | 64-bit | `rd = RAM[imm]` (full word) | `loadw r1, 0x100` |
| `STOREB` | `0x54` | `imm, rd` | 8-bit | `RAM[imm] = rd[7:0]` | `storeb 0x100, r1` |
| `STOREH` | `0x55` | `imm, rd` | 16-bit | `RAM[imm] = rd[15:0]` | `storeh 0x100, r1` |
| `STOREI` | `0x56` | `imm, rd` | 32-bit | `RAM[imm] = rd[31:0]` | `storei 0x100, r1` |
| `STOREW` | `0x57` | `imm, rd` | 64-bit | `RAM[imm] = rd[63:0]` | `storew 0x100, r1` |
| `LOADR` | `0x58` | `rd, rs1, offset` | 64-bit | `rd = RAM[rs1 + offset]` | `loadr r1, r2, 0` |
| `STORER` | `0x59` | `rd, rs1, offset` | 64-bit | `RAM[rs1 + offset] = rd` — see note below | `storer r1, r2, 0` |

> `STORER` writes `RAM[rs1 + offset] = rd` (first operand is the value, second the base register, same role order as `LOADR`); `ISA_SPECIFICATION.md` now says the same.

There is no dedicated `STORE` (bare, no width suffix) — see
[§8](#8-discrepancies-found-in-the-bundled-examples).

## 7. Worked examples

All of the following were assembled and executed against the real pipeline
described in [§3.2](#32-assembling-and-running-a-program-without-the-gui);
register/memory values shown are the actual observed results, not predicted
ones.

### 7.1 Arithmetic — `math-demo.asm`

[`src/main/resources/programs/math-demo.asm`](../src/main/resources/programs/math-demo.asm)
needed **no changes** — it assembles and runs correctly as shipped:

```asm
movi r1, 42
movi r2, 8
add r3, r1, r2       ; r3 = r1 + r2 = 50
sub r4, r1, r2       ; r4 = r1 - r2 = 34
mul r5, r1, r2       ; r5 = r1 * r2 = 336
div r6, r1, r2       ; r6 = r1 / r2 = 5
mod r7, r1, r2       ; r7 = r1 % r2 = 2
halt
```

Verified result after `run()`: `r3=50, r4=34, r5=336, r6=5, r7=2` — exactly
matching the inline comments.

### 7.2 A terminating loop — `sum-loop-demo.asm`

[`src/main/resources/programs/sum-loop-demo.asm`](../src/main/resources/programs/sum-loop-demo.asm)
(fixed — see [§8](#8-discrepancies-found-in-the-bundled-examples)):

```asm
movi r1, 5           ; loop counter
movi r2, 0           ; accumulator
movi r3, 1           ; step

loop:
add r2, r2, r1       ; accumulator = accumulator + counter
sub r1, r1, r3       ; counter = counter - 1
jmpne loop

storew 256, r2       ; store result (15) to RAM address 256
halt
```

This is a **post-test** loop (do the work, then test): the body always runs
at least once, and `SUB` sets the flags that the following `JMPNE` reads —
no separate `CMP` is needed because `SUB`'s own flags already reflect
`r1 == 0`. Verified: `r2 = 15` (1+2+3+4+5) and `RAM[256] = 15` after
`halt`.

### 7.3 Memory access — `ram-multiplication-demo.asm`

[`src/main/resources/programs/ram-multiplication-demo.asm`](../src/main/resources/programs/ram-multiplication-demo.asm)
(fixed — see [§8](#8-discrepancies-found-in-the-bundled-examples)) stores
eight factors to RAM and reads them back through `LOADR` (register-indirect
load):

```asm
movi r1, 3
storew 2048, r1        ; factor 1 = 3
movi r1, 4
storew 2056, r1        ; factor 2 = 4
...

movi r7, 2048
movi r8, 2056
call multiply_pair
storew 4096, r3         ; 3 * 4 = 12
...
```

(The `call multiply_pair` / subroutine part is covered in
[§7.4](#74-subroutines); this section is about the `LOADR`/`STOREW`
mechanics, which work the same with or without the call.)

Verified: `RAM[4096..4120] = 12, 30, 56, 18` — i.e. `3*4`, `5*6`, `7*8`,
`9*2`, exactly as intended.

### 7.4 Subroutines

The ISA has `CALL`/`CALLR`/`RET` (Category 3). **As of 2026-09-28, commit
`90bf884`, these work correctly** — they did not before that (see
[§9](#9-verified-discrepancies-code-vs-spec-vs-examples), points 2–3, and
[`reviews/2026-09-28-cpu-call-ret-bug.md`](reviews/2026-09-28-cpu-call-ret-bug.md)
for the bug this section used to describe and how it was fixed). The
walkthrough below reflects current, verified behavior.

`ram-multiplication-demo.asm` (§7.3) factors its four repeated "load two
factors, multiply" blocks into a shared subroutine:

```asm
movi r7, 2048
movi r8, 2056
call multiply_pair
storew 4096, r3         ; 3 * 4 = 12
...
halt

; --- subroutine: multiply_pair ---
; in: r7 = &A, r8 = &B ; out: r3 = A * B ; clobbers: r1, r2
multiply_pair:
loadr r1, r7, 0
loadr r2, r8, 0
mul r3, r1, r2
ret
```

Note the calling convention here (arguments in `r7`/`r8`, result in `r3`,
`r1`/`r2` clobbered, caller stores the result) is something this example
**invented for itself** — the ISA and assembler don't define one, so any
program using `CALL`/`RET` has to pick its own register roles and document
them, the same way this one does in its header comment. (`docs/LANGUAGE_SPECIFICATION.md`
§6 defines a fuller, more formal ABI for its planned higher-level language —
worth reading if you need something more structured than "pick some
registers and write it down.")

Verified end-to-end: `PC` jumps from each `call multiply_pair` directly to
the subroutine's real address (not into stack memory), `RET` returns to the
instruction immediately after each call, `SP` is back at its starting value
after all four call/return round trips, and the final register/memory state
matches §7.3's verified result (`r3 = 18` after the last call, `RAM[4096..4120]
= 12, 30, 56, 18`).

### 7.5 Stack — `stack-demo.asm`

[`src/main/resources/programs/stack-demo.asm`](../src/main/resources/programs/stack-demo.asm)
needed **no changes**:

```asm
movi r1, 123456
push r1
movi r1, 0           ; clear r1
pop r2              ; r2 gets 123456
halt
```

Verified: `r2 = 123456` after `halt`, and `SP` is restored to its reset
value (the push/pop pair is balanced).

## 8. Discrepancies found in the bundled examples

Three of the five example programs used syntax that does not parse under the
current assembler, or addresses that collide with their own code. All were
fixed in place (with an explanatory header comment in each file) rather than
left broken, since `.asm` example programs are documentation content, not
application source, and the task this document was written for explicitly
asked for corrections. Each fix was verified end-to-end.

| File | Problem | Confirmed error | Fix |
| :--- | :--- | :--- | :--- |
| `loop-demo.asm` | `jnz inner` / `jnz outer` — `JNZ` is not a valid mnemonic (stripping `NZ` off `JNZ` per [§5.4](#54-condition-suffixes-how-conditional-jumps-are-actually-spelled) leaves `J`, not `JMP`) | `ParserException: unknown opcode 'jnz'` | `jmpne` |
| `loop-demo.asm` | `store 0x20, r2` — no bare `STORE` opcode exists, only width-suffixed forms | `ParserException: unknown opcode 'store'` | `storew 0x20, r2` |
| `loop-demo.asm` | Result stored at `0x20` (32), which falls **inside** the program's own 88-byte code region under the current 8-byte-instruction ISA (this program predates the 64-bit migration's instruction-size change) | *(ran, but silently overwrote an already-executed instruction word — a latent self-modifying-code bug, not a crash)* | moved to `0x200` |
| `sum-loop-demo.asm` | Same `jnz loop` / `store 256, r2` issues as above | same as above | `jmpne` / `storew` |
| `ram-multiplication-demo.asm` | Same `store`→width-suffix issue (8 occurrences) | `ParserException: unknown opcode 'store'` | `storew` |
| `ram-multiplication-demo.asm` | `loadr r1, r7` — `LOADR` requires 3 operands (`rd, rs1, offset`), not 2 | `ParserException: expected comma between operands but found NEWLINE` | `loadr r1, r7, 0` |
| `ram-multiplication-demo.asm` | Data stored at `256`–`319`, which falls **inside** the program's own 328-byte code region (41 instructions × 8 bytes) under the current ISA — same root cause as the `loop-demo.asm` case, but here it actually corrupted execution (the program silently overwrote its own not-yet-executed instructions, producing garbled final register values instead of the documented products) | *(no exception — silently wrong results: observed `r1=7, r2=8, r3=30` instead of the expected final-block values `r1=9, r2=2, r3=18`)* | moved factor storage to `2048..`, results to `4096..` |

`math-demo.asm` and `stack-demo.asm` needed no changes.

**Root cause, for all three broken files:** they were written under the
project's original 32-bit ISA (`ROADMAP.md`'s "Initial architecture (v1)":
32-bit words, 4-byte instructions) and never re-validated after `ISA-002`
migrated the CPU to native 64-bit words with fixed 8-byte instructions —
`store`/`jnz` are pre-migration mnemonics that no longer exist, `LOADR`
gained an explicit offset operand it didn't previously need, and "safe"
low data addresses chosen when instructions were 4 bytes stopped being safe
once instructions doubled to 8 bytes each.

## 9. Verified discrepancies: code vs. spec vs. examples

Everything in this section was independently confirmed by reading the
relevant source *and* exercising it — not inferred from one side alone.

Items fixed by `ASM-003` are marked *fixed*; the reproductions are kept
short for history.

1. **`HALT`'s opcode value** — *fixed (spec).* `OpCode.HALT` is `0x03`;
   `ISA_SPECIFICATION.md` now says `0x03` too.

2. **`CALL` jumped to the wrong address — fixed 2026-09-28, commit
   `90bf884`.** Root cause: `Cpu.push(Word value)` reused the CPU's shared
   `workingAddress` scratch field to compute the stack write address as a
   side effect; `CALL`'s handler set `workingAddress` to the jump target
   during the read stage, then called `push(scratchReturnAddrWord)` (which
   overwrote `workingAddress` with the *stack* address), and only *then* did
   `specialRegisters.getPC().set(workingAddress)` — by which point
   `workingAddress` no longer held the jump target. Traced directly at the
   time: a `call subroutine` instruction resulted in the *next* fetch
   happening at the just-computed stack address instead of the subroutine's
   real address. `JMP` was unaffected (no `push()` call intervenes).
   `CALLR` was unaffected (it reads its target from `regSrc1Value`, a
   different field, never touched by `push()`). **Fix:** a dedicated
   `branchTargetAddress` scratch field that `CALL` copies the jump target
   into *before* calling `push()`, then reads back afterward — see
   [`reviews/2026-09-28-cpu-call-ret-bug.md`](reviews/2026-09-28-cpu-call-ret-bug.md)
   for the full diff and re-verification.

3. **`RET` crashed unconditionally — fixed in the same commit.**
   `Cpu.java`'s `RET` case assigned a 64-bit `scratchReturnAddrWord` (`Word`)
   directly into `specialRegisters.getPC()` (a 32-bit `Address`);
   `FixedWidthBits.set(FixedWidthBits)` asserts matching widths, so this
   threw `IllegalArgumentException: Expected 32 bits, got 64` every single
   time `RET` executed — confirmed at the time by reaching `RET` via the one
   path that *did* jump correctly (`CALLR`) and observing the crash there
   too, independent of bug 2. **Fix:** narrow the value via
   `set(scratchReturnAddrWord.getAsLong() & 0xFFFFFFFFL)` instead — the same
   pattern `JMPR`/`CALLR` already used.

   Both were re-verified end-to-end after the fix (direct `CALL` and
   `CALLR`, each paired with `RET`, both reaching the subroutine and
   returning correctly) — see
   [`reviews/2026-09-28-cpu-call-ret-bug.md`](reviews/2026-09-28-cpu-call-ret-bug.md)
   for the full history and verification, and [§7.4](#74-subroutines) for a
   working example. This is kept here, marked fixed rather than deleted,
   because it's a real example of the review-writeup-not-direct-edit
   workflow this repo uses actually closing the loop: found, written up,
   and fixed by the Developer from the writeup alone.

   `docs/LANGUAGE_SPECIFICATION.md` §6 ("64-Bit ABI Calling Convention")
   specifies a full prologue/epilogue convention built on `push`/`pop`/
   `ret` — that `ret` now works at the instruction level; this doesn't
   verify the ABI design itself, only that the instructions it's built on
   no longer crash.

4. **The 32-bit `IMM32` field was effectively a positive 28-bit range, and the
   decoder threw on a set top bit** — *fixed.* `InstructionDecoder` now keeps
   the 32 bits as they are, and every opcode has an `ImmediateKind`
   (signed 32, unsigned 32, shift amount, address) shared by the assembler
   range check, the CPU and the disassembler; see
   [§5.3](#53-numbers) for the ranges. `movi r1, 0xFFFFFFFF` is now an error.

5. **`STORER`'s documented formula was reversed from its actual behavior** —
   *fixed (spec).* `ISA_SPECIFICATION.md` now says
   `RAM[Rs1 + imm32] = Rd`, matching the code.

6. **`pc`/`sp`/`ir`/`flags` as operands raised `NullPointerException`** —
   *fixed.* `sp` is now an operand of `mov`/`addi`/`subi`; the others give a
   clean diagnostic.

7. **Directive alignment was checked inconsistently and `AssemblerCli`
   dropped unaligned data** — *fixed.* Data is byte-precise, instructions
   must be 8-byte aligned, `.align` was added, and `AssemblerCli` and the
   GUI share one pipeline (`AssemblerService`).

8. **`DIV`/`MOD` were unsigned, `MUL`'s `V` was unsigned overflow, and a zero
   divisor threw out of `Cpu.step()`** — *fixed.* See
   [§6.2](#62-arithmetic-all-set-z-n-see-per-row-notes-for-cv).

9. **Popping from an empty stack raised `StackOverflowException`** —
   *fixed.* It raises `StackUnderflowException`, and the empty check itself
   was off by one (it allowed one pop at the reset SP).

## 10. Current limitations, plainly stated

- **No standalone debugger** (`DBG-001`, `TODO`) — the disassembler it was
  blocked on now exists (`DIS-001`, `DONE`).
- **No memory-mapped I/O** (`IO-001`, `TODO`) and **no MMU / guard pages**
  (`MEM-002`, `TODO`) — the "reserved" top 4 KiB is just unmapped memory.
- **No caching**, **no multicore** — `CACHE-001` and `MC-001` are both
  `TODO`.
## Validation methodology

Every claim in this document that could be checked mechanically, was.
Concretely: the project was compiled (`mvn compile`), and a series of small
Java harnesses were written against the compiled `Lexer` → `Parser` →
`SymbolResolver` → `AssemblerEncoder` → `Cpu`/`SystemBus` classes (the exact
API shown in [§3.2](#32-assembling-and-running-a-program-without-the-gui))
to: (a) assemble every bundled `.asm` file as originally shipped and record
the exact parser/encoder exceptions raised; (b) assemble and run corrected
versions and confirm the resulting register/memory state matches each
program's own comments; (c) probe specific boundary behaviors called out
above (mnemonic decomposition, immediate range clamping, negative literals,
directive alignment, `PC`/`SP` as operands, `STORER`'s operand roles,
`CALL`/`RET`). Nothing marked "confirmed" or "verified" above is a guess from
reading code alone — each was reproduced by actually running it against this
repository's current state. Nothing marked as a limitation is based on
absence-of-evidence alone; each corresponds to a `TODO` ticket in
`docs/tasks.json` or an exception actually thrown.

This document tracks its own branch's state, not a snapshot frozen at
authoring time: the `CALL`/`RET` bugs described in §9 were found this way,
written up as a standalone review note rather than patched directly (per
this repo's review-only policy on `src/main/java`), fixed by the Developer
from that writeup the same day, and the fix was itself re-verified the same
way — by compiling the branch and re-running the reproduction programs
against the new code, not by reading the diff and assuming it worked.
