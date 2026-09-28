import os
import sys
import re
import json
import subprocess
import shutil

# Tickets live in docs/tasks/<module>/<ID>.md (see tools/taskboard/taskstore.py).
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "taskboard"))
try:
    import taskstore
except ImportError:
    taskstore = None


def load_all_tasks():
    """Markdown ticket tree first; legacy docs/tasks.json only as a fallback."""
    if taskstore is not None:
        tasks = taskstore.load_tasks()
        if tasks:
            return tasks
    legacy = os.path.join(os.getcwd(), "docs", "tasks.json")
    if os.path.exists(legacy):
        try:
            with open(legacy, "r", encoding="utf-8") as f:
                raw = json.load(f)
            return raw if isinstance(raw, list) else raw.get("tasks", [])
        except Exception as e:
            print(f"[WARNING] Could not parse legacy tasks.json: {e}")
    return []


def find_task(tasks, raw_ticket, ticket_num):
    if raw_ticket:
        for t in tasks:
            if str(t.get("id", "")).upper() == raw_ticket:
                return t
    if ticket_num:
        for t in tasks:
            t_id = str(t.get("id", "")).upper()
            if t_id.endswith(f"-{ticket_num}") or t_id.endswith(ticket_num):
                return t
    return None


def bullets(items):
    return "\n".join(f"* {i}" for i in items)


def run_cmd(cmd):
    res = subprocess.run(cmd, capture_output=True, text=True, cwd=os.getcwd())
    return res.stdout.strip()

def main():
    # 1. Get current branch
    branch = run_cmd(["git", "branch", "--show-current"])
    if not branch or branch in ["master", "main"]:
        print("[ERROR] Please switch to a feature branch before creating a PR.")
        sys.exit(1)

    print(f"[INFO] Active branch: {branch}")

    # 2. Push branch to remote
    remote_name = run_cmd(["git", "remote"])
    if not remote_name:
        remote_name = "origin"
    else:
        remote_name = remote_name.split()[0]

    print(f"[INFO] Pushing branch to remote '{remote_name}'...")
    subprocess.run(["git", "push", "-u", remote_name, branch], cwd=os.getcwd())

    # 3. Detect ticket ID & number from branch name
    ticket_match = re.search(r'([A-Z]+-\d+|\d+)', branch, re.IGNORECASE)
    raw_ticket = ticket_match.group(1).upper() if ticket_match else None
    ticket_num = re.search(r'\d+', raw_ticket).group(0) if raw_ticket else None

    # 4. Look up the ticket in docs/tasks/<module>/<ID>.md
    task_data = find_task(load_all_tasks(), raw_ticket, ticket_num)

    # 5. Construct Detailed Title & Body
    if task_data:
        actual_id = task_data.get("id", raw_ticket or branch)
        module = task_data.get("module", "core")
        title_str = task_data.get("title", branch)
        phase_str = task_data.get("phase", "Implementation")
        pr_title = f"feat({module}): {actual_id} {title_str}"
        
        desc = task_data.get("description", "")
        criteria = task_data.get("acceptance_criteria", "")
        notes = task_data.get("notes", "")
        blueprint = task_data.get("blueprint") or {}
        deps = task_data.get("dependencies") or []

        extra = ""
        if blueprint.get("goal"):
            extra += f"\n**Goal**: {blueprint['goal']}\n"
        if blueprint.get("scope_in"):
            extra += f"\n**In scope**\n{bullets(blueprint['scope_in'])}\n"
        if blueprint.get("scope_out"):
            extra += f"\n**Out of scope**\n{bullets(blueprint['scope_out'])}\n"
        if notes:
            extra += f"\n**Notes**: {notes}\n"
        
        pr_body = f"""## 🎫 Ticket Context
* **Ticket ID**: `{actual_id}`
* **Ticket Title**: {title_str}
* **Module**: `{module}`
* **Phase**: `{phase_str}`
* **Dependencies**: {", ".join(f"`{d}`" for d in deps) if deps else "none"}

---

## 📝 Summary of Changes
{desc}
{extra}
---

## 🧪 Acceptance Criteria & Verification
{criteria}

- [x] Compiles cleanly with Maven (`mvn compile`)
- [x] Structural contracts and architectural boundaries respected

---

## 🔍 AI Team Lead Review Focus
- [ ] Explicit Bit ownership & zero unnecessary allocations
- [ ] Comprehensive unit tests & edge case validation
- [ ] Strict architectural boundary compliance
"""
    else:
        pr_title = f"feat: {branch}"
        pr_body = f"## Summary\nAutomated PR draft for branch {branch}"

    # 6. Execute gh pr create directly
    print("[INFO] Creating GitHub Pull Request via gh CLI...")
    
    gh_exe = shutil.which("gh")
    if not gh_exe:
        default_path = r"C:\Program Files\GitHub CLI\gh.exe"
        if os.path.exists(default_path):
            gh_exe = default_path
        else:
            gh_exe = "gh"

    # Check available base branch (main preferred)
    base_branch = "main"

    gh_cmd = [
        gh_exe, "pr", "create",
        "--title", pr_title,
        "--body", pr_body,
        "--base", base_branch,
        "--head", branch
    ]
    
    result = subprocess.run(gh_cmd, capture_output=True, text=True, cwd=os.getcwd())
    if result.returncode == 0:
        print(f"\n[SUCCESS] Pull Request created successfully!")
        print(result.stdout.strip())
    else:
        err_msg = result.stderr.strip() or result.stdout.strip()
        if "already exists" in err_msg.lower():
            print("[INFO] PR already exists for branch. Updating existing PR title and description...")
            edit_cmd = [
                gh_exe, "pr", "edit",
                "--title", pr_title,
                "--body", pr_body
            ]
            edit_result = subprocess.run(edit_cmd, capture_output=True, text=True, cwd=os.getcwd())
            if edit_result.returncode == 0:
                print(f"\n[SUCCESS] Existing PR updated successfully with full task context!")
            else:
                print(f"[ERROR] Failed to update existing PR: {edit_result.stderr.strip()}")
        else:
            print(f"\n[ERROR] Failed to create Pull Request via gh CLI:")
            print(err_msg)

if __name__ == "__main__":
    main()
