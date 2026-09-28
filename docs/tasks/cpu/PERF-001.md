---
id: "PERF-001"
title: "Optimize Single-Core CPU Throughput & Hot Path"
module: "cpu"
status: "TODO"
priority: "HIGH"
phase: "Phase 18.5 - Single-Core Performance"
dependencies: [CPU-003]
tags: [CPU, PERF]
---

# PERF-001 — Optimize Single-Core CPU Throughput & Hot Path

## Description

Audit and eliminate per-step heap allocations and defensive copies in Cpu.step, Alu, Ram, and SystemBus; tighten the gate-level loop traversal in FixedWidthBits/Word. Establish a repeatable headless throughput benchmark as the baseline, then drive a 1.5x-2x increase in simulated MIPS with byte-identical program behavior. Do this before MC-001 so N cores do not multiply a per-step allocation cost.

## Acceptance Criteria

1. A repeatable headless throughput benchmark (steps/sec) exists and is committed (baseline recorded).
2. No per-step heap allocation on the hot path (verified by profiling / allocation counting).
3. >= 1.5x MIPS versus the recorded baseline.
4. All existing tests still pass; program-visible behavior is byte-identical before and after.

## Notes

Perf work; do on its own branch (ticket/PERF-001-...). The headless benchmark harness doubles as the before/after measuring stick and can back a GUI 'Benchmark core' path.

## Blueprint

### Goal

Make the single-core fetch-decode-execute loop as fast as the zero-allocation design intends, measured by a repeatable benchmark, so that later multicore scaling multiplies a clean hot path rather than a wasteful one.

### In Scope

- Headless throughput benchmark mirroring the GUI 'Benchmark core' path (steps/sec, warmup, fixed workload).
- Remove per-step allocations: new Word/Address/Bit or array copies created inside Cpu.step, Alu ops, Ram, SystemBus per instruction.
- Reuse preallocated scratch for working address/result/operands across steps.
- Tighten FixedWidthBits/Word inner loops (get/set, ripple traversal) on the hot path.
- Record baseline and post-optimization numbers.

### Out of Scope

- Multicore (MC-001) and any threading.
- Caching (CACHE-001).
- Algorithmic ISA changes or new opcodes.
- JIT / bytecode compilation of the guest program.

### Topology

cpu.Cpu.step -> {isa.InstructionDecoder, cpu.alu.Alu, bus.SystemBus -> memory.Ram} over core.word.Word / core.bit.FixedWidthBits. The optimization touches only the hot path objects; observable results must not change.

### Steps

- Build the headless benchmark (fixed loop program, warmup, timed window) and record the baseline steps/sec.
- Profile / allocation-count one representative run to find per-step allocations and copies.
- Replace per-step allocations with preallocated, reused scratch owned by the Cpu/Alu/bus.
- Tighten FixedWidthBits/Word traversal (avoid getAsArray copies, redundant boxing) on the hot path.
- Re-run the benchmark; confirm >= 1.5x and byte-identical behavior against the full test suite.
- Record before/after numbers in the ticket notes.

### Hazards

- Bit ownership: reusing scratch must still deep-copy values into owned slots, never alias across registers/memory.
- Correctness first: any speedup that changes a flag, wrap, or memory value is a regression - guard with the existing suite.
- Micro-benchmark noise: warm up, use a fixed workload and a timed window, report medians.
- Do not trade zero-allocation for hidden shared mutable state that breaks MC-001 later.
