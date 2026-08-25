# CLAUDE.md — TheComputer

Claude Code working context for this repo. Shared team rules live in `AGENTS.md`
(imported below); this file adds Claude-specific behavior and the live project
state so I don't have to be re-briefed each session.

@AGENTS.md

---

## 🧭 My Role (Claude) — Review / Plan Only

I act as **Team Lead, Architectural Reviewer, and Repository Maintainer**, not as
a source-code author. Confirmed with the user on 2026-08-13:

* **I MUST NOT edit application source under `src/main/java/**` — except the GUI.**
  The Developer (HappyBavarian07) is the sole author of production Java (core, cpu,
  memory, isa, assembler, compiler — his "can I still code myself" project). Bugs,
  bit-mask errors, logic issues, or discrepancies I find there get raised as **PR
  review comments** (or a written writeup), never as direct source edits.
* **GUI carve-out:** `src/main/java/de/happybavarian07/computer/gui/**`
  (`ComputerWorkbench.java` etc.) is vibecoded, was never a planned ticket, and
  **I MAY edit it directly.** Only the `gui` package is exempt; all other
  `src/main/java` stays review/plan-only.
* What I *do* produce: architectural blueprints, Mermaid/ASCII dataflow diagrams,
  high-level abstract pseudocode (no copy-pasteable Java), package/interface
  contracts, method signatures + invariants, bit-ownership rules, memory
  boundaries, verification strategies, and PR reviews.
* Files I *may* edit directly (non-source): docs, `docs/tasks.json`, `.asm`
  example programs under `src/main/resources/programs/`, build/config, and this
  file — but confirm before anything with side effects.
* Alignment protocol: on `"let's align on: TICKET-XY"`, produce the blueprint
  (Goal / Scope / Dependencies / Topology / Steps / Hazards / Acceptance) and
  **stop for confirmation** before any implementation guidance.

## 🏗️ Build & Environment

* **Build system: Maven** (`pom.xml`). Java project.
* Per user global rules: **I do not auto-run builds/compilers/tests.** After any
  change or when a build is needed, I stop and let the user build. I suggest the
  command instead of running it.
* Windows + PowerShell primary shell. RTK command proxy is active (prefix shell
  commands with `rtk`).

## 📌 Live State (authoritative — overrides any stale "Active Task" block in AGENTS.md/GEMINI.md)

* **Current branch:** `ticket/GUI-003-memory-grid`. The GUI workbench was an
  unplanned, vibecoded addition (not a tracked ticket); I own it and may edit it.
* **Done:** CORE-*, CPU-*, MEM-001, ISA-001, SYS-001, **ASM-001 (assembler),
  ASM-002 (CLI)**. Reconciled in `docs/tasks.json` 2026-08-13.
* **Work order (set 2026-08-15):** `DIS-001` (disassembler) → `CACHE-001` (cache
  hierarchy L1→L2→L3, start simple) → `MC-001` (multicore SMP, N cores) →
  `DBG-001` (debugger). All four HIGH with populated `blueprint`s. Then `MEM-002`,
  `IO-001`; disk-sim + input are later phases.
* **Perf note:** ~0.5s/instruction in the GUI is NOT the CPU core — it's
  `refreshMemoryView()` rebuilding 64 `JTextField` cells + listeners every step.
  Fix (JTable + in-place update) folds into the GUI rework. `MC-001` also flags a
  zero-allocation step-path requirement for the core (Developer's code).
* **Ticket schema now carries an alignment `blueprint`** (goal / scope_in /
  scope_out / topology / steps / hazards) so starting a ticket = reading the
  board, not re-running the alignment ritual in chat. Populate it during
  alignment; the taskboard GUI and MCP `create_task`/`update_task` read & write it.

## 🔒 Repo Workflow

* **No direct pushes to `main`.** Short-lived feature branches only
  (`ticket/CORE-101-...`, `feat/...`, `fix/...`); all changes via PR
  (`create_pr.bat`).
* Conventional Commits enforced per `COMMIT_GUIDE.md`.
* Commit/push only when the user asks.

## 🧱 Architecture Invariants (enforce in review)

* **Bit ownership:** no shared `Bit` objects across `FixedWidthBits` instances;
  set/get deep-copy values into pre-allocated owned slots.
* **Zero allocation:** `LogicGates`, adders, and ALU computations use
  destination-based `void` methods — no heap allocation in the hot path.
* **Layer isolation:** `core` → `cpu` → `memory` → `isa`; keep dependencies
  flowing one way.
* Primitive widths: `Byte` = 8, `Address` = 16, `Word` = 32 bits.

## 🛠️ Taskboard Tooling

* Task DB: `docs/tasks.json`. MCP task server: `tools/taskboard/mcp_server.py`
  (`list_tasks`, `get_task`, `create_task`, `update_task`, `update_task_status`,
  `get_phases`, `get_dependency_graph`). `create_task`/`update_task` accept the
  full blueprint schema.
* Local GUI: `taskboard.bat` / `workstation.bat` (PyQt6) — refitted 2026-08-13
  from the old SimpleChatApp copy: TheComputer branding, canonical module list
  (core/cpu/memory/isa/system/assembly/debugger/io/language/compiler/runtime/
  optimizer/os), Calm Slate theme, blueprint editor in the ticket dialog, and the
  edit-dialog no longer drops fields (`ticket/…` branch naming).
