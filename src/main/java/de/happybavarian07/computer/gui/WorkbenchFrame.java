package de.happybavarian07.computer.gui;

import de.happybavarian07.computer.gui.controller.WorkbenchController;
import de.happybavarian07.computer.gui.controller.WorkbenchListener;
import de.happybavarian07.computer.gui.dialog.InstructionReferenceDialog;
import de.happybavarian07.computer.gui.panel.CpuControlPanel;
import de.happybavarian07.computer.gui.panel.DisassemblyPanel;
import de.happybavarian07.computer.gui.panel.EditorPanel;
import de.happybavarian07.computer.gui.panel.LogPanel;
import de.happybavarian07.computer.gui.panel.MemoryPanel;
import de.happybavarian07.computer.gui.panel.RegisterPanel;
import de.happybavarian07.computer.gui.panel.ToolsPanel;
import de.happybavarian07.computer.gui.panel.TracePanel;
import de.happybavarian07.computer.gui.panel.WatchPanel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Top-level shell: builds the toolbar/menu, wires every panel to a single
 * {@link WorkbenchController}, and lays them out so nothing has to fight
 * for vertical space. The old monolithic {@code ComputerWorkbench} stacked
 * seven panels in one {@code BoxLayout} column; here only two panels
 * (editor + its controls) are ever visible at once on the left, and
 * everything else lives in tabs on the right — so resizing the window
 * doesn't squeeze five unrelated panels down to a sliver.
 */
public final class WorkbenchFrame extends JFrame {
    private static final List<String> DEFAULT_EXAMPLES = List.of(
            "math-demo", "loop-demo", "stack-demo", "sum-loop-demo", "ram-multiplication-demo", "core-benchmark-demo"
    );

    private final WorkbenchController controller = new WorkbenchController();

    private final EditorPanel editorPanel = new EditorPanel();
    private final CpuControlPanel controlPanel = new CpuControlPanel();
    private final RegisterPanel registerPanel = new RegisterPanel();
    private final DisassemblyPanel disassemblyPanel = new DisassemblyPanel(controller);
    private final TracePanel tracePanel = new TracePanel();
    private final WatchPanel watchPanel = new WatchPanel(controller);
    private final MemoryPanel memoryPanel = new MemoryPanel(controller);
    private final LogPanel logPanel = new LogPanel();
    private final ToolsPanel toolsPanel = new ToolsPanel(controller);

    private final JComboBox<String> exampleSelector = new JComboBox<>();

    public WorkbenchFrame() {
        setTitle("TheComputer Workbench");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 960);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        buildMenuBar();
        buildToolbar();
        buildMainView();
        wireController();

        editorPanel.setSource("movi r1, 42\nmovi r2, 8\nadd r3, r1, r2\nhalt\n");
        controller.reset();
        refreshAll();
    }

    private void wireController() {
        controller.addListener(new WorkbenchListener() {
            @Override
            public void onStateChanged() {
                SwingUtilities.invokeLater(WorkbenchFrame.this::refreshAll);
            }

            @Override
            public void onLog(String message) {
                SwingUtilities.invokeLater(() -> logPanel.append(message));
            }
        });

        controlPanel.setResetAction(controller::reset);
        controlPanel.setStepAction(controller::step);
        controlPanel.setStepManyAction(controller::stepMany);
        controlPanel.setRunAction(stepsPerTick -> {
            if (controller.isRunning()) {
                return;
            }
            controlPanel.setClockText("Clock: measuring...");
            controller.startRun(stepsPerTick, this::updateClockLabel);
        });
        controlPanel.setStopAction(controller::stopRun);
    }

    private void updateClockLabel() {
        double hz = controller.currentRunHz();
        if (hz < 0) {
            return;
        }
        controlPanel.setClockText(String.format("Clock: %,.0f Hz (%,d steps)", hz, controller.currentRunSteps()));
    }

    private void refreshAll() {
        var cpu = controller.getMotherboard().getCpu();
        int pc = cpu.getSpecialRegisters().getPC().getAsInt();
        registerPanel.refresh(cpu.getRegisterFile(), cpu.getSpecialRegisters(), cpu.isHalted());
        disassemblyPanel.refresh(pc);
        watchPanel.refresh();
        memoryPanel.refresh();
        tracePanel.refresh(controller.getTraceEntries());
        editorPanel.highlightExecutionPointer(pc);
    }

    // --- layout ------------------------------------------------------

    private void buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

        JButton loadAsmButton = new JButton("Load asm");
        JButton loadBinaryButton = new JButton("Load bin");
        JButton saveAsmButton = new JButton("Save asm");
        JButton saveBinaryButton = new JButton("Save bin");
        JButton assembleButton = new JButton("Assemble & Load");
        JButton instructionsButton = new JButton("Instructions");
        JButton loadExampleButton = new JButton("Load example");

        loadAsmButton.addActionListener(e -> loadAssemblyFile());
        loadBinaryButton.addActionListener(e -> loadBinaryFile());
        saveAsmButton.addActionListener(e -> saveAssemblyFile());
        saveBinaryButton.addActionListener(e -> saveBinaryFile());
        assembleButton.addActionListener(e -> assembleCurrentProgram());
        instructionsButton.addActionListener(e -> InstructionReferenceDialog.open(this));
        loadExampleButton.addActionListener(e -> loadSelectedExample());

        toolbar.add(loadAsmButton);
        toolbar.add(loadBinaryButton);
        toolbar.add(saveAsmButton);
        toolbar.add(saveBinaryButton);
        toolbar.add(assembleButton);
        toolbar.add(instructionsButton);
        toolbar.add(new JSeparator(javax.swing.SwingConstants.VERTICAL));
        exampleSelector.setPrototypeDisplayValue("ram-multiplication-demo");
        for (String example : DEFAULT_EXAMPLES) {
            exampleSelector.addItem(example);
        }
        toolbar.add(exampleSelector);
        toolbar.add(loadExampleButton);

        add(toolbar, BorderLayout.NORTH);
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(menuItem("Load assembly...", e -> loadAssemblyFile()));
        fileMenu.add(menuItem("Load binary...", e -> loadBinaryFile()));
        fileMenu.addSeparator();
        int menuMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        fileMenu.add(menuItem("Save assembly...", KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask), e -> saveAssemblyFile()));
        fileMenu.add(menuItem("Save binary...", KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask | java.awt.event.InputEvent.SHIFT_DOWN_MASK), e -> saveBinaryFile()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", e -> dispose()));
        menuBar.add(fileMenu);

        JMenu runMenu = new JMenu("Run");
        runMenu.add(menuItem("Step", KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), e -> controller.step()));
        runMenu.add(menuItem("Run", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), e -> {
            if (!controller.isRunning()) {
                controlPanel.setClockText("Clock: measuring...");
                controller.startRun(256, this::updateClockLabel);
            }
        }));
        runMenu.add(menuItem("Stop", KeyStroke.getKeyStroke(KeyEvent.VK_F5, java.awt.event.InputEvent.SHIFT_DOWN_MASK), e -> controller.stopRun()));
        runMenu.add(menuItem("Reset", KeyStroke.getKeyStroke(KeyEvent.VK_F5, java.awt.event.InputEvent.CTRL_DOWN_MASK), e -> controller.reset()));
        menuBar.add(runMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.add(menuItem("Instruction reference", e -> InstructionReferenceDialog.open(this)));
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    private JMenuItem menuItem(String text, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(listener);
        return item;
    }

    private JMenuItem menuItem(String text, KeyStroke accelerator, java.awt.event.ActionListener listener) {
        JMenuItem item = menuItem(text, listener);
        item.setAccelerator(accelerator);
        return item;
    }

    private void buildMainView() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        leftPanel.add(controlPanel, BorderLayout.NORTH);
        leftPanel.add(editorPanel, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Registers", registerPanel);
        tabs.addTab("Disassembly & Trace", disassemblyAndTrace());
        tabs.addTab("Watch", watchPanel);
        tabs.addTab("Memory", memoryPanel);
        tabs.addTab("Log", logPanel);
        tabs.addTab("Tools", toolsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, tabs);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel disassemblyAndTrace() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, disassemblyPanel, tracePanel);
        split.setResizeWeight(0.6);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    // --- actions -------------------------------------------------------

    private void loadAssemblyFile() {
        Path file = chooseFile("asm");
        if (file == null) {
            return;
        }
        try {
            editorPanel.setSource(Files.readString(file, StandardCharsets.UTF_8));
            logPanel.append("Loaded assembly file: " + file);
        } catch (IOException ex) {
            showError("Could not read assembly file", ex);
        }
    }

    private void loadBinaryFile() {
        Path file = chooseFile("bin");
        if (file == null) {
            return;
        }
        try {
            controller.loadBinary(file);
        } catch (IOException ex) {
            showError("Could not load binary file", ex);
        }
    }

    private void loadSelectedExample() {
        String selected = (String) exampleSelector.getSelectedItem();
        if (selected == null) {
            return;
        }
        URL resource = getClass().getResource("/programs/" + selected + ".asm");
        if (resource == null) {
            showError("Missing example program", new IOException("Resource not found: /programs/" + selected + ".asm"));
            return;
        }
        try {
            editorPanel.setSource(Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8));
            logPanel.append("Loaded example: " + selected);
        } catch (IOException | URISyntaxException ex) {
            showError("Could not read example", ex);
        }
    }

    private void assembleCurrentProgram() {
        WorkbenchController.AssembleResult result = controller.assemble(editorPanel.getSource());
        if (!result.success()) {
            logPanel.append("Assembly failed: " + result.message());
            JOptionPane.showMessageDialog(this, result.message(), "Assembly failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAssemblyFile() {
        Path file = chooseSaveFile("asm");
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file, editorPanel.getSource(), StandardCharsets.UTF_8);
            logPanel.append("Saved assembly file: " + file);
        } catch (IOException ex) {
            showError("Could not save assembly file", ex);
        }
    }

    private void saveBinaryFile() {
        WorkbenchController.BinaryResult result = controller.assembleToBinary(editorPanel.getSource());
        if (!result.success()) {
            logPanel.append("Binary export failed: " + result.message());
            JOptionPane.showMessageDialog(this, result.message(), "Cannot save binary", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path file = chooseSaveFile("bin");
        if (file == null) {
            return;
        }
        try {
            Files.write(file, result.image());
            logPanel.append("Saved binary file: " + file + " (" + result.image().length + " bytes)");
        } catch (IOException ex) {
            showError("Could not save binary file", ex);
        }
    }

    private Path lastDirectory;

    private Path chooseFile(String extension) {
        JFileChooser chooser = new JFileChooser(lastDirectory == null ? new File(System.getProperty("user.dir")) : lastDirectory.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " files", extension));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            lastDirectory = file.toAbsolutePath().getParent();
            return file;
        }
        return null;
    }

    /** Appends the extension if missing and asks before overwriting. */
    private Path chooseSaveFile(String extension) {
        JFileChooser chooser = new JFileChooser(lastDirectory == null ? new File(System.getProperty("user.dir")) : lastDirectory.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " files", extension));
        chooser.setSelectedFile(new java.io.File("program." + extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path file = chooser.getSelectedFile().toPath();
        if (!file.getFileName().toString().toLowerCase().endsWith("." + extension)) {
            file = file.resolveSibling(file.getFileName() + "." + extension);
        }
        lastDirectory = file.toAbsolutePath().getParent();
        if (Files.exists(file)) {
            int choice = JOptionPane.showConfirmDialog(this, file.getFileName() + " already exists. Overwrite?", "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return file;
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        logPanel.append(title + ": " + ex.getMessage());
    }
}
