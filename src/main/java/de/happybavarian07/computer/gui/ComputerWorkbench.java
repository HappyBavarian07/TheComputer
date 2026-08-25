package de.happybavarian07.computer.gui;

import de.happybavarian07.computer.gui.theme.Theme;
import de.happybavarian07.computer.assembler.cli.AssemblerCli;
import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.registers.RegisterFile;
import de.happybavarian07.computer.isa.Instruction;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.exceptions.assembler.LexerException;
import de.happybavarian07.computer.exceptions.assembler.ParserException;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.system.Motherboard;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputerWorkbench extends JFrame {
    private static final List<String> DEFAULT_EXAMPLES = List.of("math-demo", "loop-demo", "stack-demo", "sum-loop-demo", "ram-multiplication-demo");
    private static final List<String> COMPLETION_ITEMS = List.of(
            ".word", ".byte", ".ascii", ".org",
            "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "pc", "sp", "ir", "flags"
    );
    private static final Set<String> OPCODE_NAMES = Arrays.stream(de.happybavarian07.computer.isa.OpCode.values())
            .map(opCode -> opCode.name().toLowerCase())
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(?:0x[0-9a-fA-F_]+|0b[01_]+|[0-9_]+)\\b");
    private static final Pattern REGISTER_PATTERN = Pattern.compile("\\b(?:r\\d+|pc|sp|ir|flags)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[\\s]*[A-Za-z_][\\w]*:");
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^[\\s]*\\.[A-Za-z_][\\w.]*");
    private static final Pattern OPCODE_PATTERN = Pattern.compile("\\b(" + String.join("|", OPCODE_NAMES) + ")\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_REFERENCE_PATTERN = Pattern.compile("(?m)^\\s*([A-Za-z_][\\w]*)\\s*:");
    private static final Map<OperandMapping, String> OPERAND_SYNTAX = Map.of(
            OperandMapping.NONE, "",
            OperandMapping.RD_RS, "rd, rs",
            OperandMapping.RD_ONLY, "rd",
            OperandMapping.RS_ONLY, "rs",
            OperandMapping.RD_IMM16, "rd, imm16",
            OperandMapping.IMM16_RD, "imm16, rd",
            OperandMapping.IMM16_ONLY, "imm16"
    );

    private final Motherboard motherboard = new Motherboard();
    private final AssemblerCli assemblerCli = new AssemblerCli();
    private final AtomicBoolean runRequested = new AtomicBoolean(false);

    private final JTextPane sourceEditor = new JTextPane();
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
    private final DefaultTableModel disassemblyModel = new DefaultTableModel(new Object[]{"Address", "Word", "Instruction"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable disassemblyTable = new JTable(disassemblyModel);
    private final DefaultTableModel watchModel = new DefaultTableModel(new Object[]{"Target", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable watchTable = new JTable(watchModel);

    private final DefaultTableModel memoryModel = new DefaultTableModel(new Object[]{"Address", "Value", "Hex"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }
    };
    private final JTable memoryTable = new JTable(memoryModel);
    private boolean memoryRefreshing;
    private final JSpinner memoryBaseSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, Architecture.MEMORY_SIZE_BYTES - 4, 4));
    private final JSpinner memoryRowsSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(64, 4, 256, 4));
    private final JTextField memoryJumpField = new JTextField(10);
    private final JTextField memoryFindField = new JTextField(10);
    private final JTextField watchField = new JTextField("r0, r1, sp, 0x0000, 0x0004", 28);
    private final JTextArea traceArea = new JTextArea(7, 40);
    private final JTextArea sourceGutter = new JTextArea();
    private final Map<Integer, SourceSpan> sourceSpanByAddress = new HashMap<>();
    private boolean highlightingSource;
    private boolean highlightPending;
    private boolean diagnosticsPending;
    private boolean diagnosticsDirty;
    private javax.swing.Timer runTimer;
    private final JSpinner speedSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(256, 1, 100000, 64));
    private final JLabel clockLabel = new JLabel("Clock: idle");
    private final JLabel coreClockLabel = new JLabel("Core: --");
    private long runStartNanos;
    private long runStepsTotal;
    private int errorStartOffset = -1;
    private int errorLength = 0;
    private final JLabel diagnosticsStageLabel = new JLabel();
    private final JLabel diagnosticsTokensLabel = new JLabel();
    private final JLabel diagnosticsParsedLabel = new JLabel();
    private final JLabel diagnosticsResolvedLabel = new JLabel();
    private final JLabel diagnosticsEncodedLabel = new JLabel();
    private final JLabel diagnosticsMessageLabel = new JLabel();
    private final JTextField pcField = new JTextField();
    private final JTextField spField = new JTextField();
    private final JTextField irField = new JTextField();
    private final JLabel flagZ = flagLabel("Z");
    private final JLabel flagN = flagLabel("N");
    private final JLabel flagC = flagLabel("C");
    private final JLabel flagV = flagLabel("V");
    private final JLabel haltedValue = new JLabel();
    private final Deque<String> traceEntries = new ArrayDeque<>();
    private final JPopupMenu completionPopup = new JPopupMenu();
    private final DefaultTableModel completionModel = new DefaultTableModel(new Object[]{"Completion", "Usage", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable completionTable = new JTable(completionModel);
    private final JLabel completionDetailLabel = new JLabel();

    public ComputerWorkbench() {
        setTitle("TheComputer Workbench");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 960);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initLookAndFeel();
        initToolbar();
        initMainView();
        initializeExamples();

        sourceEditor.setText("load r1, 42\nload r2, 8\nadd r1, r2\nhalt\n");
        logArea.setText("Machine reset\n");
        resetMachine();
        refreshState();
        refreshAssemblyDiagnostics();
    }

    private void initLookAndFeel() {
        UIManager.put("Panel.background", new Color(20, 23, 29));
        UIManager.put("ScrollPane.background", new Color(20, 23, 29));
        UIManager.put("Viewport.background", new Color(20, 23, 29));
        UIManager.put("TextArea.background", new Color(30, 34, 42));
        UIManager.put("TextArea.foreground", new Color(214, 219, 228));
        UIManager.put("TextArea.caretForeground", new Color(91, 143, 214));
        UIManager.put("TextField.background", new Color(30, 34, 42));
        UIManager.put("TextField.foreground", new Color(214, 219, 228));
        UIManager.put("Table.gridColor", new Color(54, 60, 71));
        UIManager.put("Table.selectionBackground", new Color(91, 143, 214));
        UIManager.put("Table.background", new Color(20, 23, 29));
        UIManager.put("Table.foreground", new Color(214, 219, 228));
        UIManager.put("Label.foreground", new Color(214, 219, 228));
        UIManager.put("Button.background", new Color(46, 52, 63));
        UIManager.put("Button.foreground", new Color(232, 236, 242));
        UIManager.put("Button.focus", new Color(91, 143, 214));
        UIManager.put("TextPane.background", new Color(16, 18, 23));
        UIManager.put("TextPane.foreground", new Color(222, 227, 235));
        UIManager.put("TextPane.caretForeground", new Color(91, 143, 214));
        UIManager.put("TitledBorder.titleColor", new Color(127, 168, 221));
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font(Font.MONOSPACED, Font.PLAIN, 12));
        UIManager.put("Table.font", new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void initToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JButton loadAsmButton = new JButton("Load asm");
        JButton loadBinaryButton = new JButton("Load bin");
        JButton assembleButton = new JButton("Assemble");
        JButton openEditorButton = new JButton("Editor");
        JButton instructionsButton = new JButton("Instructions");
        JButton loadExampleButton = new JButton("Load example");

        loadAsmButton.addActionListener(e -> loadAssemblyFile());
        loadBinaryButton.addActionListener(e -> loadBinaryFile());
        assembleButton.addActionListener(e -> assembleCurrentProgram());
        openEditorButton.addActionListener(e -> openEditorWindow());
        instructionsButton.addActionListener(e -> openInstructionReferenceWindow());
        loadExampleButton.addActionListener(e -> loadSelectedExample());

        toolbar.add(loadAsmButton);
        toolbar.add(loadBinaryButton);
        toolbar.add(assembleButton);
        toolbar.add(openEditorButton);
        toolbar.add(instructionsButton);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        exampleSelector.setPrototypeDisplayValue("stack-demo");
        toolbar.add(exampleSelector);
        toolbar.add(loadExampleButton);

        add(toolbar, BorderLayout.NORTH);
    }

    private void initMainView() {
        JPanel controlsPanel = new JPanel(new BorderLayout(8, 8));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("CPU controls"));

        JButton resetButton = new JButton("Reset");
        JButton stepButton = new JButton("Step");
        JButton stepManyButton = new JButton("Step N");
        JButton runButton = new JButton("Run");
        JButton stopButton = new JButton("Stop");
        resetButton.addActionListener(e -> resetMachine());
        stepButton.addActionListener(e -> stepOnce());
        stepManyButton.addActionListener(e -> stepMany());
        runButton.addActionListener(e -> runUntilHalt());
        stopButton.addActionListener(e -> stopRun());
        stepButton.setPreferredSize(new Dimension(116, 34));
        stepManyButton.setPreferredSize(new Dimension(116, 34));
        runButton.setPreferredSize(new Dimension(116, 34));
        stopButton.setPreferredSize(new Dimension(116, 34));
        resetButton.setPreferredSize(new Dimension(116, 34));

        JPanel primaryControls = new JPanel(new java.awt.GridLayout(1, 3, 8, 8));
        primaryControls.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        primaryControls.add(stepButton);
        primaryControls.add(runButton);
        primaryControls.add(stopButton);

        JPanel secondaryControls = new JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        secondaryControls.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        secondaryControls.add(resetButton);
        secondaryControls.add(stepManyButton);

        JPanel stepConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        stepConfig.add(new JLabel("Step count:"));
        stepConfig.add(stepSpinner);
        stepConfig.add(new JLabel("Steps/tick:"));
        stepConfig.add(speedSpinner);

        JButton benchButton = new JButton("Benchmark core");
        benchButton.addActionListener(e -> benchmarkCore());
        JPanel clockPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        clockPanel.add(clockLabel);
        clockPanel.add(coreClockLabel);
        clockPanel.add(benchButton);

        JPanel southStack = new JPanel();
        southStack.setLayout(new BoxLayout(southStack, BoxLayout.Y_AXIS));
        southStack.add(stepConfig);
        southStack.add(clockPanel);

        JPanel controlBody = new JPanel(new BorderLayout());
        controlBody.add(primaryControls, BorderLayout.NORTH);
        controlBody.add(secondaryControls, BorderLayout.CENTER);
        controlBody.add(southStack, BorderLayout.SOUTH);
        controlsPanel.add(controlBody, BorderLayout.CENTER);

        JPanel sourcePanel = new JPanel(new BorderLayout(6, 6));
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Assembly source"));
        configureSourceEditor(sourceEditor);
        JScrollPane sourceScrollPane = new JScrollPane(sourceEditor);
        sourceScrollPane.getViewport().setBackground(new Color(20, 23, 29));
        sourceScrollPane.setBorder(BorderFactory.createEmptyBorder());
        configureSourceGutter(sourceGutter, sourceScrollPane);
        sourcePanel.add(sourceScrollPane, BorderLayout.CENTER);
        sourcePanel.add(createDiagnosticsPanel(), BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        leftPanel.add(controlsPanel, BorderLayout.NORTH);
        leftPanel.add(sourcePanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JPanel registersPanel = new JPanel(new BorderLayout());
        registersPanel.setBorder(BorderFactory.createTitledBorder("Registers"));
        registerTable.setRowHeight(22);
        registerTable.setFillsViewportHeight(true);
        registersPanel.add(new JScrollPane(registerTable), BorderLayout.CENTER);

        JPanel specialPanel = new JPanel(new BorderLayout());
        specialPanel.setBorder(BorderFactory.createTitledBorder("Special registers"));
        specialPanel.add(createSpecialRegistersPanel(), BorderLayout.CENTER);

        JPanel disassemblyPanel = createDisassemblyPanel();
        JPanel watchPanel = createWatchPanel();
        JPanel tracePanel = createTracePanel();

        configureMemoryTable();

        JPanel memoryPanel = new JPanel(new BorderLayout(6, 6));
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory"));
        memoryPanel.setBackground(new Color(20, 23, 29));
        JPanel memoryControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        memoryControls.setBackground(new Color(20, 23, 29));
        memoryControls.add(new JLabel("Base:"));
        memoryControls.add(memoryBaseSpinner);
        memoryControls.add(new JLabel("Rows:"));
        memoryControls.add(memoryRowsSpinner);
        memoryControls.add(new JLabel("Jump:"));
        memoryControls.add(memoryJumpField);
        JButton jumpMemoryButton = new JButton("Go");
        jumpMemoryButton.addActionListener(e -> jumpToMemoryAddress());
        memoryControls.add(jumpMemoryButton);
        memoryControls.add(new JLabel("Find:"));
        memoryControls.add(memoryFindField);
        JButton findMemoryButton = new JButton("Search");
        findMemoryButton.addActionListener(e -> searchMemoryValue());
        memoryControls.add(findMemoryButton);
        JButton refreshMemoryButton = new JButton("Refresh");
        refreshMemoryButton.addActionListener(e -> refreshMemoryView());
        memoryControls.add(refreshMemoryButton);
        memoryPanel.add(memoryControls, BorderLayout.NORTH);
        memoryPanel.add(new JScrollPane(memoryTable), BorderLayout.CENTER);

        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(BorderFactory.createTitledBorder("Execution log"));
        logArea.setEditable(false);
        logsPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        rightPanel.add(registersPanel);
        rightPanel.add(specialPanel);
        rightPanel.add(disassemblyPanel);
        rightPanel.add(watchPanel);
        rightPanel.add(tracePanel);
        rightPanel.add(memoryPanel);
        rightPanel.add(logsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.38);
        splitPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(splitPane, BorderLayout.CENTER);

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

    private void openEditorWindow() {
        JFrame editorFrame = new JFrame("Assembler editor");
        editorFrame.setSize(700, 720);
        editorFrame.setLocationRelativeTo(this);

        JTextPane editor = new JTextPane();
        configureSourceEditor(editor);
        editor.setText(sourceEditor.getText());

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            sourceEditor.setText(editor.getText());
            logArea.append("Assembly source updated.\n");
            editorFrame.dispose();
        });

        JButton assembleBtn = new JButton("Assemble & Load");
        assembleBtn.addActionListener(e -> {
            sourceEditor.setText(editor.getText());
            assembleCurrentProgram();
            editorFrame.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveBtn);
        buttons.add(assembleBtn);

        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.getViewport().setBackground(new Color(20, 23, 29));
        editorScrollPane.setBorder(BorderFactory.createEmptyBorder());
        editorFrame.add(editorScrollPane, BorderLayout.CENTER);
        editorFrame.add(buttons, BorderLayout.SOUTH);
        editorFrame.setVisible(true);
    }

    private void openInstructionReferenceWindow() {
        JFrame frame = new JFrame("Instruction reference");
        frame.setSize(980, 620);
        frame.setLocationRelativeTo(this);
        frame.setAlwaysOnTop(true);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Opcode", "Syntax", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (OpCode opCode : OpCode.values()) {
            model.addRow(new Object[]{
                    opCode.name(),
                    buildSyntax(opCode),
                    describeOpcode(opCode)
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(20, 23, 29));
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void loadAssemblyFile() {
        Path file = chooseFile("asm");
        if (file == null) {
            return;
        }
        try {
            sourceEditor.setText(Files.readString(file, StandardCharsets.UTF_8));
            logArea.append("Loaded assembly file: " + file + "\n");
            refreshAssemblyDiagnostics();
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
            refreshAssemblyDiagnostics();
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
            sourceEditor.setText(Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8));
            logArea.append("Loaded example: " + selected + "\n");
            refreshAssemblyDiagnostics();
        } catch (IOException | URISyntaxException ex) {
            showError("Could not read example", ex);
        }
    }

    private void assembleCurrentProgram() {
        String source = sourceEditor.getText();
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
            refreshAssemblyDiagnostics();
        } catch (IOException ex) {
            showError("Assembly failed", ex);
        }
    }

    private void resetMachine() {
        motherboard.reset();
        runRequested.set(false);
        traceEntries.clear();
        sourceSpanByAddress.clear();
        refreshTraceView();
        logArea.append("Machine reset.\n");
        refreshState();
        refreshAssemblyDiagnostics();
    }

    private void stepOnce() {
        int pcBefore = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
        int rawBefore = readMemoryWord(pcBefore);
        try {
            motherboard.stepSystem();
            logArea.append("Step complete.\n");
            if (motherboard.getCpu().isHalted()) {
                logArea.append("CPU reached HALT.\n");
            }
        } catch (RuntimeException ex) {
            logArea.append("Execution error: " + ex.getMessage() + "\n");
            recordTrace("step", pcBefore, rawBefore, pcBefore);
            return;
        }
        recordTrace("step", pcBefore, rawBefore, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
        refreshState();
    }

    private void stepMany() {
        int count = (Integer) stepSpinner.getValue();
        for (int i = 0; i < count; i++) {
            if (motherboard.getCpu().isHalted()) {
                logArea.append("CPU already halted before step " + (i + 1) + ".\n");
                break;
            }
            int pcBefore = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            int rawBefore = readMemoryWord(pcBefore);
            try {
                motherboard.stepSystem();
                recordTrace("step", pcBefore, rawBefore, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
            } catch (RuntimeException ex) {
                logArea.append("Execution error after step " + (i + 1) + ": " + ex.getMessage() + "\n");
                recordTrace("step", pcBefore, rawBefore, pcBefore);
                break;
            }
        }
        logArea.append("Advanced " + count + " steps.\n");
        refreshState();
    }

    private void runUntilHalt() {
        if (runTimer != null && runTimer.isRunning()) {
            return;
        }
        runRequested.set(true);
        runStartNanos = System.nanoTime();
        runStepsTotal = 0;
        clockLabel.setText("Clock: measuring...");
        runTimer = new javax.swing.Timer(30, e -> {
            if (!runRequested.get() || motherboard.getCpu().isHalted()) {
                runTimer.stop();
                if (motherboard.getCpu().isHalted()) {
                    logArea.append("Run complete: CPU halted.\n");
                }
                refreshState();
                updateClockLabel();
                return;
            }
            int batch = (Integer) speedSpinner.getValue();
            int pcBefore = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            int rawBefore = readMemoryWord(pcBefore);
            int executed = 0;
            String error = null;
            try {
                for (int i = 0; i < batch; i++) {
                    if (motherboard.getCpu().isHalted()) {
                        break;
                    }
                    motherboard.stepSystem();
                    executed++;
                }
            } catch (RuntimeException ex) {
                error = ex.getMessage();
            }
            runStepsTotal += executed;
            recordTrace("run", pcBefore, rawBefore, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
            refreshState();
            updateClockLabel();
            if (error != null) {
                runTimer.stop();
                logArea.append("Execution error: " + error + "\n");
            }
        });
        runTimer.setInitialDelay(0);
        runTimer.start();
    }

    private void stopRun() {
        runRequested.set(false);
        if (runTimer != null) {
            runTimer.stop();
        }
        updateClockLabel();
        logArea.append("Run stopped by user.\n");
    }

    private void updateClockLabel() {
        long elapsed = System.nanoTime() - runStartNanos;
        if (elapsed <= 0) {
            return;
        }
        double hz = runStepsTotal / (elapsed / 1_000_000_000.0);
        clockLabel.setText(String.format("Clock: %,.0f Hz (%,d steps)", hz, runStepsTotal));
    }

    /**
     * Measures raw core throughput: replays the current memory image on a
     * throwaway Motherboard from PC=0 with no GUI refresh, so the number
     * reflects the CPU core alone (not Swing rendering). Reset the machine
     * first if the live program has already run past its start.
     */
    private void benchmarkCore() {
        final int[] image = new int[Architecture.MEMORY_FREE_END / 4];
        for (int i = 0; i < image.length; i++) {
            image[i] = readMemoryWord(i * 4);
        }
        coreClockLabel.setText("Core: benchmarking...");
        Thread worker = new Thread(() -> {
            final int stepCap = 5_000_000;
            final int startSp = 0xEFFF;
            Motherboard bench = new Motherboard();
            for (int w = 0; w < 3; w++) {
                runProgramOnce(bench, image, startSp, stepCap);
            }
            long steps = 0;
            boolean capped = false;
            long start = System.nanoTime();
            while (System.nanoTime() - start < 300_000_000L) {
                long s = runProgramOnce(bench, image, startSp, stepCap);
                if (s < 0) {
                    capped = true;
                    steps += stepCap;
                } else {
                    steps += s;
                }
            }
            long elapsed = System.nanoTime() - start;
            double hz = steps / (elapsed / 1_000_000_000.0);
            final boolean fCapped = capped;
            SwingUtilities.invokeLater(() -> coreClockLabel.setText(
                    String.format("Core: %,.0f Hz%s", hz, fCapped ? " (loop capped)" : "")));
        }, "core-benchmark");
        worker.setDaemon(true);
        worker.start();
    }

    private long runProgramOnce(Motherboard bench, int[] image, int startSp, int cap) {
        bench.reset();
        for (int i = 0; i < image.length; i++) {
            bench.getSystemBus().write(new Address(i * 4), new Word(image[i]));
        }
        bench.getCpu().getSpecialRegisters().getPC().set(0);
        bench.getCpu().getSpecialRegisters().getSP().set(startSp);
        long steps = 0;
        try {
            while (!bench.getCpu().isHalted() && steps < cap) {
                bench.stepSystem();
                steps++;
            }
        } catch (RuntimeException ex) {
            return steps;
        }
        if (steps >= cap && !bench.getCpu().isHalted()) {
            return -1;
        }
        return steps;
    }

    private void writeWord(int address, int value) {
        motherboard.getSystemBus().write(new Address(address), new Word(value));
        refreshState();
    }

    private void loadBinary(Path file) throws IOException {
        motherboard.reset();
        traceEntries.clear();
        sourceSpanByAddress.clear();
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
        refreshDisassemblyView();
        refreshWatchView();
        refreshMemoryView();
        refreshExecutionPointerView();
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
                    "0x" + Integer.toHexString(value.getAsInt()).toUpperCase()
            });
        }
    }

    private void refreshSpecialRegisters() {
        var special = motherboard.getCpu().getSpecialRegisters();
        var pc = special.getPC();
        var sp = special.getSP();
        var ir = special.getIR();
        pcField.setText(formatRegisterValue(pc.getAsInt()));
        spField.setText(formatRegisterValue(sp.getAsInt()));
        irField.setText(formatRegisterValue(ir.getAsInt()));
        updateFlag(flagZ, special.isZero());
        updateFlag(flagN, special.isNegative());
        updateFlag(flagC, special.isCarry());
        updateFlag(flagV, special.isOverflow());
        haltedValue.setText(motherboard.getCpu().isHalted() ? "HALTED" : "RUNNING");
    }

    private void refreshDisassemblyView() {
        disassemblyModel.setRowCount(0);
        int pc = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
        int base = Math.max(0, pc - 16);
        base = (base / 4) * 4;
        int rows = 12;
        int selectedRow = -1;

        for (int i = 0; i < rows; i++) {
            int address = base + i * 4;
            if (address >= Architecture.MEMORY_SIZE_BYTES) {
                break;
            }
            int raw = readMemoryWord(address);
            Instruction instruction = decodeInstruction(raw);
            disassemblyModel.addRow(new Object[]{
                    String.format("0x%04X", address),
                    String.format("0x%08X", raw),
                    formatInstruction(instruction)
            });
            if (address == pc) {
                selectedRow = i;
            }
        }

        if (selectedRow >= 0 && selectedRow < disassemblyTable.getRowCount()) {
            disassemblyTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    private void refreshWatchView() {
        watchModel.setRowCount(0);
        String[] tokens = watchField.getText() == null ? new String[0] : watchField.getText().split("[,\\s]+");
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalized = token.trim();
            Integer value = resolveWatchValue(normalized);
            watchModel.addRow(new Object[]{
                    normalized,
                    value == null ? "invalid" : Integer.toUnsignedString(value),
                    value == null ? "invalid" : String.format("0x%08X", value)
            });
        }
    }

    private void refreshTraceView() {
        StringBuilder builder = new StringBuilder();
        for (String entry : traceEntries) {
            builder.append(entry).append('\n');
        }
        traceArea.setText(builder.toString());
    }

    private void refreshExecutionPointerView() {
        int pc = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
        SourceSpan span = sourceSpanByAddress.get(pc);
        sourceGutter.setText(buildSourceGutterText(span == null ? 0 : span.startLine()));
    }

    private String buildSourceGutterText(int activeLine) {
        String source = sourceEditor.getText();
        if (source == null || source.isEmpty()) {
            return " ";
        }
        String[] lines = source.split("\\R", -1);
        int width = Integer.toString(lines.length).length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            builder.append(lineNumber == activeLine ? "→" : " ");
            builder.append(' ');
            builder.append(String.format("%" + width + "d", lineNumber));
            builder.append('\n');
        }
        return builder.toString();
    }

    private void configureMemoryTable() {
        memoryTable.setRowHeight(22);
        memoryTable.setFont(Theme.MONO);
        memoryTable.setFillsViewportHeight(true);
        memoryTable.getTableHeader().setReorderingAllowed(false);
        memoryModel.addTableModelListener(e -> {
            if (memoryRefreshing) {
                return;
            }
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE
                    && e.getColumn() == 1 && e.getFirstRow() >= 0) {
                onMemoryCellEdited(e.getFirstRow());
            }
        });
    }

    /**
     * Rebuilds row structure only when the visible window (base/rows) changes;
     * otherwise updates the existing rows in place. No Swing components are
     * created per refresh, so stepping stays cheap.
     */
    private void refreshMemoryView() {
        int base = (Integer) memoryBaseSpinner.getValue();
        int rows = (Integer) memoryRowsSpinner.getValue();
        base = (base / 4) * 4;
        int maxAddresses = Math.max(0, Math.min(rows, (Architecture.MEMORY_SIZE_BYTES - base) / 4));

        memoryRefreshing = true;
        try {
            if (memoryModel.getRowCount() != maxAddresses) {
                memoryModel.setRowCount(0);
                for (int i = 0; i < maxAddresses; i++) {
                    memoryModel.addRow(new Object[]{"", "", ""});
                }
            }
            for (int i = 0; i < maxAddresses; i++) {
                int address = base + i * 4;
                int value = readMemoryWord(address);
                memoryModel.setValueAt(String.format("0x%04X", address), i, 0);
                memoryModel.setValueAt(Integer.toUnsignedString(value), i, 1);
                memoryModel.setValueAt("0x" + Integer.toHexString(value).toUpperCase(), i, 2);
            }
        } finally {
            memoryRefreshing = false;
        }
    }

    private void onMemoryCellEdited(int row) {
        int base = ((Integer) memoryBaseSpinner.getValue() / 4) * 4;
        int address = base + row * 4;
        Object raw = memoryModel.getValueAt(row, 1);
        String normalized = raw == null ? "" : raw.toString().trim();
        try {
            int parsed;
            if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                parsed = Integer.parseUnsignedInt(normalized.substring(2), 16);
            } else if (normalized.startsWith("-") || normalized.matches("[0-9]+")) {
                parsed = Integer.parseInt(normalized, 10);
            } else {
                parsed = Integer.parseUnsignedInt(normalized, 16);
            }
            writeWord(address, parsed);
        } catch (NumberFormatException ex) {
            refreshMemoryView();
        }
    }

    private int readMemoryWord(int address) {
        Word word = new Word();
        try {
            motherboard.getSystemBus().read(new Address(address), word);
        } catch (RuntimeException ex) {
            word.set(0);
        }
        return word.getAsInt();
    }

    private Instruction decodeInstruction(int rawWord) {
        Instruction instruction = new Instruction();
        try {
            motherboard.getCpu().getInstructionDecoder().decode(new Word(rawWord), instruction);
            return instruction;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String formatInstruction(Instruction instruction) {
        if (instruction == null || instruction.opCode() == null) {
            return "data";
        }
        OpCode opCode = instruction.opCode();
        return switch (opCode) {
            case NOP, HALT -> opCode.name().toLowerCase();
            case MOV, LOADR, STORER, ADD, SUB, AND, OR, XOR -> opCode.name().toLowerCase() + " r" + instruction.regDestIndex() + ", r" + instruction.regSourceIndex();
            case LOAD -> opCode.name().toLowerCase() + " r" + instruction.regDestIndex() + ", " + formatImmediate(instruction.immediateAddr());
            case STORE -> opCode.name().toLowerCase() + " " + formatImmediate(instruction.immediateAddr()) + ", r" + instruction.regDestIndex();
            case NOT, POP -> opCode.name().toLowerCase() + " r" + instruction.regDestIndex();
            case SHL, SHR -> opCode.name().toLowerCase() + " r" + instruction.regDestIndex() + ", " + formatImmediate(instruction.immediateAddr());
            case JMP, JZ, JNZ -> opCode.name().toLowerCase() + " " + formatImmediate(instruction.immediateAddr());
            case PUSH -> opCode.name().toLowerCase() + " r" + instruction.regSourceIndex();
        };
    }

    private String formatImmediate(int value) {
        return String.format("0x%04X", value & 0xFFFF);
    }

    private String tooltipForSourcePosition(JTextPane editor, java.awt.Point point) {
        int offset = editor.viewToModel2D(point);
        if (offset < 0) {
            return null;
        }
        String source = editor.getText();
        if (source == null || source.isEmpty()) {
            return null;
        }
        int lineStart = source.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        int lineEnd = source.indexOf('\n', offset);
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        String line = source.substring(lineStart, lineEnd).trim();
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
            return null;
        }
        if (line.endsWith(":")) {
            return "Label";
        }
        if (DIRECTIVE_PATTERN.matcher(line).find()) {
            return "Directive";
        }
        String opToken = line.split("\\s+", 2)[0].replace(":", "");
        OpCode opCode = OpCode.valueOfNullable(opToken.toUpperCase());
        if (opCode != null) {
            return opCode.name().toLowerCase() + " - " + buildSyntax(opCode) + " - " + describeOpcode(opCode);
        }
        if (REGISTER_PATTERN.matcher(opToken).matches()) {
            return "Register " + opToken.toLowerCase();
        }
        return null;
    }

    private Integer resolveWatchValue(String token) {
        String normalized = token.toLowerCase();
        switch (normalized) {
            case "pc":
                return motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            case "sp":
                return motherboard.getCpu().getSpecialRegisters().getSP().getAsInt();
            case "ir":
                return motherboard.getCpu().getSpecialRegisters().getIR().getAsInt();
            case "flags":
                int flags = 0;
                if (motherboard.getCpu().getSpecialRegisters().isZero()) {
                    flags |= 1;
                }
                if (motherboard.getCpu().getSpecialRegisters().isNegative()) {
                    flags |= 2;
                }
                if (motherboard.getCpu().getSpecialRegisters().isCarry()) {
                    flags |= 4;
                }
                if (motherboard.getCpu().getSpecialRegisters().isOverflow()) {
                    flags |= 8;
                }
                return flags;
            default:
                if (normalized.matches("r\\d+")) {
                    int registerIndex = Integer.parseInt(normalized.substring(1));
                    if (registerIndex < 0 || registerIndex >= Architecture.GPR_COUNT) {
                        return null;
                    }
                    Word value = new Word();
                    motherboard.getCpu().getRegisterFile().read(registerIndex, value);
                    return value.getAsInt();
                }
        }
        Integer address = parseFlexibleInteger(token);
        if (address == null || address < 0 || address >= Architecture.MEMORY_SIZE_BYTES) {
            return null;
        }
        return readMemoryWord(address);
    }

    private Integer parseFlexibleInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                return Integer.parseUnsignedInt(normalized.substring(2), 16);
            }
            if (normalized.startsWith("0b") || normalized.startsWith("0B")) {
                return Integer.parseUnsignedInt(normalized.substring(2), 2);
            }
            if (normalized.matches("-?[0-9]+")) {
                return Integer.parseInt(normalized, 10);
            }
            return Integer.parseUnsignedInt(normalized, 16);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void jumpToMemoryAddress() {
        Integer address = parseFlexibleInteger(memoryJumpField.getText());
        if (address == null) {
            return;
        }
        address = (address / 4) * 4;
        if (address < 0) {
            address = 0;
        }
        if (address > Architecture.MEMORY_SIZE_BYTES - 4) {
            address = Architecture.MEMORY_SIZE_BYTES - 4;
        }
        memoryBaseSpinner.setValue(address);
        refreshMemoryView();
    }

    private void searchMemoryValue() {
        Integer target = parseFlexibleInteger(memoryFindField.getText());
        if (target == null) {
            return;
        }
        int matchAddress = -1;
        for (int address = 0; address < Architecture.MEMORY_SIZE_BYTES; address += 4) {
            if (readMemoryWord(address) == target) {
                matchAddress = address;
                break;
            }
        }
        if (matchAddress >= 0) {
            memoryJumpField.setText(String.format("0x%04X", matchAddress));
            memoryBaseSpinner.setValue(matchAddress);
            refreshMemoryView();
        }
    }

    private void recordTrace(String phase, int pcBefore, int rawWord, int pcAfter) {
        String decoded = formatInstruction(decodeInstruction(rawWord));
        String entry = phase + " @" + String.format("0x%04X", pcBefore) + " -> " + decoded + " | next " + String.format("0x%04X", pcAfter);
        traceEntries.addFirst(entry);
        while (traceEntries.size() > 24) {
            traceEntries.removeLast();
        }
        refreshTraceView();
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

    private JPanel createDiagnosticsPanel() {
        diagnosticsStageLabel.setOpaque(true);
        diagnosticsStageLabel.setBackground(new Color(38, 43, 52));
        diagnosticsStageLabel.setForeground(new Color(214, 219, 228));
        diagnosticsStageLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        diagnosticsMessageLabel.setOpaque(true);
        diagnosticsMessageLabel.setBackground(new Color(30, 34, 42));
        diagnosticsMessageLabel.setForeground(new Color(154, 164, 178));
        diagnosticsMessageLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel cards = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        cards.add(createMetricCard("Tokens", diagnosticsTokensLabel));
        cards.add(createMetricCard("Parsed", diagnosticsParsedLabel));
        cards.add(createMetricCard("Resolved", diagnosticsResolvedLabel));
        cards.add(createMetricCard("Encoded", diagnosticsEncodedLabel));

        JPanel summary = new JPanel(new BorderLayout(8, 8));
        summary.add(diagnosticsStageLabel, BorderLayout.NORTH);
        summary.add(diagnosticsMessageLabel, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Assembler diagnostics"));
        panel.add(cards, BorderLayout.CENTER);
        panel.add(summary, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSpecialRegistersPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel registerGrid = new JPanel(new java.awt.GridLayout(3, 2, 8, 8));
        registerGrid.add(new JLabel("PC"));
        registerGrid.add(createReadOnlyField(pcField));
        registerGrid.add(new JLabel("SP"));
        registerGrid.add(createReadOnlyField(spField));
        registerGrid.add(new JLabel("IR"));
        registerGrid.add(createReadOnlyField(irField));

        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        flagsPanel.add(flagZ);
        flagsPanel.add(flagN);
        flagsPanel.add(flagC);
        flagsPanel.add(flagV);
        flagsPanel.add(new JLabel("State:"));
        flagsPanel.add(haltedValue);

        panel.add(registerGrid, BorderLayout.NORTH);
        panel.add(flagsPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void configureSourceGutter(JTextArea gutter, JScrollPane sourceScrollPane) {
        gutter.setEditable(false);
        gutter.setFocusable(false);
        gutter.setBackground(new Color(20, 23, 29));
        gutter.setForeground(new Color(107, 116, 130));
        gutter.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        gutter.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        gutter.setText(" ");
        gutter.setColumns(7);
        gutter.setRows(1);
        gutter.setLineWrap(false);
        gutter.setWrapStyleWord(false);
        gutter.setOpaque(true);
        sourceScrollPane.setRowHeaderView(gutter);
    }

    private JPanel createDisassemblyPanel() {
        disassemblyTable.setRowHeight(22);
        disassemblyTable.setFillsViewportHeight(true);
        disassemblyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        disassemblyTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        disassemblyTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        disassemblyTable.getColumnModel().getColumn(2).setPreferredWidth(420);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Live disassembly"));
        panel.add(new JScrollPane(disassemblyTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWatchPanel() {
        watchTable.setRowHeight(22);
        watchTable.setFillsViewportHeight(true);
        watchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watchTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        watchTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        watchTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        watchField.setToolTipText("Comma or space separated registers and addresses");

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> refreshWatchView());
        watchField.addActionListener(e -> refreshWatchView());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Watch:"));
        controls.add(watchField);
        controls.add(applyButton);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Watch panel"));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(watchTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTracePanel() {
        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        traceArea.setBackground(new Color(20, 23, 29));
        traceArea.setForeground(new Color(222, 227, 235));
        traceArea.setCaretColor(new Color(222, 227, 235));
        traceArea.setLineWrap(false);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Instruction trace"));
        panel.add(new JScrollPane(traceArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(127, 168, 221));

        valueLabel.setOpaque(true);
        valueLabel.setBackground(new Color(30, 34, 42));
        valueLabel.setForeground(new Color(222, 227, 235));
        valueLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(54, 60, 71)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JTextField createReadOnlyField(JTextField field) {
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setBackground(new Color(30, 34, 42));
        field.setForeground(new Color(222, 227, 235));
        field.setCaretColor(new Color(222, 227, 235));
        return field;
    }

    private JLabel flagLabel(String text) {
        JLabel label = new JLabel(text + ": off");
        label.setOpaque(true);
        label.setBackground(new Color(38, 43, 52));
        label.setForeground(new Color(214, 219, 228));
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private void updateFlag(JLabel label, boolean enabled) {
        String flagName = String.valueOf(label.getText().charAt(0));
        label.setText(flagName + ": " + (enabled ? "on" : "off"));
        label.setBackground(enabled ? new Color(56, 84, 140) : new Color(38, 43, 52));
    }

    private String formatRegisterValue(int value) {
        return value + " (0x" + Integer.toHexString(value).toUpperCase() + ")";
    }

    private String buildSyntax(OpCode opCode) {
        String operands = OPERAND_SYNTAX.getOrDefault(opCode.operandMapping(), "");
        return operands.isEmpty() ? opCode.name().toLowerCase() : opCode.name().toLowerCase() + " " + operands;
    }

    private String describeOpcode(OpCode opCode) {
        return switch (opCode) {
            case NOP -> "No operation.";
            case MOV -> "Copy a value from one register to another.";
            case LOAD -> "Load an immediate literal into a register.";
            case LOADR -> "Load a value from memory via register address.";
            case STORE -> "Store a register value into an immediate memory address.";
            case STORER -> "Store a register value into a register-addressed memory location.";
            case ADD -> "Add the source register into the destination register.";
            case SUB -> "Subtract the source register from the destination register.";
            case AND -> "Bitwise AND between destination and source registers.";
            case OR -> "Bitwise OR between destination and source registers.";
            case XOR -> "Bitwise XOR between destination and source registers.";
            case NOT -> "Bitwise negate the destination register.";
            case SHL -> "Shift the destination register left by an immediate amount.";
            case SHR -> "Shift the destination register right by an immediate amount.";
            case JMP -> "Jump to an absolute address.";
            case JZ -> "Jump if the zero flag is set.";
            case JNZ -> "Jump if the zero flag is clear.";
            case PUSH -> "Push a register onto the stack.";
            case POP -> "Pop a register from the stack.";
            case HALT -> "Stop execution.";
        };
    }

    private void scheduleDiagnosticsRefresh() {
        if (diagnosticsPending) {
            diagnosticsDirty = true;
            return;
        }
        diagnosticsPending = true;
        diagnosticsDirty = false;
        SwingUtilities.invokeLater(() -> {
            try {
                refreshAssemblyDiagnostics();
            } finally {
                diagnosticsPending = false;
                if (diagnosticsDirty) {
                    scheduleDiagnosticsRefresh();
                }
            }
        });
    }

    private void refreshAssemblyDiagnostics() {
        String source = sourceEditor.getText();
        if (source == null || source.isBlank()) {
            setDiagnosticsState("Idle", 0, 0, 0, 0, "No assembly source loaded.");
            clearSourceErrorHighlight();
            return;
        }

        try {
            IndexedLexer lexer = new IndexedLexer();
            lexer.reset(source, "workbench.asm");
            List<Token> tokens = lexer.tokenizeAll();
            int tokenCount = tokens.size();

            DefaultParser parser = new DefaultParser(new IndexedLexer());
            parser.reset(source, "workbench.asm");
            Program program = parser.parse();
            int parsedCount = program.statements().size();

            SymbolResolver resolver = new SymbolResolver();
            ResolvedProgram resolvedProgram = resolver.resolve(program);
            int resolvedCount = resolvedProgram.statements().size();

            AssemblerEncoder encoder = new AssemblerEncoder();
            EncodedProgram encodedProgram = encoder.encodeProgram(resolvedProgram);
            int encodedCount = encodedProgram.words().size();

            sourceSpanByAddress.clear();
            for (ResolvedStatement statement : resolvedProgram.statements()) {
                SourceSpan span = statement.sourceStatement().span();
                if (span != null) {
                    sourceSpanByAddress.put(statement.address(), span);
                }
            }

            setDiagnosticsState("Ready", tokenCount, parsedCount, resolvedCount, encodedCount, "Source parses and encodes cleanly.");
            clearSourceErrorHighlight();
        } catch (LexerException ex) {
            sourceSpanByAddress.clear();
            setDiagnosticsError("Lexer error", "Tokenization failed", ex.getMessage(), ex.getLine(), ex.getColumn(), null);
        } catch (ParserException ex) {
            sourceSpanByAddress.clear();
            setDiagnosticsError("Parser error", "Parsing failed", ex.getMessage(), ex.getLine(), ex.getColumn(), null);
        } catch (ResolutionException ex) {
            sourceSpanByAddress.clear();
            setDiagnosticsError("Resolver error", "Resolution failed", ex.getMessage(), ex.getLine(), ex.getColumn(), ex.getSpan());
        } catch (EncodingException ex) {
            sourceSpanByAddress.clear();
            setDiagnosticsError("Encoder error", "Encoding failed", ex.getMessage(), ex.getLine(), ex.getColumn(), ex.getSpan());
        } catch (RuntimeException ex) {
            sourceSpanByAddress.clear();
            setDiagnosticsError("Assembler error", "Diagnostics unavailable", ex.getMessage(), -1, -1, null);
        }
        refreshExecutionPointerView();
    }

    private void setDiagnosticsState(String stage, int tokenCount, int parsedCount, int resolvedCount, int encodedCount, String message) {
        diagnosticsStageLabel.setText(stage);
        diagnosticsTokensLabel.setText(Integer.toString(tokenCount));
        diagnosticsParsedLabel.setText(Integer.toString(parsedCount));
        diagnosticsResolvedLabel.setText(Integer.toString(resolvedCount));
        diagnosticsEncodedLabel.setText(Integer.toString(encodedCount));
        diagnosticsMessageLabel.setText("<html>" + escapeHtml(message) + "</html>");
        errorStartOffset = -1;
        errorLength = 0;
        scheduleHighlight(sourceEditor);
    }

    private void setDiagnosticsError(String stage, String summary, String message, int line, int column, SourceSpan span) {
        diagnosticsStageLabel.setText(stage);
        diagnosticsTokensLabel.setText("0");
        diagnosticsParsedLabel.setText("0");
        diagnosticsResolvedLabel.setText("0");
        diagnosticsEncodedLabel.setText("0");
        diagnosticsMessageLabel.setText("<html>" + escapeHtml(summary) + "<br>" + escapeHtml(message) + "</html>");
        if (span != null) {
            errorStartOffset = offsetForLineColumn(sourceEditor.getText(), span.startLine(), span.startColumn());
            int endOffset = offsetForLineColumn(sourceEditor.getText(), span.endLine(), span.endColumn());
            errorLength = Math.max(1, endOffset - errorStartOffset);
        } else if (line > 0 && column > 0) {
            errorStartOffset = offsetForLineColumn(sourceEditor.getText(), line, column);
            errorLength = Math.max(1, lineEndOffset(sourceEditor.getText(), line) - errorStartOffset);
        } else {
            clearSourceErrorHighlight();
            return;
        }
        scheduleHighlight(sourceEditor);
    }

    private void clearSourceErrorHighlight() {
        errorStartOffset = -1;
        errorLength = 0;
        scheduleHighlight(sourceEditor);
    }

    private int offsetForLineColumn(String source, int line, int column) {
        if (source == null || source.isEmpty() || line <= 0 || column <= 0) {
            return -1;
        }
        int currentLine = 1;
        int offset = 0;
        while (currentLine < line && offset < source.length()) {
            int newline = source.indexOf('\n', offset);
            if (newline < 0) {
                return source.length();
            }
            offset = newline + 1;
            currentLine++;
        }
        return Math.min(source.length(), offset + Math.max(0, column - 1));
    }

    private int lineEndOffset(String source, int line) {
        if (source == null || source.isEmpty() || line <= 0) {
            return -1;
        }
        int currentLine = 1;
        int offset = 0;
        while (currentLine < line && offset < source.length()) {
            int newline = source.indexOf('\n', offset);
            if (newline < 0) {
                return source.length();
            }
            offset = newline + 1;
            currentLine++;
        }
        int newline = source.indexOf('\n', offset);
        return newline < 0 ? source.length() : newline;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void installCompletionSupport(JTextPane editor) {
        completionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        completionTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        completionTable.setBackground(new Color(30, 34, 42));
        completionTable.setForeground(new Color(222, 227, 235));
        completionTable.setSelectionBackground(new Color(91, 143, 214));
        completionTable.setSelectionForeground(Color.WHITE);
        completionTable.setRowHeight(22);
        completionTable.getTableHeader().setReorderingAllowed(false);
        completionTable.getTableHeader().setBackground(new Color(20, 23, 29));
        completionTable.getTableHeader().setForeground(new Color(214, 219, 228));
        completionTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        completionTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        completionTable.getColumnModel().getColumn(2).setPreferredWidth(380);
        completionTable.setFocusable(false);
        completionTable.getSelectionModel().addListSelectionListener(e -> updateCompletionDetail());
        completionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    acceptCompletion(editor);
                }
            }
        });
        completionTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept-suggestion");
        completionTable.getActionMap().put("accept-suggestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptCompletion(editor);
            }
        });
        completionTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "accept-suggestion");
        completionTable.getActionMap().put("accept-suggestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptCompletion(editor);
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "show-completion");
        editor.getActionMap().put("show-completion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCompletionPopup(editor);
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "accept-completion-or-tab");
        editor.getActionMap().put("accept-completion-or-tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (completionPopup.isVisible()) {
                    acceptCompletion(editor);
                } else {
                    insertText(editor, "    ");
                }
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept-completion-or-enter");
        editor.getActionMap().put("accept-completion-or-enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (completionPopup.isVisible()) {
                    acceptCompletion(editor);
                } else {
                    insertText(editor, "\n");
                }
            }
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleCompletionRefresh(editor);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleCompletionRefresh(editor);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private void showCompletionPopup(JTextPane editor) {
        CompletionContext context = analyzeCompletionContext(editor);
        List<CompletionSuggestion> suggestions = getCompletionSuggestions(editor, context);
        if (suggestions.isEmpty()) {
            hideCompletionPopup();
            return;
        }

        completionModel.setRowCount(0);
        for (CompletionSuggestion suggestion : suggestions) {
            completionModel.addRow(new Object[]{suggestion.text(), suggestion.usage(), suggestion.description()});
        }
        completionTable.setRowSelectionInterval(0, 0);
        completionPopup.removeAll();
        JScrollPane scrollPane = new JScrollPane(completionTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(20, 23, 29));
        int popupWidth = context.mode() == CompletionMode.OPCODE ? 980 : 1120;
        scrollPane.setPreferredSize(new Dimension(popupWidth, 320));
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        completionDetailLabel.setOpaque(true);
        completionDetailLabel.setBackground(new Color(20, 23, 29));
        completionDetailLabel.setForeground(new Color(154, 164, 178));
        completionDetailLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(completionDetailLabel, BorderLayout.SOUTH);
        completionPopup.add(panel);
        completionDetailLabel.setText("<html><b>" + escapeHtml(context.mode().name().toLowerCase()) + "</b> &nbsp; " + escapeHtml(context.detail()) + "</html>");
        updateCompletionDetail();
        completionDetailLabel.setText("<html><b>" + escapeHtml(context.mode().name().toLowerCase()) + "</b> &nbsp; " + escapeHtml(context.detail()) + "</html>");
        completionPopup.setFocusable(false);

        try {
            Rectangle2D caret = editor.modelToView2D(editor.getCaretPosition());
            if (caret != null) {
                completionPopup.show(editor, (int) caret.getX(), (int) caret.getMaxY());
            } else {
                completionPopup.show(editor, 0, editor.getHeight());
            }
        } catch (Exception ex) {
            completionPopup.show(editor, 0, editor.getHeight());
        }
    }

    private void hideCompletionPopup() {
        completionPopup.setVisible(false);
    }

    private void scheduleCompletionRefresh(JTextPane editor) {
        if (!completionPopup.isVisible()) {
            return;
        }
        SwingUtilities.invokeLater(() -> showCompletionPopup(editor));
    }

    private List<CompletionSuggestion> getCompletionSuggestions(JTextPane editor, CompletionContext context) {
        String prefix = context.prefix().toLowerCase();
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        Set<String> labelNames = extractLabels(editor.getText());

        switch (context.mode()) {
            case OPCODE -> {
                for (OpCode opCode : OpCode.values()) {
                    String text = opCode.name().toLowerCase();
                    if (prefix.isEmpty() || text.startsWith(prefix)) {
                        suggestions.add(new CompletionSuggestion(text, buildSyntax(opCode), describeOpcode(opCode), text));
                    }
                }
            }
            case OPERAND -> suggestions.addAll(operandSuggestionsForContext(context, labelNames, prefix));
            case DIRECTIVE -> {
                for (String item : COMPLETION_ITEMS) {
                    if (prefix.isEmpty() || item.startsWith(prefix)) {
                        suggestions.add(new CompletionSuggestion(item, completionUsageFor(item), completionDescriptionFor(item), item));
                    }
                }
            }
            case GENERAL -> {
                for (String label : labelNames) {
                    if (prefix.isEmpty() || label.toLowerCase().startsWith(prefix)) {
                        suggestions.add(new CompletionSuggestion(label, "label", "Label from current source", label));
                    }
                }
            }
        }

        suggestions.sort((a, b) -> a.text().compareToIgnoreCase(b.text()));
        return suggestions;
    }

    private List<CompletionSuggestion> operandSuggestionsForContext(CompletionContext context, Set<String> labelNames, String prefix) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        OperandMapping mapping = context.mapping();
        int operandIndex = context.operandIndex();
        OperandRole role = roleFor(mapping, operandIndex);
        if (role == OperandRole.REGISTER) {
            addIfMatches(suggestions, "rd", "register", "Destination register", prefix);
            addIfMatches(suggestions, "rs", "register", "Source register", prefix);
            for (int i = 0; i < Architecture.GPR_COUNT; i++) {
                String reg = "r" + i;
                addIfMatches(suggestions, reg, "register", "General purpose register", prefix);
            }
            addIfMatches(suggestions, "pc", "special register", "Program counter", prefix);
            addIfMatches(suggestions, "sp", "special register", "Stack pointer", prefix);
            addIfMatches(suggestions, "ir", "special register", "Instruction register", prefix);
            addIfMatches(suggestions, "flags", "special register", "Flag register view", prefix);
        } else if (role == OperandRole.IMMEDIATE) {
            addIfMatches(suggestions, "imm16", "immediate", "16-bit immediate or label", prefix);
            for (String label : labelNames) {
                addIfMatches(suggestions, label, "label", "Label from current source", prefix);
            }
        } else if (role == OperandRole.LABEL_OR_IMMEDIATE) {
            addIfMatches(suggestions, "label", "label", "Jump target or named address", prefix);
            for (String label : labelNames) {
                addIfMatches(suggestions, label, "label", "Label from current source", prefix);
            }
        }
        return suggestions;
    }

    private void addIfMatches(List<CompletionSuggestion> suggestions, String text, String usage, String description, String prefix) {
        if (prefix.isEmpty() || text.toLowerCase().startsWith(prefix)) {
            suggestions.add(new CompletionSuggestion(text, usage, description, text));
        }
    }

    private CompletionContext analyzeCompletionContext(JTextPane editor) {
        String source = editor.getText();
        int caret = editor.getCaretPosition();
        int lineStart = Math.max(0, source.lastIndexOf('\n', Math.max(0, caret - 1)) + 1);
        String linePrefix = source.substring(lineStart, caret);
        int commentIndex = findCommentIndex(linePrefix);
        if (commentIndex >= 0) {
            linePrefix = linePrefix.substring(0, commentIndex);
        }
        String trimmed = linePrefix.stripLeading();
        if (trimmed.isEmpty()) {
            return new CompletionContext(CompletionMode.OPCODE, currentCompletionPrefix(editor), null, 0, OperandMapping.NONE, "opcode suggestion");
        }

        if (trimmed.startsWith(".")) {
            return new CompletionContext(CompletionMode.DIRECTIVE, currentCompletionPrefix(editor), null, 0, OperandMapping.NONE, "directive suggestion");
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length > 0 && tokens[0].endsWith(":")) {
            int labelEnd = trimmed.indexOf(tokens[0]) + tokens[0].length();
            String remainder = trimmed.substring(labelEnd).stripLeading();
            if (remainder.isEmpty()) {
                return new CompletionContext(CompletionMode.OPCODE, currentCompletionPrefix(editor), null, 0, OperandMapping.NONE, "opcode after label");
            }
            trimmed = remainder;
            tokens = trimmed.split("\\s+");
        }

        OpCode opCode = tokens.length > 0 ? OpCode.valueOfNullable(tokens[0].toUpperCase()) : null;
        if (opCode == null) {
            return new CompletionContext(CompletionMode.OPCODE, currentCompletionPrefix(editor), null, 0, OperandMapping.NONE, "opcode suggestion");
        }

        String afterOpcode = trimmed.substring(tokens[0].length()).trim();
        int operandIndex = 0;
        if (!afterOpcode.isEmpty()) {
            operandIndex = afterOpcode.split(",", -1).length - 1;
            if (trimmed.endsWith(",") || linePrefix.endsWith(",")) {
                operandIndex++;
            }
        }
        int normalizedOperandIndex = Math.max(0, operandIndex);
        return new CompletionContext(CompletionMode.OPERAND, currentCompletionPrefix(editor), opCode, normalizedOperandIndex, opCode.operandMapping(), "expected " + expectedOperandText(opCode, normalizedOperandIndex));
    }

    private Set<String> extractLabels(String source) {
        Set<String> labels = new LinkedHashSet<>();
        if (source == null || source.isBlank()) {
            return labels;
        }
        Matcher matcher = LABEL_REFERENCE_PATTERN.matcher(source);
        while (matcher.find()) {
            labels.add(matcher.group(1));
        }
        return labels;
    }

    private String currentCompletionPrefix(JTextPane editor) {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        int start = caret;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                start--;
            } else {
                break;
            }
        }
        return text.substring(start, caret);
    }

    private void acceptCompletion(JTextPane editor) {
        int row = completionTable.getSelectedRow();
        if (row < 0) {
            hideCompletionPopup();
            return;
        }
        String value = String.valueOf(completionModel.getValueAt(row, 0));
        String insertion = completionModel.getColumnCount() > 3 ? String.valueOf(completionModel.getValueAt(row, 3)) : value;

        String prefix = currentCompletionPrefix(editor);
        int caret = editor.getCaretPosition();
        int start = caret - prefix.length();
        try {
            var doc = editor.getDocument();
            doc.remove(start, prefix.length());
            doc.insertString(start, insertion, null);
            editor.setCaretPosition(start + insertion.length());
            hideCompletionPopup();
            scheduleHighlight(editor);
            scheduleDiagnosticsRefresh();
        } catch (Exception ex) {
            showError("Completion failed", new RuntimeException(ex));
        }
    }

    private void updateCompletionDetail() {
        int row = completionTable.getSelectedRow();
        if (row < 0) {
            completionDetailLabel.setText(" ");
            return;
        }
        String text = String.valueOf(completionModel.getValueAt(row, 0));
        String usage = String.valueOf(completionModel.getValueAt(row, 1));
        String description = String.valueOf(completionModel.getValueAt(row, 2));
        completionDetailLabel.setText("<html><b>" + escapeHtml(text) + "</b> &nbsp; " + escapeHtml(usage) + " &nbsp; " + escapeHtml(description) + "</html>");
    }

    private String completionUsageFor(String item) {
        if (item.startsWith(".")) {
            return item;
        }
        if (item.matches("r\\d+|pc|sp|ir|flags")) {
            return item;
        }
        return switch (item) {
            case "jmp", "jz", "jnz" -> item + " label";
            case "push" -> "push rs";
            case "pop" -> "pop rd";
            default -> item;
        };
    }

    private String completionDescriptionFor(String item) {
        return switch (item) {
            case ".word" -> "Emit a 4-byte word.";
            case ".byte" -> "Emit one byte.";
            case ".ascii" -> "Emit a string as bytes.";
            case ".org" -> "Move output address.";
            case "jmp" -> "Jump to an absolute label or address.";
            case "jz" -> "Jump if zero flag is set.";
            case "jnz" -> "Jump if zero flag is clear.";
            case "push" -> "Push a register value on the stack.";
            case "pop" -> "Pop a register value from the stack.";
            default -> "";
        };
    }

    private OperandRole roleFor(OperandMapping mapping, int operandIndex) {
        return switch (mapping) {
            case RD_RS -> OperandRole.REGISTER;
            case RD_ONLY -> OperandRole.REGISTER;
            case RS_ONLY -> OperandRole.REGISTER;
            case RD_IMM16 -> operandIndex == 0 ? OperandRole.REGISTER : OperandRole.IMMEDIATE;
            case IMM16_RD -> operandIndex == 0 ? OperandRole.IMMEDIATE : OperandRole.REGISTER;
            case IMM16_ONLY -> OperandRole.LABEL_OR_IMMEDIATE;
            case NONE -> OperandRole.NONE;
        };
    }

    private String expectedOperandText(OpCode opCode, int operandIndex) {
        OperandRole role = roleFor(opCode.operandMapping(), operandIndex);
        return switch (role) {
            case REGISTER -> "register";
            case IMMEDIATE -> "imm16";
            case LABEL_OR_IMMEDIATE -> "imm16 or label";
            case NONE -> "none";
        };
    }

    private record CompletionSuggestion(String text, String usage, String description, String insertion) {
    }

    private record CompletionContext(CompletionMode mode, String prefix, OpCode opcode, int operandIndex, OperandMapping mapping, String detail) {
    }

    private enum CompletionMode {
        OPCODE,
        OPERAND,
        DIRECTIVE,
        GENERAL
    }

    private enum OperandRole {
        NONE,
        REGISTER,
        IMMEDIATE,
        LABEL_OR_IMMEDIATE
    }

    private void insertText(JTextPane editor, String text) {
        try {
            editor.getDocument().insertString(editor.getCaretPosition(), text, null);
        } catch (Exception ex) {
            showError("Editor input failed", new RuntimeException(ex));
        }
    }

    private void configureSourceEditor(JTextPane editor) {
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setBackground(new Color(16, 18, 23));
        editor.setForeground(new Color(222, 227, 235));
        editor.setCaretColor(new Color(91, 143, 214));
        editor.setOpaque(true);
        editor.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editor.setToolTipText("");
        javax.swing.ToolTipManager.sharedInstance().registerComponent(editor);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleHighlight(editor);
                scheduleDiagnosticsRefresh();
                refreshExecutionPointerView();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleHighlight(editor);
                scheduleDiagnosticsRefresh();
                refreshExecutionPointerView();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        editor.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                editor.setToolTipText(tooltipForSourcePosition(editor, e.getPoint()));
            }
        });
        scheduleHighlight(editor);
        scheduleDiagnosticsRefresh();
        installCompletionSupport(editor);
    }

    private void scheduleHighlight(JTextPane editor) {
        if (highlightingSource) {
            highlightPending = true;
            return;
        }
        SwingUtilities.invokeLater(() -> highlightSource(editor));
    }

    private void highlightSource(JTextPane editor) {
        if (highlightingSource) {
            return;
        }
        highlightingSource = true;
        try {
            StyledDocument doc = editor.getStyledDocument();
            SimpleAttributeSet base = new SimpleAttributeSet();
            StyleConstants.setForeground(base, new Color(222, 227, 235));
            StyleConstants.setBold(base, false);
            doc.setCharacterAttributes(0, doc.getLength(), base, true);

            String text = editor.getText();
            int offset = 0;
            for (String line : text.split("\n", -1)) {
                int lineLength = line.length();
                int commentIndex = findCommentIndex(line);
                int codeEnd = commentIndex >= 0 ? commentIndex : lineLength;

                if (commentIndex >= 0) {
                    applyStyle(doc, offset + commentIndex, lineLength - commentIndex, new Color(107, 116, 130), false);
                }

                String code = line.substring(0, codeEnd);
                applyPattern(doc, offset, code, LABEL_PATTERN, new Color(127, 168, 221), true);
                applyPattern(doc, offset, code, DIRECTIVE_PATTERN, new Color(207, 159, 99), true);
                applyPattern(doc, offset, code, OPCODE_PATTERN, new Color(91, 143, 214), true);
                applyPattern(doc, offset, code, REGISTER_PATTERN, new Color(157, 147, 196), true);
                applyPattern(doc, offset, code, NUMBER_PATTERN, new Color(207, 138, 138), false);
                applyPattern(doc, offset, code, STRING_PATTERN, new Color(130, 171, 134), false);

                offset += lineLength + 1;
            }
            if (errorStartOffset >= 0 && errorLength > 0) {
                applyErrorStyle(doc, errorStartOffset, errorLength);
            }
        } finally {
            highlightingSource = false;
            if (highlightPending) {
                highlightPending = false;
                scheduleHighlight(editor);
            }
        }
    }

    private void applyPattern(StyledDocument doc, int baseOffset, String text, Pattern pattern, Color foreground, boolean bold) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            applyStyle(doc, baseOffset + matcher.start(), matcher.end() - matcher.start(), foreground, bold);
        }
    }

    private void applyStyle(StyledDocument doc, int start, int length, Color foreground, boolean bold) {
        if (length <= 0) {
            return;
        }
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, foreground);
        StyleConstants.setBold(set, bold);
        doc.setCharacterAttributes(start, length, set, false);
    }

    private void applyErrorStyle(StyledDocument doc, int start, int length) {
        if (start < 0 || length <= 0) {
            return;
        }
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, new Color(214, 150, 150));
        StyleConstants.setBackground(set, new Color(92, 42, 46));
        StyleConstants.setBold(set, true);
        StyleConstants.setUnderline(set, true);
        doc.setCharacterAttributes(start, length, set, false);
    }

    private int findCommentIndex(String line) {
        int semicolon = line.indexOf(';');
        int hash = line.indexOf('#');
        int slashSlash = line.indexOf("//");
        int index = -1;
        if (semicolon >= 0) {
            index = semicolon;
        }
        if (hash >= 0 && (index < 0 || hash < index)) {
            index = hash;
        }
        if (slashSlash >= 0 && (index < 0 || slashSlash < index)) {
            index = slashSlash;
        }
        return index;
    }
}
