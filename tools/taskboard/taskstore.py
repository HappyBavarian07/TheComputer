"""
Markdown-backed task store. Source of truth is a tree of one file per ticket:

    docs/tasks/<module>/<TICKET-ID>.md

Each file has a small frontmatter block (machine fields) + a markdown body
(human-editable prose). Both the taskboard GUI and the MCP server read/write
through here, so the tasks are directly editable by hand.

No third-party dependencies: the frontmatter is a tiny fixed schema (scalars +
short string lists), parsed/written by the helpers below rather than a YAML lib.
The parser still reads block-style ("- item") lists so older files load fine.
"""

import os
import re

FRONTMATTER_FIELDS = ["id", "title", "module", "status", "priority", "phase", "dependencies", "tags"]
LIST_FIELDS = {"dependencies", "tags"}


def project_root():
    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.abspath(os.path.join(here, "..", ".."))
    cur = root
    while True:
        if os.path.isdir(os.path.join(cur, "docs")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur:
            return root
        cur = parent


def tasks_dir():
    return os.path.join(project_root(), "docs", "tasks")


# ---------------------------------------------------------- frontmatter I/O ---

def _quote(s):
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"') + '"'


def _unquote(s):
    s = s.strip()
    if len(s) >= 2 and s[0] == '"' and s[-1] == '"':
        return s[1:-1].replace('\\"', '"').replace("\\\\", "\\")
    return s


def _list_item(x):
    s = str(x)
    return _quote(s) if re.search(r'[\[\],"\s]', s) else s


def _dump_frontmatter(fm):
    lines = []
    for k in FRONTMATTER_FIELDS:
        if k not in fm:
            continue
        if k in LIST_FIELDS:
            items = ", ".join(_list_item(x) for x in (fm[k] or []))
            lines.append(f"{k}: [{items}]")
        else:
            lines.append(f"{k}: {_quote(fm[k])}")
    return "\n".join(lines)


def _split_top_commas(s):
    out, buf, in_q = [], "", False
    for ch in s:
        if ch == '"':
            in_q = not in_q
            buf += ch
        elif ch == "," and not in_q:
            out.append(buf)
            buf = ""
        else:
            buf += ch
    if buf.strip():
        out.append(buf)
    return [_unquote(x) for x in out if x.strip()]


def _parse_frontmatter(block):
    fm = {}
    lines = block.split("\n")
    i = 0
    while i < len(lines):
        m = re.match(r"^([A-Za-z0-9_]+):\s*(.*)$", lines[i])
        if not m:
            i += 1
            continue
        key, rest = m.group(1), m.group(2).strip()
        if rest.startswith("[") and rest.endswith("]"):
            fm[key] = _split_top_commas(rest[1:-1])
        elif rest == "":
            items, j = [], i + 1
            while j < len(lines) and re.match(r"^\s*-\s+", lines[j]):
                items.append(_unquote(re.sub(r"^\s*-\s+", "", lines[j]).strip()))
                j += 1
            if items:
                fm[key] = items
                i = j
                continue
            fm[key] = ""
        else:
            fm[key] = _unquote(rest)
        i += 1
    return fm


# ---------------------------------------------------------------- serialize ---

def _text_section(out, name, value, level="##"):
    if value:
        out.append(f"{level} {name}")
        out.append("")
        out.append(str(value).strip())
        out.append("")


def _list_section(out, name, items, level="###"):
    items = [i for i in (items or []) if str(i).strip()]
    if items:
        out.append(f"{level} {name}")
        out.append("")
        out.extend(f"- {i}" for i in items)
        out.append("")


def to_markdown(task):
    fm = {}
    for k in FRONTMATTER_FIELDS:
        v = task.get(k)
        if k in LIST_FIELDS:
            fm[k] = list(v) if v else []
        elif v is not None:
            fm[k] = v

    out = ["---", _dump_frontmatter(fm), "---", ""]
    heading = task.get("id", "")
    if task.get("title"):
        heading = f"{heading} — {task['title']}".strip(" —")
    out.append(f"# {heading}")
    out.append("")

    _text_section(out, "Description", task.get("description"))
    _text_section(out, "Acceptance Criteria", task.get("acceptance_criteria"))
    _text_section(out, "Notes", task.get("notes"))

    bp = task.get("blueprint") or {}
    if any(bp.get(k) for k in ("goal", "scope_in", "scope_out", "topology", "steps", "hazards")):
        out.append("## Blueprint")
        out.append("")
        _text_section(out, "Goal", bp.get("goal"), level="###")
        _list_section(out, "In Scope", bp.get("scope_in"))
        _list_section(out, "Out of Scope", bp.get("scope_out"))
        _text_section(out, "Topology", bp.get("topology"), level="###")
        _list_section(out, "Steps", bp.get("steps"))
        _list_section(out, "Hazards", bp.get("hazards"))

    return "\n".join(out).rstrip() + "\n"


# ------------------------------------------------------------------- parse ---

_FRONT_RE = re.compile(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", re.DOTALL)


def from_markdown(text):
    m = _FRONT_RE.match(text)
    if m:
        fm = _parse_frontmatter(m.group(1))
        body = m.group(2)
    else:
        fm = {}
        body = text

    task = dict(fm)
    task.setdefault("dependencies", [])
    task.setdefault("tags", [])

    sections = {}
    cur = None
    for ln in body.split("\n"):
        if re.match(r"^#\s+", ln):          # doc title, ignore
            cur = None
            continue
        h = re.match(r"^#{2,3}\s+(.*)$", ln)
        if h:
            cur = h.group(1).strip().lower()
            sections[cur] = []
            continue
        if cur is not None:
            sections[cur].append(ln)

    def text_of(key):
        return "\n".join(sections.get(key, [])).strip()

    def bullets_of(key):
        return [re.sub(r"^-\s+", "", l).strip()
                for l in sections.get(key, []) if l.strip().startswith("- ")]

    if text_of("description"):
        task["description"] = text_of("description")
    if text_of("acceptance criteria"):
        task["acceptance_criteria"] = text_of("acceptance criteria")
    if text_of("notes"):
        task["notes"] = text_of("notes")

    bp = {}
    if text_of("goal"):
        bp["goal"] = text_of("goal")
    if bullets_of("in scope"):
        bp["scope_in"] = bullets_of("in scope")
    if bullets_of("out of scope"):
        bp["scope_out"] = bullets_of("out of scope")
    if text_of("topology"):
        bp["topology"] = text_of("topology")
    if bullets_of("steps"):
        bp["steps"] = bullets_of("steps")
    if bullets_of("hazards"):
        bp["hazards"] = bullets_of("hazards")
    if bp:
        task["blueprint"] = bp

    return task


# --------------------------------------------------------------- load/save ---

def load_tasks():
    root = tasks_dir()
    if not os.path.isdir(root):
        return []
    tasks = []
    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            if not fn.endswith(".md"):
                continue
            try:
                with open(os.path.join(dirpath, fn), "r", encoding="utf-8") as f:
                    t = from_markdown(f.read())
                if t.get("id"):
                    tasks.append(t)
            except (OSError, ValueError):
                continue
    tasks.sort(key=lambda t: (str(t.get("module") or ""), str(t.get("id") or "")))
    return tasks


def _path_for(task):
    module = (task.get("module") or "misc").lower()
    tid = task.get("id") or "UNKNOWN"
    return os.path.join(tasks_dir(), module, f"{tid}.md")


def save_tasks(tasks):
    """Write every task to its md file and prune md files no longer represented."""
    root = tasks_dir()
    os.makedirs(root, exist_ok=True)
    wanted = set()
    for t in tasks:
        path = _path_for(t)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(to_markdown(t))
        wanted.add(os.path.normcase(os.path.abspath(path)))

    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            if not fn.endswith(".md"):
                continue
            p = os.path.normcase(os.path.abspath(os.path.join(dirpath, fn)))
            if p not in wanted:
                try:
                    os.remove(p)
                except OSError:
                    pass
