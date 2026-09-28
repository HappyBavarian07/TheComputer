---
id: "CACHE-001"
title: "Implement CPU Cache Hierarchy (L1 -> L2 -> L3)"
module: "memory"
status: "TODO"
priority: "HIGH"
phase: "Phase 15 - Cache Hierarchy"
dependencies: [MEM-001, SYS-001]
tags: [MEMORY]
---

# CACHE-001 — Implement CPU Cache Hierarchy (L1 -> L2 -> L3)

## Description

Cache layer between CPU and main memory to cut access latency. Start with one simple L1; architected so L2/L3 chain in without touching CPU or RAM.

## Acceptance Criteria

1. L1 read hit/miss + fill works; results identical to running without cache.
2. Hit/miss statistics exposed.
3. Transparent: a program's final state is byte-identical with and without the cache.
4. Later phases add L2, L3, set-associativity, write-back, LRU.

## Notes

Start simple (single direct-mapped write-through L1), grow to full L1/L2/L3.

## Blueprint

### Goal

A cache hierarchy on the CPU<->memory path that reduces effective memory latency, beginning with a single simple L1 and structured so L2 and L3 slot in as a chain with no changes to the CPU or RAM.

### In Scope

- Phase 1: single L1, direct-mapped, write-through + no-write-allocate.
- Tag/index/offset decode from a 16-bit Address; line fill on read miss.
- Same read/write(Address, Word) interface as a bus device so it is transparent to the CPU.
- Hit/miss counters and a simple stats view.
- Optional wiring into Motherboard between CPU and SystemBus.
- Later phases: L2 then L3 by chaining; set-associativity + LRU; write-back/write-allocate.

### Out of Scope

- Cross-core cache coherence (owned by the multicore ticket, later phase).
- Prefetching / victim caches.
- TLB / address translation (MEM-002).

### Topology

cpu -> cache(L1[-L2[-L3]]) -> systembus -> ram. Each cache level implements the same read/write(Address, Word) contract as the bus and holds a reference to the next level; chaining levels needs no CPU change. Cache is transparent: identical observable results with or without it.

### Steps

- Define a CacheLevel abstraction: read(Address, Word dst), write(Address, Word), backed by the next level (cache or bus).
- Implement direct-mapped L1: partition Address into tag/index/offset; on read miss, fill the line from the next level; on write, write-through to next level.
- Preallocate line storage (owned Word slots) so steady-state is zero-allocation.
- Expose hit/miss stats.
- Optionally insert L1 between CPU and SystemBus in Motherboard behind a flag.
- Verify transparency: run programs with and without cache, assert byte-identical final memory + registers.
- Later: parametrize associativity + replacement (LRU) + write-back; add L2/L3 by chaining.

### Hazards

- Bit ownership: cache lines must deep-copy Word/Bit values, never alias RAM's or the CPU's Bit objects.
- 16-bit Address partition (tag/index/offset) must be exact and total.
- Write-through vs write-back correctness and ordering to the next level.
- Steady-state zero allocation: preallocate lines, no per-access heap objects.
- Transparency is the invariant: cache must never change program-visible results.
