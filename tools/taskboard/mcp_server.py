import os
import sys
import json

def get_tasks_file():
    # Always resolve relative to script location tools/taskboard/mcp_server.py -> project root
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    tasks_path = os.path.join(project_root, "docs", "tasks.json")
    if os.path.exists(tasks_path):
        return tasks_path
    return tasks_path

def load_data():
    file_path = get_tasks_file()
    if not os.path.exists(file_path):
        return {"project": "TheComputer", "version": "1.0.0", "tasks": []}
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)

def save_data(data):
    file_path = get_tasks_file()
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    tmp_path = file_path + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    os.replace(tmp_path, file_path)

def handle_request(request):
    method = request.get("method")
    params = request.get("params", {})
    req_id = request.get("id")

    # 1. MCP Standard Initialize Handshake
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "result": {
                "protocolVersion": params.get("protocolVersion", "2024-11-05"),
                "capabilities": {
                    "tools": {}
                },
                "serverInfo": {
                    "name": "task-manager",
                    "version": "1.0.0"
                }
            }
        }

    # 2. Notifications (e.g. notifications/initialized)
    if method and method.startswith("notifications/"):
        return None

    # 3. List Tools
    if method == "tools/list":
        tools = [
            {
                "name": "list_tasks",
                "description": "List all project tasks filtered by module, status, or priority.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "module": {"type": "string"},
                        "status": {"type": "string", "enum": ["TODO", "IN_PROGRESS", "DONE", "all"]},
                        "priority": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW", "all"]}
                    }
                }
            },
            {
                "name": "get_task",
                "description": "Get details of a specific task by ID (e.g. CORE-001).",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "task_id": {"type": "string"}
                    },
                    "required": ["task_id"]
                }
            },
            {
                "name": "create_task",
                "description": "Create a new project task ticket.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "task_id": {"type": "string"},
                        "title": {"type": "string"},
                        "module": {"type": "string"},
                        "priority": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"]},
                        "description": {"type": "string"},
                        "phase": {"type": "string"}
                    },
                    "required": ["task_id", "title", "module"]
                }
            },
            {
                "name": "update_task_status",
                "description": "Update the status of a task (TODO, IN_PROGRESS, DONE).",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "task_id": {"type": "string"},
                        "status": {"type": "string", "enum": ["TODO", "IN_PROGRESS", "DONE"]}
                    },
                    "required": ["task_id", "status"]
                }
            }
        ]
        return {"jsonrpc": "2.0", "id": req_id, "result": {"tools": tools}}

    # 4. Call Tools
    elif method == "tools/call":
        name = params.get("name")
        args = params.get("arguments", {})
        data = load_data()
        tasks = data.get("tasks", [])

        if name == "list_tasks":
            mod = args.get("module", "all")
            stat = args.get("status", "all")
            prio = args.get("priority", "all")

            filtered = []
            for t in tasks:
                if mod != "all" and t.get("module") != mod:
                    continue
                if stat != "all" and t.get("status") != stat:
                    continue
                if prio != "all" and t.get("priority") != prio:
                    continue
                filtered.append(t)

            return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": json.dumps(filtered, indent=2)}]}}

        elif name == "get_task":
            tid = args.get("task_id")
            for t in tasks:
                if t.get("id") == tid:
                    return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": json.dumps(t, indent=2)}]}}
            return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": f"Task {tid} not found."}], "isError": True}}

        elif name == "create_task":
            new_task = {
                "id": args.get("task_id", f"TASK-{len(tasks)+1:03d}"),
                "title": args.get("title"),
                "module": args.get("module", "server"),
                "priority": args.get("priority", "HIGH"),
                "status": "TODO",
                "description": args.get("description", ""),
                "phase": args.get("phase", "General"),
                "tags": [args.get("module", "SERVER").upper()]
            }
            tasks.append(new_task)
            save_data(data)
            return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": f"Task {new_task['id']} created successfully."}]}}

        elif name == "update_task_status":
            tid = args.get("task_id")
            nstat = args.get("status")
            found = False
            for t in tasks:
                if t.get("id") == tid:
                    t["status"] = nstat
                    found = True
                    break
            if found:
                save_data(data)
                return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": f"Task {tid} updated to {nstat}."}]}}
            else:
                return {"jsonrpc": "2.0", "id": req_id, "result": {"content": [{"type": "text", "text": f"Task {tid} not found."}], "isError": True}}

    if req_id is not None:
        return {"jsonrpc": "2.0", "id": req_id, "error": {"code": -32601, "message": f"Method {method} not found"}}
    return None

def main():
    while True:
        line = sys.stdin.readline()
        if not line:
            break
        line_str = line.strip()
        if not line_str:
            continue
        try:
            req = json.loads(line_str)
            res = handle_request(req)
            if res is not None:
                sys.stdout.write(json.dumps(res) + "\n")
                sys.stdout.flush()
        except Exception as e:
            if "id" in line_str:
                err = {"jsonrpc": "2.0", "error": {"code": -32603, "message": str(e)}}
                sys.stdout.write(json.dumps(err) + "\n")
                sys.stdout.flush()

if __name__ == "__main__":
    main()
