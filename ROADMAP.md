# Project: From Bits to a Computer

A computer system built completely from scratch in Java.

The goal is not to reproduce x86, ARM, or any existing architecture.

The goal is to understand and implement the full stack of a computer system — starting from individual bits and ending with a custom programming language and compiler.

---

# 0. Project Philosophy

## Core principles

* Build everything yourself where practical
* Do not hide fundamental concepts behind Java abstractions unless explicitly intended
* Keep each layer independent
* Define behavior before implementation
* Test every layer before building on top of it
* Prefer simplicity over feature completeness
* Avoid copying real CPU architectures unless necessary
* Document all design decisions as the system evolves

---

## The full stack

```text
Your Programming Language
        ↓
Compiler
        ↓
Intermediate Representation
        ↓
Assembler
        ↓
Assembly Language
        ↓
Instruction Set Architecture (ISA)
        ↓
CPU
        ↓
Control Unit / Registers / ALU
        ↓
Memory / Bus / I/O
        ↓
Word / Byte / Bit / Address
```

Each layer is built on top of the previous one.

---

# 1. Phase 0 — Architecture Specification

Before writing any CPU code, define the system.

## Initial architecture (v1)

```text
CPU word size:        32-bit
Address size:         16-bit
Memory size:          64 KiB
Registers:            16 general-purpose
Register width:       32-bit
Instruction format:    Variable (16/32-bit base)
Endianness:           Little-endian
Signed integers:      Two's complement
Overflow behavior:    Wraparound + flags
Memory model:         Byte-addressable
Stack direction:      Downward
I/O model:            Memory-mapped
Flags:                Z, N, C, V
```

---

## Key design idea

* **Word (32-bit)** = CPU data unit
* **Address (16-bit)** = memory location identifier
* **Byte (8-bit)** = smallest addressable unit

These are *different concepts*, even if they are all stored as bits.

---

## Important clarification: “Do we need 2 bits for a Bit?”

No.

A **Bit is not stored as “two bits”**.

A Bit is a **logical abstraction**, not a storage format.

Internally, you may represent it as:

* `boolean`
* `byte` (0 or 1)
* `int` (0 or 1)

But conceptually:

```text
Bit = {0, 1}
```

If you need to represent uncertainty or multiple states, that is a different system (e.g. ternary logic), not a Bit.

So:

* 1 Bit = 1 binary value
* It does NOT require 2 bits of storage

---

# 2. Phase 1 — Fundamental Data Types

## 2.1 Bit

A Bit represents a single binary value:

```text
0 or 1
```

### Supported operations

* NOT
* AND
* OR
* XOR

### Design rules

* Prefer immutability
* Only allow valid values (0 or 1)
* Invalid inputs should fail fast

---

## 2.2 Byte

A Byte contains:

```text
8 Bits
```

Used for:

* memory storage
* character encoding
* low-level data manipulation

---

## 2.3 Word

A Word is the CPU’s native data size:

```text
32 Bits
```

Used for:

* arithmetic
* registers
* ALU operations

---

## 2.4 Address

An Address represents a memory location:

```text
16 Bits → 0x0000 to 0xFFFF
```

Important distinction:

* `Word = value`
* `Address = location`

Even if both are stored as numbers, they are not interchangeable.

---

# 3. Phase 2 — Logic Gates

Build logic from Bits.

## Core gates

* NOT
* AND
* OR
* XOR
* NAND
* NOR

These are the foundation of all computation.

---

# 4. Phase 3 — Multi-Bit Structures

Once Bits work, build structured types.

## Types

* Bit
* Byte (8 bits)
* Word (32 bits)
* Address (16 bits)

Each represents a different **meaning**, not just size.

---

# 5. Phase 4 — Arithmetic Logic

## Components

* Half Adder
* Full Adder
* Multi-bit Adder
* Subtractor
* Comparator
* Shifter

---

## Half Adder

Inputs:

```text
A, B
```

Outputs:

```text
Sum, Carry
```

---

## Full Adder

Inputs:

```text
A, B, CarryIn
```

Outputs:

```text
Sum, CarryOut
```

---

## Multi-bit Adder

Chain full adders to build:

```text
32-bit addition
```

---

# 6. Phase 5 — ALU

The ALU performs all computation.

## Inputs

* A
* B
* Operation

## Outputs

* Result
* Flags

---

## Operations

### Arithmetic

* ADD
* SUB

### Logic

* AND
* OR
* XOR
* NOT

### Comparison

* EQ
* NEQ
* LT
* GT

### Bit operations

* SHIFT LEFT
* SHIFT RIGHT

---

# 7. Phase 6 — Flags

```text
Z = Zero
N = Negative
C = Carry
V = Overflow
```

Used for conditional execution:

```asm
JZ label
JNZ label
```

---

# 8. Phase 7 — Registers

## General registers

```text
R0 - R15
```

## Special registers

```text
PC   (Program Counter)
SP   (Stack Pointer)
IR   (Instruction Register)
FLAGS
```

---

# 9. Phase 8 — Memory

Memory is a simple mapping:

```text
Address → Byte
```

Operations:

* read
* write
* reset
* bounds checking

---

# 10. Phase 9 — Bus (Conceptual)

A bus connects components:

```text
CPU ↔ Memory ↔ I/O
```

You may simulate it or keep it abstract.

---

# 11. Phase 10 — ISA Design

## Instruction categories

### Data movement

* MOV
* LOAD
* STORE

### Arithmetic

* ADD
* SUB

### Logic

* AND
* OR
* XOR
* NOT

### Control flow

* JMP
* JZ
* JNZ

### Stack

* PUSH
* POP

### System

* NOP
* HALT

Keep ISA small and orthogonal.

---

# 12. Phase 11 — Instruction Encoding

Define how instructions are stored in memory.

Example structure:

```text
[opcode][registers][immediate/address]
```

Keep it simple and consistent.

---

# 13. Phase 12 — CPU Cycle

```text
FETCH → DECODE → EXECUTE → WRITEBACK
```

---

# 14. Phase 13 — First Programs

Write machine code manually:

* addition
* loops
* comparisons
* stack usage
* function calls

---

# 15. Phase 14 — Assembly Language

Example:

```asm
MOV R0, 10
ADD R0, R1
JMP loop
```

---

# 16. Phase 15 — Assembler

Pipeline:

```text
Source → Lexer → Parser → Symbol Resolution → Encoding → Machine Code
```

---

# 17. Phase 16 — Disassembler

Machine code → Assembly

Used for debugging.

---

# 18. Phase 17 — Debugger

Features:

* step execution
* breakpoints
* register view
* memory view
* stack trace

---

# 19. Phase 18 — I/O

Memory-mapped I/O:

```text
0xFF00 → output
0xFF01 → input
```

---

# 20. Phase 19 — Stack & Calling Convention

Define:

* function calls
* return addresses
* argument passing
* stack layout

---

# 21. Phase 20 — High-Level Language

Start simple:

```text
let x = 10;
let y = 20;
let z = x + y;
```

---

# 22. Phase 21 — Compiler Pipeline

```text
Source → Lexer → Parser → AST → IR → Optimization → Codegen → Assembly
```

---

# 23. Phase 22 — Lexer

Turns text into tokens.

---

# 24. Phase 23 — Parser

Builds AST.

---

# 25. Phase 24 — Semantic Analysis

Checks correctness:

* types
* variables
* scope
* function calls

---

# 26. Phase 25 — IR

Intermediate representation before assembly.

---

# 27. Phase 26 — Code Generation

IR → Assembly

---

# 28. Phase 27 — Runtime

Basic standard library:

* print
* input
* memory allocation

---

# 29. Phase 28 — Memory Model

```text
Code
Data
Heap
Stack
```

---

# 30. Phase 29 — Optimizations

* constant folding
* dead code removal
* simplification

---

# 31. Phase 30 — Advanced CPU Features

* interrupts
* timers
* privilege levels
* caching (optional)

---

# 31.5. Phase 30.5 — MMU & Memory Protection

* Memory Management Unit (MMU)
* Guard Pages & Segmentation Fault (`SegFaultException`) traps
* Dynamic Stack Limits derived from `MEMORY_SIZE_BYTES`
* Page tables with Read/Write/Execute (R/W/X) permissions

---

# 32. Phase 31 — OS Layer (Optional)

* scheduler
* filesystem
* shell

---

# 33. Phase 32 — Self-Hosting (Optional)

Compiler written in your own language.

---

# 34. Architecture Separation Rule

Each layer must only depend on the one below it:

```text
Language
↓
Compiler
↓
Assembly
↓
ISA
↓
CPU
↓
Hardware model
```

---

# 35. Testing Strategy

Every layer must be tested independently:

* Bit logic → exhaustive tests
* ALU → edge cases
* Memory → read/write correctness
* CPU → full programs
* Compiler → language features

---

# 36. Java Package Structure (Overall Project Layout)

This is a recommended **clean modular package architecture** for implementing the entire system in Java.

The goal is strict separation of concerns so each layer maps directly to the conceptual stack.

---

## 36.1 Root package

```text
com.yourname.computer
```

---

## 36.2 Core hardware layer

```text
com.yourname.computer.core
```

### Subpackages

#### Bits and primitives

```text
com.yourname.computer.core.bit
com.yourname.computer.core.byte
com.yourname.computer.core.word
com.yourname.computer.core.address
```

#### Logic gates

```text
com.yourname.computer.core.logic
```

#### Arithmetic

```text
com.yourname.computer.core.arithmetic
```

---

## 36.3 CPU layer

```text
com.yourname.computer.cpu
```

### Subpackages

```text
com.yourname.computer.cpu.alu
com.yourname.computer.cpu.registers
com.yourname.computer.cpu.controlunit
com.yourname.computer.cpu.flags
com.yourname.computer.cpu.instructions
```

---

## 36.4 Memory system

```text
com.yourname.computer.memory
```

### Subpackages

```text
com.yourname.computer.memory.ram
com.yourname.computer.memory.cache   (optional later)
com.yourname.computer.memory.io
```

---

## 36.5 Bus and system interconnect

```text
com.yourname.computer.bus
```

---

## 36.6 ISA layer

```text
com.yourname.computer.isa
```

### Subpackages

```text
com.yourname.computer.isa.encoding
com.yourname.computer.isa.decoder
com.yourname.computer.isa.opcodes
```

---

## 36.7 Assembly layer

```text
com.yourname.computer.assembly
```

### Subpackages

```text
com.yourname.computer.assembly.lexer
com.yourname.computer.assembly.parser
com.yourname.computer.assembly.assembler
com.yourname.computer.assembly.disassembler
```

---

## 36.8 Compiler layer

```text
com.yourname.computer.compiler
```

### Subpackages

```text
com.yourname.computer.compiler.lexer
com.yourname.computer.compiler.parser
com.yourname.computer.compiler.ast
com.yourname.computer.compiler.semantics
com.yourname.computer.compiler.ir
com.yourname.computer.compiler.codegen
com.yourname.computer.compiler.optimization
```

---

## 36.9 Runtime layer

```text
com.yourname.computer.runtime
```

---

## 36.10 Debugging tools

```text
com.yourname.computer.debugger
```

---

## 36.11 Testing framework

```text
com.yourname.computer.tests
```

---

## 36.12 Utilities

```text
com.yourname.computer.util
```

---

## 36.13 Optional OS layer

```text
com.yourname.computer.os
```

---

## Design rule for packages

Each package must obey:

```text
higher layer → depends only on lower layer
```

Example:

* `compiler` depends on `assembly`
* `assembly` depends on `isa`
* `isa` depends on `cpu`
* `cpu` depends on `core`

But never the reverse.
