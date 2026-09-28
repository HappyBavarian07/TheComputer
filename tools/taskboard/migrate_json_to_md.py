"""One-shot: migrate docs/tasks.json into the docs/tasks/<module>/<ID>.md tree.

After this, taskstore (GUI + MCP) reads the md files; tasks.json is legacy and
can be deleted once you're happy. Safe to re-run (it rewrites from the json).
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import taskstore

def main():
    root = taskstore.project_root()
    src = os.path.join(root, "docs", "tasks.json")
    with open(src, "r", encoding="utf-8") as f:
        data = json.load(f)
    tasks = data.get("tasks", []) if isinstance(data, dict) else data
    taskstore.save_tasks(tasks)
    print(f"migrated {len(tasks)} tasks -> {taskstore.tasks_dir()}")

if __name__ == "__main__":
    main()
