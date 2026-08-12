package de.happybavarian07.computer.gui;

import de.happybavarian07.computer.assembler.cli.AssemblerCli;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.registers.RegisterFile;
import de.happybavarian07.computer.system.Motherboard;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ComputerWorkbench extends JFrame {
    private static final List<String> DEFAULT_EXAMPLES = List.of("math-demo", "loop-demo", "stack-demo");

    private final Motherboard motherboard = new Motherboard();
    private final AssemblerCli assemblerCli = new AssemblerCli();
    private final AtomicBoolean runRequested = new AtomicBoolean(false);

    private final JTextArea editorBuffer = new JTextArea();
    private final JTextArea sourcePreview = new JTextArea();
    private final JTextArea stateArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JSpinner stepSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 1000, 1));
    private final JComboBox<String> exampleSelector = new JComboBox<>();

    private final DefaultTableModel registerModel = new DefaultTableModel(new Object[]{"Register", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable registerTable = new JTable(registerModel);

    private final DefaultTableModel memoryModel = new DefaultTableModel(new Object[]{"Addr", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable memoryTable = new JTable(memoryModel);
    private final JSpinner memoryBaseSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, Architecture.MEMORY_SIZE_BYTES - 4, 4));
    private final JSpinner memoryRowsSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(64, 8, 1024, 8));

    public ComputerWorkbench() {
        setTitle("TheComputer Workbench");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 920);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        initLookAndFeel();
        initToolbar();
        initMainView();
        initializeExamples();

        editorBuffer.setText("load r1, 42\nload r2, 8\nadd r1, r2\nhalt\n");
        syncSourcePreview();
        resetMachine();
        refreshState();
    }

    private void initLookAndFeel() {
        UIManager.put("Table.gridColor", new Color(220, 220, 220));
        UIManager.put("Table.selectionBackground", new Color(54, 95, 160));
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JButton loadAsmButton = new JButton("Load .asm");
        JButton loadBinaryButton = new JButton("Load .bin");
        JButton assembleButton = new JButton("Assemble to RAM");
        JButton openEditorButton = new JButton("Open Editor");
        JButton resetButton = new JButton("Reset");
        JButton stepButton = new JButton("Step");
        JButton stepManyButton = new JButton("Step N");
        JButton runButton = new JButton("Run");
        JButton stopButton = new JButton("Stop");
        JButton loadExampleButton = new JButton("Load example");

        loadAsmButton.addActionListener(e -> loadAssemblyFile());
        loadBinaryButton.addActionListener(e -> loadBinaryFile());
        assembleButton.addActionListener(e -> assembleCurrentProgram());
        openEditorButton.addActionListener(e -> openEditorWindow());
        resetButton.addActionListener(e -> resetMachine());
        stepButton.addActionListener(e -> stepOnce());
        stepManyButton.addActionListener(e -> stepMany());
        runButton.addActionListener(e -> runUntilHalt());
        stopButton.addActionListener(e -> stopRun());
        loadExampleButton.addActionListener(e -> loadSelectedExample());

        toolbar.add(loadAsmButton);
        toolbar.add(loadBinaryButton);
        toolbar.add(assembleButton);
        toolbar.add(openEditorButton);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));

        JPanel controlGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        controlGroup.add(resetButton);
        controlGroup.add(stepButton);
        controlGroup.add(new JLabel("Step count:"));
        controlGroup.add(stepSpinner);
        controlGroup.add(stepManyButton);
        controlGroup.add(runButton);
        controlGroup.add(stopButton);
        toolbar.add(controlGroup);

        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        exampleSelector.setPrototypeDisplayValue("stack-demo");
        toolbar.add(exampleSelector);
        toolbar.add(loadExampleButton);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initMainView() {
        sourcePreview.setEditable(false);
        sourcePreview.setText("Use 'Open Editor' to edit assembly source.\n");

        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Assembly source (buffer)"));
        leftPanel.add(new JScrollPane(sourcePreview), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));

        registerTable.setRowHeight(22);
        registerTable.setFillsViewportHeight(true);
        JPanel registersPanel = new JPanel(new BorderLayout());
        registersPanel.setBorder(BorderFactory.createTitledBorder("Registers"));
        registersPanel.add(new JScrollPane(registerTable), BorderLayout.CENTER);

        stateArea.setEditable(false);
        JPanel specialPanel = new JPanel(new BorderLayout());
        specialPanel.setBorder(BorderFactory.createTitledBorder("Special registers"));
        specialPanel.add(new JScrollPane(stateArea), BorderLayout.CENTER);

        JPanel topRight = new JPanel();
        topRight.setLayout(new BoxLayout(topRight, BoxLayout.Y_AXIS));
        topRight.add(registersPanel);
        topRight.add(specialPanel);

        memoryTable.setFillsViewportHeight(true);
        memoryTable.setRowHeight(20);
        TableColumnModel columns = memoryTable.getColumnModel();
        if (columns.getColumnCount() >= 3) {
            columns.getColumn(0).setPreferredWidth(70);
            columns.getColumn(1).setPreferredWidth(160);
            columns.getColumn(2).setPreferredWidth(120);
        }

        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory (scroll/page view)"));

        JPanel memoryControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        memoryControls.add(new JLabel("Base addr:"));
        memoryControls.add(memoryBaseSpinner);
        memoryControls.add(new JLabel("Rows:"));
        memoryControls.add(memoryRowsSpinner);
        JButton refreshMemoryBtn = new JButton("Refresh");
        refreshMemoryBtn.addActionListener(e -> refreshMemoryView());
        memoryControls.add(refreshMemoryBtn);

        memoryPanel.add(memoryControls, BorderLayout.NORTH);
        memoryPanel.add(new JScrollPane(memoryTable), BorderLayout.CENTER);

        logArea.setEditable(false);
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Execution log"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.36);

        JPanel rightComposite = new JPanel(new BorderLayout(8, 8));
        rightComposite.add(topRight, BorderLayout.NORTH);
        rightComposite.add(memoryPanel, BorderLayout.CENTER);
        rightComposite.add(logPanel, BorderLayout.SOUTH);

        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(rightComposite);

        add(mainSplit, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JLabel("TheComputer"));
        setJMenuBar(menuBar);
    }

    private void initializeExamples() {
        exampleSelector.removeAllItems();
        for (String example : DEFAULT_EXAMPLES) {
            exampleSelector.addItem(example);
        }
        exampleSelector.setSelectedIndex(0);
    }

    private void syncSourcePreview() {
        sourcePreview.setText(editorBuffer.getText());
    }

    private void openEditorWindow() {
        JFrame editorFrame = new JFrame("Assembly Editor");
        editorFrame.setSize(700, 700);
        editorFrame.setLocationRelativeTo(this);

        JTextArea editor = new JTextArea();
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setText(editorBuffer.getText().isBlank() ? "load r1, 42\nload r2, 8\nadd r1, r2\nhalt\n" : editorBuffer.getText());

        JButton saveBtn = new JButton("Save to Workbench");
        saveBtn.addActionListener(e -> {
            editorBuffer.setText(editor.getText());
            syncSourcePreview();
            logArea.append("Editor: saved content to workbench buffer.\n");
            editorFrame.dispose();
        });

        JButton assembleBtn = new JButton("Assemble & Load");
        assembleBtn.addActionListener(e -> {
            editorBuffer.setText(editor.getText());
            syncSourcePreview();
            assembleFromBuffer();
            editorFrame.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveBtn);
        buttons.add(assembleBtn);

        editorFrame.add(new JScrollPane(editor), BorderLayout.CENTER);
        editorFrame.add(buttons, BorderLayout.SOUTH);
        editorFrame.setVisible(true);
    }

    private void loadAssemblyFile() {
        Path file = chooseFile("asm");
        if (file == null) {
            return;
        }
        try {
            editorBuffer.setText(Files.readString(file, StandardCharsets.UTF_8));
            syncSourcePreview();
            logArea.append("Loaded assembly file: " + file + "\n");
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
            loadBinary(file);
            logArea.append("Loaded binary file: " + file + "\n");
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
            editorBuffer.setText(Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8));
            syncSourcePreview();
            logArea.append("Loaded example: " + selected + "\n");
        } catch (IOException | URISyntaxException ex) {
            showError("Could not read example", ex);
        }
    }

    private void assembleCurrentProgram() {
        String source = editorBuffer.getText();
        if (source == null || source.isBlank()) {
            logArea.append("No assembly source to assemble.\n");
            return;
        }
        assembleFromBuffer();
    }

    private void assembleFromBuffer() {
        String source = editorBuffer.getText();
        if (source == null || source.isBlank()) {
            logArea.append("No assembly source to assemble.\n");
            return;
        }
        try {
            Path inputFile = Files.createTempFile("computer-asm-", ".asm");
            Path outputFile = Files.createTempFile("computer-bin-", ".bin");
            Files.writeString(inputFile, source, StandardCharsets.UTF_8);

            int exitCode = assemblerCli.handleCommandInput(new String[]{
                    inputFile.toString(),
                    "-o",
                    outputFile.toString(),
                    "--overwrite"
            });

            if (exitCode != 0) {
                logArea.append("Assembly failed with exit code " + exitCode + ".\n");
                return;
            }

            loadBinary(outputFile);
            logArea.append("Assembly succeeded and machine code loaded into RAM.\n");
        } catch (IOException ex) {
            showError("Assembly failed", ex);
        }
    }

    private void resetMachine() {
        motherboard.reset();
        runRequested.set(false);
        logArea.append("Machine reset.\n");
        refreshState();
    }

    private void stepOnce() {
        try {
            motherboard.stepSystem();
            logArea.append("Step complete.\n");
            if (motherboard.getCpu().isHalted()) {
                logArea.append("CPU reached HALT.\n");
            }
        } catch (RuntimeException ex) {
            logArea.append("Execution error: " + ex.getMessage() + "\n");
            return;
        }
        refreshState();
    }

    private void stepMany() {
        int count = (Integer) stepSpinner.getValue();
        for (int i = 0; i < count; i++) {
            if (motherboard.getCpu().isHalted()) {
                logArea.append("CPU already halted before step " + (i + 1) + ".\n");
                break;
            }
            try {
                motherboard.stepSystem();
            } catch (RuntimeException ex) {
                logArea.append("Execution error after step " + (i + 1) + ": " + ex.getMessage() + "\n");
                break;
            }
        }
        logArea.append("Advanced " + count + " steps.\n");
        refreshState();
    }

    private void runUntilHalt() {
        runRequested.set(true);
        Thread runner = new Thread(() -> {
            while (runRequested.get() && !motherboard.getCpu().isHalted()) {
                try {
                    motherboard.stepSystem();
                    SwingUtilities.invokeLater(this::refreshState);
                    Thread.sleep(60);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (RuntimeException ex) {
                    SwingUtilities.invokeLater(() -> logArea.append("Execution error: " + ex.getMessage() + "\n"));
                    break;
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (motherboard.getCpu().isHalted()) {
                    logArea.append("Run complete: CPU halted.\n");
                }
                refreshState();
            });
        }, "computer-runner");
        runner.setDaemon(true);
        runner.start();
    }

    private void stopRun() {
        runRequested.set(false);
        logArea.append("Run stopped by user.\n");
    }

    private void loadBinary(Path file) throws IOException {
        motherboard.reset();
        byte[] binary = Files.readAllBytes(file);

        for (int offset = 0; offset < binary.length; offset += 4) {
            int raw = 0;
            for (int byteIndex = 0; byteIndex < 4 && offset + byteIndex < binary.length; byteIndex++) {
                raw |= (binary[offset + byteIndex] & 0xFF) << (byteIndex * 8);
            }
            motherboard.getSystemBus().write(new Address(offset), new Word(raw));
        }

        motherboard.getCpu().getSpecialRegisters().getPC().set(0);
        motherboard.getCpu().getSpecialRegisters().getSP().set(0xEFFF);
        refreshState();
    }

    private void refreshState() {
        refreshRegisters();
        refreshSpecialRegisters();
        refreshMemoryView();
    }

    private void refreshRegisters() {
        registerModel.setRowCount(0);
        RegisterFile registerFile = motherboard.getCpu().getRegisterFile();
        for (int i = 0; i < Architecture.GPR_COUNT; i++) {
            Word value = new Word();
            registerFile.read(i, value);
            registerModel.addRow(new Object[]{
                    "R" + i,
                    value.getAsInt(),
                    "0x" + Integer.toHexString(value.getAsInt() & 0xFFFF_FFFF).toUpperCase()
            });
        }
    }

    private void refreshSpecialRegisters() {
        var special = motherboard.getCpu().getSpecialRegisters();
        var pc = special.getPC();
        var sp = special.getSP();
        var ir = special.getIR();
        StringBuilder builder = new StringBuilder();
        builder.append("PC: ").append(pc.getAsInt()).append(" (0x").append(Integer.toHexString(pc.getAsInt()).toUpperCase()).append(")\n");
        builder.append("SP: ").append(sp.getAsInt()).append(" (0x").append(Integer.toHexString(sp.getAsInt()).toUpperCase()).append(")\n");
        builder.append("IR: ").append(ir.getAsInt()).append(" (0x").append(Integer.toHexString(ir.getAsInt() & 0xFFFF_FFFF).toUpperCase()).append(")\n");
        builder.append("Flags: Z=").append(special.isZero()).append(" N=").append(special.isNegative())
                .append(" C=").append(special.isCarry()).append(" V=").append(special.isOverflow()).append("\n");
        builder.append("Halted: ").append(motherboard.getCpu().isHalted()).append("\n");
        stateArea.setText(builder.toString());
    }

    private void refreshMemoryView() {
        memoryModel.setRowCount(0);
        int base = (Integer) memoryBaseSpinner.getValue();
        int rows = (Integer) memoryRowsSpinner.getValue();
        base = (base / 4) * 4;
        for (int i = 0; i < rows; i++) {
            int address = base + i * 4;
            if (address >= Architecture.MEMORY_SIZE_BYTES) {
                break;
            }
            Word word = new Word();
            try {
                motherboard.getSystemBus().read(new Address(address), word);
            } catch (RuntimeException ex) {
                word.set(0);
            }
            memoryModel.addRow(new Object[]{
                    String.format("%04X", address),
                    word.getAsInt(),
                    "0x" + Integer.toHexString(word.getAsInt() & 0xFFFF_FFFF).toUpperCase()
            });
        }
    }

    private Path chooseFile(String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " files", extension));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        logArea.append(title + ": " + ex.getMessage() + "\n");
    }
}
