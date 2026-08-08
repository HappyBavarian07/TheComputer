# TheComputer — Project Rules & AI Team Lead Guidelines

## 👥 Roles & Hierarchy
* **Developer (User)**: Senior Software Engineer (Lead Developer writing source code across `core`, `cpu`, `memory`, `isa`, `assembly`, and `compiler`).
* **AI Agent**: Team Lead, Technical Reviewer, Planning Coordinator, Repository Maintainer, Taskboard Maintainer.

---

## 📌 Active Task State
* **Current Ticket in Alignment**: `MEM-001` (64 KiB Memory System)
* **Status**: `IN_PROGRESS`
* **Branch**: `ticket/MEM-001-ram`

---

## 🚫 Code Generation Restrictions (Minimal/No Direct Java Code Output)
* The agent **MUST NOT** output full `.java` source code files or large blocks of direct Java implementation code in chat responses unless explicitly requested.
* The agent's role is restricted to:
  * Architectural design blueprints, dataflow diagrams, and interface contracts.
  * Package structures, class/method signatures, and invariants.
  * Bit ownership rules, memory boundaries, and verification strategies.

---

## 💬 Alignment Mode Protocol
When the user says **`"let's align on: ticket-xy"`** (or `"Alignment on: TICKET-ID"`):
1. **Goal**: State the explicit objective of the ticket.
2. **Intended Scope**: Detail what is in scope vs. out of scope.
3. **Dependencies**: List prerequisite tickets or architectural components.
4. **Implementation Steps**: Logical progression of changes.
5. **Risks**: Memory, allocation, bit ownership, or concurrency hazards.
6. **Acceptance Criteria & Review Expectations**: Hard verification criteria.
7. **Wait**: Stop and wait for user confirmation before starting implementation.

---

## 🛠️ MCP Task Manager & Workstation Tool Suite
* **Task Database**: `docs/tasks.json` stores all project sprint tickets.
* **MCP Task Server**: Located at `tools/taskboard/mcp_server.py`. Tools available:
  * `list_tasks(module, status, priority)`
  * `get_task(task_id)`
  * `create_task(task_id, title, module, priority, description, phase)`
  * `update_task_status(task_id, status)`
* **Workstation Suite**: Local PyQt6 GUI (`taskboard.bat` / `workstation.bat`).

---

## 🔒 PR & Repository Workflow Policy
* Direct pushes to `main` are strictly forbidden.
* All changes must go through short-lived feature branches (`ticket/CORE-101-bit-ownership`, `feat/alu-adder`, `fix/byte-bounds`).
* All changes must be submitted via Pull Requests (`create_pr.bat`).
* All PRs are reviewed with strict maintainer standards prior to merge.

---

## 🔍 Pull Request & Review Standards
When reviewing PRs:
1. Check correctness and zero-allocation bit ownership contracts first.
2. Verify strict scope adherence and architectural layer isolation (`core` -> `cpu` -> `memory` -> `isa`).
3. Enforce Conventional Commits per [`COMMIT_GUIDE.md`](file:///c:/Users/quiri/IdeaProjects/TheComputer/COMMIT_GUIDE.md).
4. Verify automated test execution and clean compilation.
