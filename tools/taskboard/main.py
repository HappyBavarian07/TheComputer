import os
import sys
import json
import re
import datetime
import subprocess
from PyQt6.QtCore import Qt, QMimeData, QUrl, QFileSystemWatcher
from PyQt6.QtGui import QDrag, QAction, QColor, QFont
from PyQt6.QtWidgets import (
    QApplication,
    QMainWindow,
    QWidget,
    QVBoxLayout,
    QHBoxLayout,
    QTabWidget,
    QListWidget,
    QListWidgetItem,
    QLabel,
    QPushButton,
    QMenu,
    QMessageBox,
    QSplitter,
    QComboBox,
    QLineEdit,
    QTextEdit,
    QPlainTextEdit,
    QTextBrowser,
    QDialog,
    QFormLayout,
    QDialogButtonBox,
    QTableWidget,
    QTableWidgetItem,
    QCheckBox,
    QFileDialog,
    QHeaderView,
    QFrame,
    QProgressBar
)

# Try importing QWebEngineView for live Mermaid diagram rendering
HAS_WEBENGINE = False
try:
    from PyQt6.QtWebEngineWidgets import QWebEngineView
    HAS_WEBENGINE = True
except ImportError:
    HAS_WEBENGINE = False

# Modern GitHub Dark / Discord theme stylesheet
STYLESHEET = """
QMainWindow {
    background-color: #0d1117;
}
QWidget {
    font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, 'SF Pro Display', Roboto, sans-serif;
    color: #c9d1d9;
}
QDialog {
    background-color: #161b22;
    border: 1px solid #30363d;
    border-radius: 10px;
}
QTabWidget::pane {
    border: 1px solid #30363d;
    background-color: #0d1117;
    border-radius: 8px;
}
QTabBar::tab {
    background-color: #161b22;
    color: #8b949e;
    padding: 10px 22px;
    border: 1px solid #30363d;
    border-bottom: none;
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
    font-weight: 600;
    margin-right: 4px;
}
QTabBar::tab:hover {
    background-color: #21262d;
    color: #f0f6fc;
}
QTabBar::tab:selected {
    background-color: #0d1117;
    color: #58a6ff;
    border-bottom: 2px solid #58a6ff;
}
QLabel#columnTitleTodo {
    font-size: 14px;
    font-weight: bold;
    color: #f0f6fc;
    padding: 10px 14px;
    background-color: #161b22;
    border: 1px solid #30363d;
    border-top: 4px solid #f85149;
    border-radius: 6px;
}
QLabel#columnTitleIP {
    font-size: 14px;
    font-weight: bold;
    color: #f0f6fc;
    padding: 10px 14px;
    background-color: #161b22;
    border: 1px solid #30363d;
    border-top: 4px solid #d29922;
    border-radius: 6px;
}
QLabel#columnTitleDone {
    font-size: 14px;
    font-weight: bold;
    color: #f0f6fc;
    padding: 10px 14px;
    background-color: #161b22;
    border: 1px solid #30363d;
    border-top: 4px solid #238636;
    border-radius: 6px;
}
QListWidget {
    background-color: #161b22;
    border: 1px solid #30363d;
    border-radius: 8px;
    color: #c9d1d9;
    padding: 8px;
}
QListWidget::item {
    background-color: transparent;
    border: none;
    padding: 0px;
    margin: 4px 0px;
}
QPushButton {
    background-color: #238636;
    color: #ffffff;
    border: 1px solid #2ea043;
    border-radius: 6px;
    padding: 8px 16px;
    font-weight: 600;
    font-size: 13px;
}
QPushButton:hover {
    background-color: #2ea043;
}
QPushButton:pressed {
    background-color: #238636;
}
QPushButton#secondaryBtn {
    background-color: #21262d;
    color: #c9d1d9;
    border: 1px solid #30363d;
}
QPushButton#secondaryBtn:hover {
    background-color: #30363d;
    color: #f0f6fc;
}
QComboBox, QLineEdit {
    background-color: #0d1117;
    border: 1px solid #30363d;
    border-radius: 6px;
    padding: 8px 12px;
    color: #f0f6fc;
    font-size: 13px;
}
QComboBox:hover, QLineEdit:focus {
    border: 1px solid #58a6ff;
}
QTextBrowser, QTextEdit, QPlainTextEdit, QTableWidget {
    background-color: #0d1117;
    border: 1px solid #30363d;
    border-radius: 8px;
    color: #c9d1d9;
    padding: 10px;
    font-size: 13px;
}
QProgressBar {
    border: 1px solid #30363d;
    border-radius: 6px;
    text-align: center;
    background-color: #161b22;
    color: #f0f6fc;
    font-weight: bold;
}
QProgressBar::chunk {
    background-color: #238636;
    border-radius: 5px;
}
"""

class TaskCardWidget(QFrame):
    def __init__(self, task_data, parent=None):
        super().__init__(parent)
        self.task_data = task_data
        self.setObjectName("taskCard")
        self.setStyleSheet("""
            QFrame#taskCard {
                background-color: #21262d;
                border: 1px solid #30363d;
                border-radius: 8px;
                padding: 10px;
            }
            QFrame#taskCard:hover {
                background-color: #262c36;
                border: 1px solid #58a6ff;
            }
        """)
        self.setup_ui()

    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(10, 10, 10, 10)
        layout.setSpacing(6)

        header_lay = QHBoxLayout()
        
        id_lbl = QLabel(self.task_data.get("id", "TASK-000"))
        id_lbl.setStyleSheet("font-weight: bold; color: #58a6ff; font-size: 12px;")
        header_lay.addWidget(id_lbl)

        header_lay.addStretch()

        mod = self.task_data.get("module", "server").upper()
        mod_bg = "#1f6beb" if mod == "SERVER" else ("#8957e5" if mod == "CLIENT" else "#238636")
        mod_badge = QLabel(f" {mod} ")
        mod_badge.setStyleSheet(f"background-color: {mod_bg}; color: #ffffff; font-size: 10px; font-weight: bold; border-radius: 4px; padding: 2px 6px;")
        header_lay.addWidget(mod_badge)

        prio = self.task_data.get("priority", "HIGH").upper()
        prio_bg = "#da3633" if prio == "HIGH" else ("#9e6a03" if prio == "MEDIUM" else "#6e7681")
        prio_badge = QLabel(f" {prio} ")
        prio_badge.setStyleSheet(f"background-color: {prio_bg}; color: #ffffff; font-size: 10px; font-weight: bold; border-radius: 4px; padding: 2px 6px;")
        header_lay.addWidget(prio_badge)

        layout.addLayout(header_lay)

        title_lbl = QLabel(self.task_data.get("title", "Untitled Task"))
        title_lbl.setWordWrap(True)
        title_lbl.setStyleSheet("font-size: 14px; font-weight: bold; color: #f0f6fc; margin-top: 2px;")
        layout.addWidget(title_lbl)

        phase = self.task_data.get("phase", "General")
        phase_lbl = QLabel(f"📍 {phase}")
        phase_lbl.setStyleSheet("font-size: 11px; color: #8b949e;")
        layout.addWidget(phase_lbl)

class TaskDetailsDialog(QDialog):
    def __init__(self, parent=None, task_data=None):
        super().__init__(parent)
        self.setWindowTitle(f"Task Details — {task_data.get('id', 'New Task')}" if task_data else "New Task Ticket")
        self.resize(780, 600)
        self.setStyleSheet(STYLESHEET)
        
        self.task_data = task_data or {}
        self.setup_ui()

    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(18, 18, 18, 18)
        layout.setSpacing(12)

        header_lbl = QLabel(f"[{self.task_data.get('id', 'NEW')}] {self.task_data.get('title', 'Ticket Details')}")
        header_lbl.setStyleSheet("font-size: 18px; font-weight: bold; color: #58a6ff;")
        layout.addWidget(header_lbl)

        grid_lay = QHBoxLayout()

        self.id_edit = QLineEdit(self.task_data.get("id", "SERVER-XXX"))
        self.id_edit.setFixedWidth(110)
        grid_lay.addWidget(QLabel("ID:"))
        grid_lay.addWidget(self.id_edit)

        self.title_edit = QLineEdit(self.task_data.get("title", ""))
        grid_lay.addWidget(QLabel("Title:"))
        grid_lay.addWidget(self.title_edit)

        self.module_combo = QComboBox()
        self.module_combo.addItems(["server", "client", "shared"])
        self.module_combo.setCurrentText(self.task_data.get("module", "server"))
        grid_lay.addWidget(QLabel("Module:"))
        grid_lay.addWidget(self.module_combo)

        self.priority_combo = QComboBox()
        self.priority_combo.addItems(["HIGH", "MEDIUM", "LOW"])
        self.priority_combo.setCurrentText(self.task_data.get("priority", "HIGH"))
        grid_lay.addWidget(QLabel("Priority:"))
        grid_lay.addWidget(self.priority_combo)

        self.status_combo = QComboBox()
        self.status_combo.addItems(["TODO", "IN_PROGRESS", "DONE"])
        self.status_combo.setCurrentText(self.task_data.get("status", "TODO"))
        grid_lay.addWidget(QLabel("Status:"))
        grid_lay.addWidget(self.status_combo)

        layout.addLayout(grid_lay)

        self.phase_edit = QLineEdit(self.task_data.get("phase", "Phase 1"))
        phase_lay = QHBoxLayout()
        phase_lay.addWidget(QLabel("Phase / Milestone:"))
        phase_lay.addWidget(self.phase_edit)
        
        copy_branch_btn = QPushButton("Copy Git Branch Name")
        copy_branch_btn.setObjectName("secondaryBtn")
        copy_branch_btn.clicked.connect(self.copy_branch_name)
        phase_lay.addWidget(copy_branch_btn)

        layout.addLayout(phase_lay)

        layout.addWidget(QLabel("Detailed Technical Requirements & Blueprint:"))
        self.desc_edit = QTextEdit()
        self.desc_edit.setPlainText(self.task_data.get("description", ""))
        layout.addWidget(self.desc_edit)

        layout.addWidget(QLabel("Acceptance Criteria & Verification Checklist:"))
        self.criteria_edit = QTextEdit()
        self.criteria_edit.setPlainText(self.task_data.get("acceptance_criteria", ""))
        self.criteria_edit.setFixedHeight(120)
        layout.addWidget(self.criteria_edit)

        bot = QHBoxLayout()
        bot.addStretch()

        save_btn = QPushButton("Save Changes")
        save_btn.clicked.connect(self.accept)
        bot.addWidget(save_btn)

        cancel_btn = QPushButton("Cancel")
        cancel_btn.setObjectName("secondaryBtn")
        cancel_btn.clicked.connect(self.reject)
        bot.addWidget(cancel_btn)

        layout.addLayout(bot)

    def copy_branch_name(self):
        tid = self.id_edit.text().strip()
        title_slug = re.sub(r'[^a-zA-Z0-9]', '-', self.title_edit.text().lower()).strip('-')
        branch_name = f"feature/{tid}-{title_slug}"
        cb = QApplication.clipboard()
        cb.setText(branch_name)
        QMessageBox.information(self, "Branch Name Copied", f"Copied branch name to clipboard:\n\n{branch_name}")

    def get_data(self):
        return {
            "id": self.id_edit.text(),
            "title": self.title_edit.text(),
            "module": self.module_combo.currentText(),
            "priority": self.priority_combo.currentText(),
            "status": self.status_combo.currentText(),
            "description": self.desc_edit.toPlainText(),
            "acceptance_criteria": self.criteria_edit.toPlainText(),
            "phase": self.phase_edit.text(),
            "tags": [self.module_combo.currentText().upper()]
        }

class KanbanListWidget(QListWidget):
    def __init__(self, status, parent_board):
        super().__init__()
        self.status = status
        self.parent_board = parent_board
        self.setDragEnabled(True)
        self.setAcceptDrops(True)
        self.setDropIndicatorShown(True)
        self.setDefaultDropAction(Qt.DropAction.MoveAction)
        self.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.customContextMenuRequested.connect(self.show_context_menu)
        self.itemDoubleClicked.connect(self.on_item_double_clicked)

    def dragEnterEvent(self, event):
        if event.mimeData().hasFormat("application/x-qabstractitemmodeldatalist"):
            event.acceptProposedAction()

    def dragMoveEvent(self, event):
        event.acceptProposedAction()

    def dropEvent(self, event):
        source = event.source()
        if source and source != self:
            selected_items = source.selectedItems()
            if selected_items:
                item = selected_items[0]
                task_data = item.data(Qt.ItemDataRole.UserRole)
                source.takeItem(source.row(item))
                self.addItem(item)
                self.parent_board.move_task(task_data, self.status)
                event.acceptProposedAction()
        else:
            super().dropEvent(event)

    def on_item_double_clicked(self, item):
        task_data = item.data(Qt.ItemDataRole.UserRole)
        if task_data:
            self.parent_board.edit_task(task_data)

    def show_context_menu(self, pos):
        item = self.itemAt(pos)
        if not item:
            return
        task_data = item.data(Qt.ItemDataRole.UserRole)

        menu = QMenu(self)
        menu.setStyleSheet("background-color: #161b22; color: #f0f6fc; border: 1px solid #30363d;")

        edit_act = QAction("View Details / Edit", self)
        edit_act.triggered.connect(lambda: self.parent_board.edit_task(task_data))
        menu.addAction(edit_act)

        branch_act = QAction("Copy Git Branch Name", self)
        branch_act.triggered.connect(lambda: self.copy_task_branch(task_data))
        menu.addAction(branch_act)

        menu.addSeparator()
        if self.status != "TODO":
            act = QAction("Move to To-Do", self)
            act.triggered.connect(lambda: self.parent_board.move_task(task_data, "TODO"))
            menu.addAction(act)
        if self.status != "IN_PROGRESS":
            act = QAction("Move to In-Progress", self)
            act.triggered.connect(lambda: self.parent_board.move_task(task_data, "IN_PROGRESS"))
            menu.addAction(act)
        if self.status != "DONE":
            act = QAction("Move to Done", self)
            act.triggered.connect(lambda: self.parent_board.move_task(task_data, "DONE"))
            menu.addAction(act)

        menu.exec(self.mapToGlobal(pos))

    def copy_task_branch(self, task_data):
        tid = task_data.get("id", "TASK")
        title_slug = re.sub(r'[^a-zA-Z0-9]', '-', task_data.get("title", "").lower()).strip('-')
        branch_name = f"feature/{tid}-{title_slug}"
        cb = QApplication.clipboard()
        cb.setText(branch_name)
        self.parent_board.main_window.statusBar().showMessage(f"Copied branch name: {branch_name}", 3000)

class KanbanBoardWidget(QWidget):
    def __init__(self, json_path, main_window):
        super().__init__()
        self.json_path = json_path
        self.main_window = main_window
        self.tasks = []
        
        self.setup_ui()
        self.load_tasks()

    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(12, 12, 12, 12)

        filter_layout = QHBoxLayout()
        filter_layout.addWidget(QLabel("Module:"))
        self.module_filter = QComboBox()
        self.module_filter.addItems(["ALL", "server", "client", "shared"])
        self.module_filter.currentTextChanged.connect(self.apply_filters)
        filter_layout.addWidget(self.module_filter)

        filter_layout.addWidget(QLabel("Priority:"))
        self.priority_filter = QComboBox()
        self.priority_filter.addItems(["ALL", "HIGH", "MEDIUM", "LOW"])
        self.priority_filter.currentTextChanged.connect(self.apply_filters)
        filter_layout.addWidget(self.priority_filter)

        filter_layout.addWidget(QLabel("Search:"))
        self.search_edit = QLineEdit()
        self.search_edit.setPlaceholderText("Filter title or ID...")
        self.search_edit.textChanged.connect(self.apply_filters)
        filter_layout.addWidget(self.search_edit)

        filter_layout.addStretch()

        add_btn = QPushButton("+ New Ticket")
        add_btn.clicked.connect(self.create_task)
        filter_layout.addWidget(add_btn)

        layout.addLayout(filter_layout)

        prog_lay = QHBoxLayout()
        prog_lay.addWidget(QLabel("Sprint Progress:"))
        self.progress_bar = QProgressBar()
        self.progress_bar.setFixedHeight(18)
        prog_lay.addWidget(self.progress_bar)
        layout.addLayout(prog_lay)

        splitter = QSplitter(Qt.Orientation.Horizontal)

        # Dynamic phase columns container: columns are created in apply_filters
        self.columns_widget = QWidget()
        self.columns_layout = QHBoxLayout(self.columns_widget)
        self.columns_layout.setContentsMargins(0, 0, 0, 0)
        self.columns_layout.setSpacing(12)
        splitter.addWidget(self.columns_widget)

        layout.addWidget(splitter)

    def load_tasks(self):
        if not os.path.exists(self.json_path):
            return
        try:
            with open(self.json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if isinstance(data, list):
                    self.tasks = data
                elif isinstance(data, dict):
                    self.tasks = data.get("tasks", [])
            # Ensure phase_lists mapping exists
            self.phase_lists = {}
            self.apply_filters()
        except (json.JSONDecodeError, OSError, ValueError):
            pass

    def apply_filters(self):
        # Rebuild dynamic phase columns on every filter/update
        # Clear previous column widgets
        for i in reversed(range(self.columns_layout.count())):
            w = self.columns_layout.itemAt(i).widget()
            if w:
                w.setParent(None)

        mod_f = self.module_filter.currentText()
        prio_f = self.priority_filter.currentText()
        search_f = self.search_edit.text().lower()

        total_tasks = 0
        completed_tasks = 0

        # Collect visible tasks after filters
        visible = []
        for t in self.tasks:
            if t.get("status") == "DONE":
                completed_tasks += 1
            if mod_f != "ALL" and t.get("module") != mod_f:
                continue
            if prio_f != "ALL" and t.get("priority") != prio_f:
                continue
            if search_f and search_f not in t.get("title", "").lower() and search_f not in t.get("id", "").lower():
                continue
            visible.append(t)
            total_tasks += 1

        # Group tasks by phase
        phase_map = {}
        for t in visible:
            p = t.get("phase") or "Unspecified"
            phase_map.setdefault(p, []).append(t)

        # Sort phases by natural order (attempt numeric prefix), then name
        def phase_key(p):
            m = re.match(r"Phase\s*(\d+)", p)
            if m:
                return (int(m.group(1)), p)
            return (9999, p)
        phases = sorted(list(phase_map.keys()), key=phase_key)

        self.phase_lists = {}
        for p in phases:
            col_widget = QWidget()
            col_layout = QVBoxLayout(col_widget)
            col_layout.setContentsMargins(0,0,0,0)
            lbl = QLabel(f"{p} ({len(phase_map.get(p, []))})")
            lbl.setStyleSheet("font-weight:bold; color:#f0f6fc; margin-bottom:6px;")
            col_layout.addWidget(lbl)

            listw = PhaseListWidget(p, self)
            self.phase_lists[p] = listw
            col_layout.addWidget(listw)
            self.columns_layout.addWidget(col_widget)

            # populate
            for t in phase_map.get(p, []):
                card_widget = TaskCardWidget(t)
                item = QListWidgetItem()
                item.setSizeHint(card_widget.sizeHint())
                item.setData(Qt.ItemDataRole.UserRole, t)
                listw.addItem(item)
                listw.setItemWidget(item, card_widget)

        # Update overall progress
        pct = int((completed_tasks / len(self.tasks) * 100)) if len(self.tasks) > 0 else 0
        self.progress_bar.setValue(pct)

    

    def save_tasks(self):
        data = {
            "project": "TheComputer",
            "version": "1.0.0",
            "tasks": self.tasks
        }
        with open(self.json_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2)
        self.main_window.statusBar().showMessage("Saved tasks to docs/tasks.json", 3000)

    def move_task(self, task_data, new_status):
        for t in self.tasks:
            if t.get("id") == task_data.get("id"):
                t["status"] = new_status
                break
        self.save_tasks()
        self.apply_filters()

    def move_task_phase(self, task_data, new_phase):
        for t in self.tasks:
            if t.get("id") == task_data.get("id"):
                t["phase"] = new_phase
                break
        self.save_tasks()
        self.apply_filters()

    def edit_task(self, task_data):
        dlg = TaskDetailsDialog(self, task_data)
        if dlg.exec():
            new_data = dlg.get_data()
            for idx, t in enumerate(self.tasks):
                if t.get("id") == task_data.get("id"):
                    self.tasks[idx] = new_data
                    break
            self.save_tasks()
            self.apply_filters()

    def create_task(self):
        dlg = TaskDetailsDialog(self)
        if dlg.exec():
            new_data = dlg.get_data()
            self.tasks.append(new_data)
            self.save_tasks()
            self.apply_filters()

class PhaseListWidget(QListWidget):
    def __init__(self, phase_name, parent_board):
        super().__init__()
        self.phase_name = phase_name
        self.parent_board = parent_board
        self.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.customContextMenuRequested.connect(self.show_context_menu)

    def show_context_menu(self, pos):
        item = self.itemAt(pos)
        if not item:
            return
        task_data = item.data(Qt.ItemDataRole.UserRole)
        menu = QMenu(self)
        menu.setStyleSheet("background-color: #161b22; color: #f0f6fc; border: 1px solid #30363d;")
        edit_act = QAction("View Details / Edit", self)
        edit_act.triggered.connect(lambda: self.parent_board.edit_task(task_data))
        menu.addAction(edit_act)

        # Move to another phase submenu
        move_menu = QMenu("Move to Phase", self)
        phases = sorted({t.get("phase") or "Unspecified" for t in self.parent_board.tasks})
        for p in phases:
            act = QAction(p, self)
            act.triggered.connect(lambda checked, p=p, td=task_data: self.parent_board.move_task_phase(td, p))
            move_menu.addAction(act)
        menu.addMenu(move_menu)

        # Status actions
        menu.addSeparator()
        for s in ("TODO", "IN_PROGRESS", "DONE"):
            act = QAction(f"Set status: {s}", self)
            act.triggered.connect(lambda checked, s=s, td=task_data: self.parent_board.move_task(td, s))
            menu.addAction(act)

        menu.exec(self.mapToGlobal(pos))


class NetworkWidget(QWidget):
    def __init__(self, kanban_board):
        super().__init__()
        self.kanban = kanban_board
        self.critical_set = set()
        self.setup_ui()

    def setup_ui(self):
        layout = QVBoxLayout(self)
        top = QHBoxLayout()
        gen_btn = QPushButton("Generate Graph (DOT)")
        gen_btn.clicked.connect(self.generate_dot)
        top.addWidget(gen_btn)
        export_btn = QPushButton("Export DOT to file")
        export_btn.setObjectName("secondaryBtn")
        export_btn.clicked.connect(self.export_dot)
        top.addWidget(export_btn)
        top.addStretch()
        layout.addLayout(top)

        self.viewer = QTextBrowser()
        layout.addWidget(self.viewer)

    def generate_dot(self):
        tasks = self.kanban.tasks
        phase_nodes = {}
        nodes = []
        edges = []
        for t in tasks:
            nid = t.get("id")
            nodes.append(nid)
            p = t.get("phase") or "Unspecified"
            phase_nodes.setdefault(p, []).append(nid)
            for dep in t.get("dependencies", []):
                edges.append((dep, nid))

        lines = ["digraph G {", "  rankdir=LR;", "  node [shape=box, style=filled, fillcolor=\"#21262d\", fontcolor=\"#c9d1d9\"];"]
        for p, ids in phase_nodes.items():
            safe = re.sub(r"[^A-Za-z0-9_]", "_", p)[:40]
            lines.append(f'  subgraph cluster_{safe} {{')
            lines.append(f'    label = "{p}";')
            for i in ids:
                # color critical nodes differently
                if i in self.critical_set:
                    lines.append(f'    "{i}" [style=filled, fillcolor="\"#b62324\"", fontcolor="#ffffff"];')
                else:
                    lines.append(f'    "{i}";')
            lines.append('  }')
        for a, b in edges:
            lines.append(f'  "{a}" -> "{b}";')
        lines.append('}')
        dot = "\n".join(lines)
        self.last_dot = dot
        self.viewer.setPlainText(dot)

    def set_critical(self, ids):
        try:
            self.critical_set = set(ids)
        except Exception:
            self.critical_set = set()

    def export_dot(self):
        if not hasattr(self, 'last_dot'):
            QMessageBox.information(self, "No graph", "Generate the graph before exporting.")
            return
        fname = QFileDialog.getSaveFileName(self, "Save DOT", os.path.join(os.getcwd(), "task_graph.dot"), "DOT Files (*.dot);;All Files (*)")[0]
        if fname:
            try:
                with open(fname, 'w', encoding='utf-8') as f:
                    f.write(self.last_dot)
                QMessageBox.information(self, "Saved", f"DOT exported to {fname}")
            except Exception as e:
                QMessageBox.critical(self, "Export Error", str(e))


class DependencyRoadmapWidget(QWidget):
    def __init__(self, kanban_board):
        super().__init__()
        self.kanban = kanban_board
        self.setup_ui()

    def setup_ui(self):
        layout = QVBoxLayout(self)
        top = QHBoxLayout()
        top.addWidget(QLabel("Project Start (YYYY-MM-DD):"))
        self.start_edit = QLineEdit(datetime.date.today().isoformat())
        top.addWidget(self.start_edit)
        compute_btn = QPushButton("Compute Schedule")
        compute_btn.clicked.connect(self.compute_schedule)
        top.addWidget(compute_btn)
        export_btn = QPushButton("Export Schedule")
        export_btn.setObjectName("secondaryBtn")
        export_btn.clicked.connect(self.export_schedule)
        top.addWidget(export_btn)
        top.addStretch()
        layout.addLayout(top)

        self.table = QTableWidget()
        self.table.setColumnCount(7)
        self.table.setHorizontalHeaderLabels(["ID","Title","Est(days)","Start","End","Slack","Critical"])
        self.table.horizontalHeader().setStretchLastSection(True)
        layout.addWidget(self.table)

        self.setLayout(layout)

    def compute_schedule(self):
        tasks = self.kanban.tasks
        # build maps
        tasks_by_id = {t.get('id'): t for t in tasks}
        for t in tasks:
            if 'dependencies' not in t or not isinstance(t.get('dependencies'), list):
                t['dependencies'] = []
        # detect cycles and topo sort using Kahn
        indeg = {tid:0 for tid in tasks_by_id}
        succ = {tid:[] for tid in tasks_by_id}
        for tid,t in tasks_by_id.items():
            for d in t.get('dependencies', []):
                if d in tasks_by_id:
                    indeg[tid] += 1
                    succ[d].append(tid)
        queue = [tid for tid,c in indeg.items() if c==0]
        topo = []
        while queue:
            n = queue.pop(0)
            topo.append(n)
            for s in succ.get(n, []):
                indeg[s] -= 1
                if indeg[s]==0:
                    queue.append(s)
        if len(topo) != len(tasks_by_id):
            QMessageBox.critical(self, "Cycle Detected", "Task dependencies contain a cycle — cannot compute schedule.")
            return
        # durations
        dur = {tid: max(1, int(tasks_by_id[tid].get('est_days', 1))) for tid in tasks_by_id}
        earliest_start = {tid:0 for tid in tasks_by_id}
        earliest_finish = {}
        for n in topo:
            start = 0
            preds = tasks_by_id[n].get('dependencies', [])
            for p in preds:
                if p in earliest_finish:
                    start = max(start, earliest_finish[p])
            earliest_start[n] = start
            earliest_finish[n] = start + dur[n]
        project_end = max(earliest_finish.values()) if earliest_finish else 0
        # latest
        latest_finish = {tid: project_end for tid in tasks_by_id}
        latest_start = {}
        for n in reversed(topo):
            if succ.get(n):
                lf = min((latest_start[s] for s in succ[n]))
            else:
                lf = project_end
            latest_finish[n] = lf
            latest_start[n] = lf - dur[n]
        # slack and critical
        schedule = []
        critical = set()
        for tid in topo:
            es = earliest_start[tid]
            ef = earliest_finish[tid]
            ls = latest_start[tid]
            lf = latest_finish[tid]
            slack = ls - es
            is_crit = (slack == 0)
            if is_crit:
                critical.add(tid)
            schedule.append({
                'id': tid,
                'title': tasks_by_id[tid].get('title',''),
                'est': dur[tid],
                'start_day': es,
                'end_day': ef-1,
                'slack': slack,
                'critical': is_crit
            })
        # fill table
        self.table.setRowCount(len(schedule))
        try:
            base = datetime.date.fromisoformat(self.start_edit.text().strip())
        except Exception:
            base = datetime.date.today()
        for i,row in enumerate(schedule):
            self.table.setItem(i,0, QTableWidgetItem(row['id']))
            self.table.setItem(i,1, QTableWidgetItem(row['title']))
            self.table.setItem(i,2, QTableWidgetItem(str(row['est'])))
            sdate = (base + datetime.timedelta(days=row['start_day'])).isoformat()
            edate = (base + datetime.timedelta(days=row['end_day'])).isoformat()
            self.table.setItem(i,3, QTableWidgetItem(sdate))
            self.table.setItem(i,4, QTableWidgetItem(edate))
            self.table.setItem(i,5, QTableWidgetItem(str(row['slack'])))
            self.table.setItem(i,6, QTableWidgetItem("YES" if row['critical'] else ""))
            if row['critical']:
                for c in range(7):
                    item = self.table.item(i,c)
                    if item:
                        item.setBackground(QColor('#b62324'))
                        item.setForeground(QColor('#ffffff'))
        # notify network widget
        parent = getattr(self.parent(), 'parent', None)
        # set critical in network widget if present on the main window
        mw = None
        w = self
        while w and not isinstance(w, QMainWindow):
            w = w.parent()
        if isinstance(w, QMainWindow):
            mw = w
        if mw and hasattr(mw, 'network_widget'):
            mw.network_widget.set_critical(critical)
            mw.network_widget.generate_dot()
        self.last_schedule = schedule

    def export_schedule(self):
        if not hasattr(self, 'last_schedule'):
            QMessageBox.information(self, "Nothing to export", "Compute the schedule first.")
            return
        fname = QFileDialog.getSaveFileName(self, "Save Schedule JSON", os.path.join(os.getcwd(), "schedule.json"), "JSON Files (*.json);;All Files (*)")[0]
        if fname:
            try:
                with open(fname, 'w', encoding='utf-8') as f:
                    json.dump(self.last_schedule, f, indent=2)
                QMessageBox.information(self, "Saved", f"Schedule exported to {fname}")
            except Exception as e:
                QMessageBox.critical(self, "Export Error", str(e))

class DiagramEditorWidget(QWidget):
    def __init__(self, root_dir, main_window):
        super().__init__()
        self.root_dir = root_dir
        self.main_window = main_window
        self.current_filepath = None
        self.setup_ui()
        self.load_diagram_files()

    def setup_ui(self):
        layout = QHBoxLayout(self)
        layout.setContentsMargins(8, 8, 8, 8)

        side_layout = QVBoxLayout()
        side_layout.addWidget(QLabel("Diagram Files:"))
        
        self.file_list = QListWidget()
        self.file_list.setFixedWidth(220)
        self.file_list.currentTextChanged.connect(self.load_diagram_file)
        side_layout.addWidget(self.file_list)

        new_btn = QPushButton("+ New Diagram File")
        new_btn.setObjectName("secondaryBtn")
        new_btn.clicked.connect(self.create_new_diagram_file)
        side_layout.addWidget(new_btn)

        side_layout.addSpacing(10)
        side_layout.addWidget(QLabel("Insert Templates:"))

        seq_btn = QPushButton("Sequence Diagram")
        seq_btn.setObjectName("secondaryBtn")
        seq_btn.clicked.connect(lambda: self.insert_template("sequence"))
        side_layout.addWidget(seq_btn)

        cls_btn = QPushButton("Class Diagram")
        cls_btn.setObjectName("secondaryBtn")
        cls_btn.clicked.connect(lambda: self.insert_template("class"))
        side_layout.addWidget(cls_btn)

        layout.addLayout(side_layout)

        splitter = QSplitter(Qt.Orientation.Horizontal)

        editor_widget = QWidget()
        editor_lay = QVBoxLayout(editor_widget)
        editor_lay.setContentsMargins(0, 0, 0, 0)
        
        ed_header = QHBoxLayout()
        ed_header.addWidget(QLabel("Mermaid Code Editor"))
        ed_header.addStretch()
        
        save_btn = QPushButton("Save Diagram")
        save_btn.clicked.connect(self.save_current_diagram)
        ed_header.addWidget(save_btn)
        
        editor_lay.addLayout(ed_header)

        self.editor = QPlainTextEdit()
        self.editor.textChanged.connect(self.update_preview)
        editor_lay.addWidget(self.editor)

        splitter.addWidget(editor_widget)

        preview_widget = QWidget()
        preview_lay = QVBoxLayout(preview_widget)
        preview_lay.setContentsMargins(0, 0, 0, 0)
        preview_lay.addWidget(QLabel("Visual Diagram Preview"))

        if HAS_WEBENGINE:
            self.preview_view = QWebEngineView()
            preview_lay.addWidget(self.preview_view)
        else:
            self.preview_view = QTextBrowser()
            preview_lay.addWidget(self.preview_view)

        splitter.addWidget(preview_widget)

        layout.addWidget(splitter)

    def load_diagram_files(self):
        self.file_list.clear()
        diag_dir = os.path.join(self.root_dir, "docs", "diagrams")
        os.makedirs(diag_dir, exist_ok=True)
        files = [f for f in os.listdir(diag_dir) if f.endswith(".md")]
        for f in files:
            self.file_list.addItem(f)
        if files and not self.current_filepath:
            self.file_list.setCurrentRow(0)

    def load_diagram_file(self, filename):
        if not filename:
            return
        filepath = os.path.join(self.root_dir, "docs", "diagrams", filename)
        self.current_filepath = filepath
        if os.path.exists(filepath):
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            self.editor.blockSignals(True)
            self.editor.setPlainText(content)
            self.editor.blockSignals(False)
            self.update_preview()

    def save_current_diagram(self):
        if not self.current_filepath:
            return
        try:
            with open(self.current_filepath, 'w', encoding='utf-8') as f:
                f.write(self.editor.toPlainText())
            self.main_window.statusBar().showMessage(f"Saved diagram to {os.path.basename(self.current_filepath)}", 3000)
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save diagram:\n{str(e)}")

    def create_new_diagram_file(self):
        diag_dir = os.path.join(self.root_dir, "docs", "diagrams")
        os.makedirs(diag_dir, exist_ok=True)
        count = len(os.listdir(diag_dir)) + 1
        new_name = f"diagram_{count}.md"
        filepath = os.path.join(diag_dir, new_name)
        initial_content = f"# New Diagram\n\n```mermaid\nsequenceDiagram\n    participant A as Client\n    participant B as Server\n    A->>B: Packet\n```\n"
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(initial_content)
        self.load_diagram_files()
        items = self.file_list.findItems(new_name, Qt.MatchFlag.MatchExact)
        if items:
            self.file_list.setCurrentItem(items[0])

    def insert_template(self, template_type):
        if template_type == "sequence":
            code = "\n```mermaid\nsequenceDiagram\n    autonumber\n    participant C as Client\n    participant S as ChatServer\n    C->>S: LoginPacket\n    S-->>C: LoginResponsePacket\n```\n"
        elif template_type == "class":
            code = "\n```mermaid\nclassDiagram\n    class Service {\n        <<interface>>\n        +execute()\n    }\n```\n"
        self.editor.insertPlainText(code)

    def update_preview(self):
        raw_text = self.editor.toPlainText()
        mermaid_blocks = re.findall(r"```mermaid\s*(.*?)\s*```", raw_text, re.DOTALL)
        mermaid_code = mermaid_blocks[0] if mermaid_blocks else raw_text

        if HAS_WEBENGINE:
            html = f"""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
              <style>
                body {{ background-color: #0d1117; color: #c9d1d9; font-family: sans-serif; padding: 15px; }}
                .mermaid {{ background-color: #161b22; padding: 15px; border-radius: 8px; border: 1px solid #30363d; }}
              </style>
            </head>
            <body>
              <script>
                mermaid.initialize({{ startOnLoad: true, theme: 'dark' }});
              </script>
              <div class="mermaid">
                {mermaid_code}
              </div>
            </body>
            </html>
            """
            self.preview_view.setHtml(html)
        else:
            formatted = raw_text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            formatted = formatted.replace("```mermaid", "<div style='background-color:#161b22; border:1px solid #30363d; padding:12px; border-left:4px solid #58a6ff; margin:10px 0;'><b style='color:#58a6ff;'>[MERMAID CODE]</b><br/>").replace("```", "</div>")
            formatted = formatted.replace("\n", "<br/>")
            self.preview_view.setHtml(f"<div style='font-family:Segoe UI, sans-serif; color:#c9d1d9;'>{formatted}</div>")

class RoadmapBrowserWidget(QWidget):
    def __init__(self, root_dir):
        super().__init__()
        self.root_dir = root_dir
        self.setup_ui()

    def setup_ui(self):
        layout = QHBoxLayout(self)
        
        self.file_list = QListWidget()
        self.file_list.setFixedWidth(240)
        self.file_list.currentTextChanged.connect(self.load_selected_file)
        
        doc_files = ["ROADMAP.md", "shared_roadmap.md", "server_roadmap.md", "client_roadmap.md", ".gemini/GEMINI.md"]
        for df in doc_files:
            if os.path.exists(os.path.join(self.root_dir, df)):
                self.file_list.addItem(df)

        layout.addWidget(self.file_list)

        self.viewer = QTextBrowser()
        self.viewer.setOpenExternalLinks(True)
        layout.addWidget(self.viewer)

    def load_selected_file(self, filename):
        if not filename:
            return
        filepath = os.path.join(self.root_dir, filename)
        if os.path.exists(filepath):
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            html = self.simple_markdown_to_html(content)
            self.viewer.setHtml(html)

    def simple_markdown_to_html(self, text):
        html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        html = re.sub(r"^# (.*)", r"<h1 style='color:#58a6ff;'>\1</h1>", html, flags=re.M)
        html = re.sub(r"^## (.*)", r"<h2 style='color:#79c0ff;'>\1</h2>", html, flags=re.M)
        html = re.sub(r"^### (.*)", r"<h3 style='color:#a5d6ff;'>\1</h3>", html, flags=re.M)
        html = re.sub(r"\*\*(.*?)\*\*", r"<b>\1</b>", html)
        html = re.sub(r"`(.*?)`", r"<code style='background-color:#21262d; border:1px solid #30363d; padding:2px 6px; border-radius:4px; color:#f0f6fc;'>\1</code>", html)
        html = re.sub(r"^- \[x\] (.*)", r"<p style='color:#3fb950;'>✓ \1</p>", html, flags=re.M)
        html = re.sub(r"^- \[ \] (.*)", r"<p style='color:#d29922;'>☐ \1</p>", html, flags=re.M)
        html = html.replace("\n", "<br/>")
        return f"<div style='font-family:Segoe UI, sans-serif; color:#c9d1d9;'>{html}</div>"

class MainWindow(QMainWindow):
    def __init__(self, root_dir):
        super().__init__()
        self.root_dir = root_dir
        self.setWindowTitle("SimpleChatApp — Developer Workstation Suite")
        self.resize(1280, 860)
        self.setStyleSheet(STYLESHEET)

        self.setup_ui()
        self.setup_watcher()
        self.update_git_status()

    def setup_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        main_layout = QVBoxLayout(central)

        header = QHBoxLayout()
        title = QLabel("SIMPLE CHAT APP — DEV WORKSTATION SUITE")
        title.setStyleSheet("font-size: 18px; font-weight: bold; color: #f0f0f5; letter-spacing: 1px;")
        header.addWidget(title)

        header.addStretch()

        self.git_lbl = QLabel("Git: Checking...")
        self.git_lbl.setStyleSheet("color: #8b949e; font-size: 12px;")
        header.addWidget(self.git_lbl)

        sync_btn = QPushButton("Sync Workstation")
        sync_btn.setObjectName("secondaryBtn")
        sync_btn.clicked.connect(self.sync_all)
        header.addWidget(sync_btn)

        main_layout.addLayout(header)

        self.tabs = QTabWidget()
        main_layout.addWidget(self.tabs)

        json_path = os.path.join(self.root_dir, "docs", "tasks.json")
        self.kanban_board = KanbanBoardWidget(json_path, self)
        self.tabs.addTab(self.kanban_board, "Kanban Board")

        # Dependency roadmap (make it default open)
        self.dependency_roadmap = DependencyRoadmapWidget(self.kanban_board)
        self.tabs.addTab(self.dependency_roadmap, "Dependency Roadmap")

        self.diagram_editor = DiagramEditorWidget(self.root_dir, self)
        self.tabs.addTab(self.diagram_editor, "Diagram Creator & Editor")

        # Network / dependency view driven from current board data
        self.network_widget = NetworkWidget(self.kanban_board)
        self.tabs.addTab(self.network_widget, "Network View")

        self.roadmap_browser = RoadmapBrowserWidget(self.root_dir)
        self.tabs.addTab(self.roadmap_browser, "Roadmaps & Docs")

        # Open Dependency Roadmap tab by default
        self.tabs.setCurrentWidget(self.dependency_roadmap)

    def setup_watcher(self):
        self.watcher = QFileSystemWatcher(self)
        tasks_json = os.path.join(self.root_dir, "docs", "tasks.json")
        diag_dir = os.path.join(self.root_dir, "docs", "diagrams")
        
        watch_paths = []
        if os.path.exists(tasks_json):
            watch_paths.append(tasks_json)
        if os.path.exists(diag_dir):
            watch_paths.append(diag_dir)

        if watch_paths:
            self.watcher.addPaths(watch_paths)
            self.watcher.fileChanged.connect(self.on_file_auto_changed)
            self.watcher.directoryChanged.connect(self.on_file_auto_changed)

    def on_file_auto_changed(self, path):
        self.kanban_board.load_tasks()
        self.diagram_editor.load_diagram_files()
        self.statusBar().showMessage("Auto-refreshed workstation from disk change.", 3000)

    def sync_all(self):
        self.kanban_board.load_tasks()
        self.diagram_editor.load_diagram_files()
        self.update_git_status()
        self.statusBar().showMessage("Synced with disk and Git.", 3000)

    def update_git_status(self):
        try:
            res = subprocess.run(["git", "branch", "--show-current"], cwd=self.root_dir, capture_output=True, text=True)
            branch = res.stdout.strip() or "main"
            self.git_lbl.setText(f"Git Branch: {branch}")
        except Exception:
            self.git_lbl.setText("Git: N/A")

def main():
    app = QApplication(sys.argv)
    root_dir = os.getcwd()
    for _ in range(3):
        if os.path.exists(os.path.join(root_dir, "docs", "tasks.json")) or os.path.exists(os.path.join(root_dir, "ROADMAP.md")):
            break
        root_dir = os.path.dirname(root_dir)
    window = MainWindow(root_dir)
    window.show()
    sys.exit(app.exec())

if __name__ == "__main__":
    main()
