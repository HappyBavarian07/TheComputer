---
id: "CORE-005"
title: "Implement 32-Bit Word Adder and Subtractor"
module: "core"
status: "DONE"
priority: "HIGH"
phase: "Phase 4 - Arithmetic Logic"
dependencies: [CORE-004]
tags: []
---

# CORE-005 — Implement 32-Bit Word Adder and Subtractor

## Description

Cascade 32 Full Adders to construct a zero-allocation 32-bit Word adder/subtractor with Carry and Overflow output.

## Acceptance Criteria

1. 32-bit addition computes sum in destination Word.
2. Subtraction implemented using two's complement inversion + CarryIn=1.
3. Correct generation of CarryOut and Two's Complement Overflow signals.
4. Comprehensive unit tests covering boundary values (0, MaxInt, MinInt, overflow).

## Notes

Requires scratch bit pre-allocation in adder instance.
