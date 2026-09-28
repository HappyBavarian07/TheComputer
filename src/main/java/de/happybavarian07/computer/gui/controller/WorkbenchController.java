package de.happybavarian07.computer.gui.controller;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.AssemblerService;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.disassembler.Disassembler;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.exceptions.assembler.LexerException;
import de.happybavarian07.computer.exceptions.assembler.ParserException;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import de.happybavarian07.computer.gui.util.NumberFormats;
import de.happybavarian07.computer.isa.Instruction;
import de.happybavarian07.computer.system.Motherboard;
import de.happybavarian07.computer.util.Architecture;

import javax.swing.Timer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the emulated machine ({@link Motherboard}) and every operation that
 * mutates it (reset, step, run, assemble, load). Carries no Swing types in
 * its public surface beyond {@link Timer}, which is just an EDT-safe
 * scheduler, not a visual component. Panels read state through the getters
 * here and are notified of changes via {@link WorkbenchListener} instead of
 * touching {@link Motherboard} directly.
 */
public final class WorkbenchController {
    private final Motherboard motherboard = new Motherboard();
    private final Disassembler disassembler = new Disassembler();
    private final AtomicBoolean runRequested = new AtomicBoolean(false);
    private static final int MAX_TRACE_ENTRIES = 1000;
    private static final int RUN_TRACE_STEPS_PER_TICK = 32;
    /** Pass as steps per tick to fill each timer tick's time slice with as many steps as fit. */
    public static final int MAX_THROUGHPUT = 0;
    private static final long TICK_BUDGET_NANOS = 25_000_000L;
    private final int[] tracePcRing = new int[RUN_TRACE_STEPS_PER_TICK];
    private final Deque<String> traceEntries = new ArrayDeque<>();
    private final List<WorkbenchListener> listeners = new CopyOnWriteArrayList<>();

    private Timer runTimer;
    private long runStartNanos;
    private long runStepsTotal;

    public void addListener(WorkbenchListener listener) {
        listeners.add(listener);
    }

    public Motherboard getMotherboard() {
        return motherboard;
    }

    public Disassembler getDisassembler() {
        return disassembler;
    }

    public boolean isRunning() {
        return runTimer != null && runTimer.isRunning();
    }

    public List<String> getTraceEntries() {
        return List.copyOf(traceEntries);
    }

    // --- lifecycle -----------------------------------------------------

    public void reset() {
        motherboard.reset();
        runRequested.set(false);
        traceEntries.clear();
        log("Machine reset.");
        fireStateChanged();
    }

    public void step() {
        int pcBefore = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
        long rawBefore = readMemoryWord(pcBefore);
        try {
            motherboard.stepSystem();
            log("Step complete.");
            if (motherboard.getCpu().isHalted()) {
                log(motherboard.getCpu().isFaulted() ? "CPU fault: " + motherboard.getCpu().getFaultReason() : "CPU reached HALT.");
            }
        } catch (RuntimeException ex) {
            log("Execution error: " + ex.getMessage());
            recordTrace("step", pcBefore, rawBefore, pcBefore);
            fireStateChanged();
            return;
        }
        recordTrace("step", pcBefore, rawBefore, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
        fireStateChanged();
    }

    public void stepMany(int count) {
        for (int i = 0; i < count; i++) {
            if (motherboard.getCpu().isHalted()) {
                log("CPU already halted before step " + (i + 1) + ".");
                break;
            }
            int pcBefore = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            long rawBefore = readMemoryWord(pcBefore);
            try {
                motherboard.stepSystem();
                recordTrace("step", pcBefore, rawBefore, motherboard.getCpu().getSpecialRegisters().getPC().getAsInt());
            } catch (RuntimeException ex) {
                log("Execution error after step " + (i + 1) + ": " + ex.getMessage());
                recordTrace("step", pcBefore, rawBefore, pcBefore);
                break;
            }
        }
        log("Advanced " + count + " steps.");
        fireStateChanged();
    }

    /** Runs on a Swing {@link Timer} tick until halted or {@link #stopRun()}. */
    public void startRun(int stepsPerTick, Runnable onTick) {
        if (isRunning()) {
            return;
        }
        runRequested.set(true);
        runStartNanos = System.nanoTime();
        runStepsTotal = 0;
        runTimer = new Timer(maxThroughputTimerDelay(stepsPerTick), e -> {
            if (!runRequested.get() || motherboard.getCpu().isHalted()) {
                runTimer.stop();
                if (motherboard.getCpu().isHalted()) {
                    log(motherboard.getCpu().isFaulted() ? "CPU fault: " + motherboard.getCpu().getFaultReason() : "Run complete: CPU halted.");
                }
                fireStateChanged();
                onTick.run();
                return;
            }
            boolean maxThroughput = stepsPerTick <= MAX_THROUGHPUT;
            long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
            int[] ring = tracePcRing;
            int executed = 0;
            String error = null;
            try {
                for (int i = 0; maxThroughput || i < stepsPerTick; i++) {
                    if (motherboard.getCpu().isHalted()) {
                        break;
                    }
                    // max throughput fills the time slice instead of a fixed step count; check the clock rarely
                    if (maxThroughput && (i & 0xFF) == 0xFF && System.nanoTime() >= deadline) {
                        break;
                    }
                    ring[i % RUN_TRACE_STEPS_PER_TICK] = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
                    motherboard.stepSystem();
                    executed++;
                }
            } catch (RuntimeException ex) {
                error = ex.getMessage();
            }
            runStepsTotal += executed;
            // decoding every step would slow the run down, so the trace keeps the last few steps of each tick
            int pcNow = motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            for (int i = Math.max(0, executed - RUN_TRACE_STEPS_PER_TICK); i < executed; i++) {
                int pc = ring[i % RUN_TRACE_STEPS_PER_TICK];
                int next = i + 1 < executed ? ring[(i + 1) % RUN_TRACE_STEPS_PER_TICK] : pcNow;
                recordTrace("run", pc, readMemoryWord(pc), next);
            }
            if (error != null) {
                recordTrace("run", pcNow, readMemoryWord(pcNow), pcNow);
            }
            fireStateChanged();
            onTick.run();
            if (error != null) {
                runTimer.stop();
                log("Execution error: " + error);
            }
        });
        runTimer.setInitialDelay(0);
        runTimer.start();
    }

    // in max throughput mode the slice itself already takes ~25 ms, so the timer only needs to yield to the event queue
    private static int maxThroughputTimerDelay(int stepsPerTick) {
        return stepsPerTick <= MAX_THROUGHPUT ? 5 : 30;
    }

    public void stopRun() {
        runRequested.set(false);
        if (runTimer != null) {
            runTimer.stop();
        }
        log("Run stopped by user.");
    }

    /** Hz reading for the current/last run, or -1 if nothing has run yet. */
    public double currentRunHz() {
        long elapsed = System.nanoTime() - runStartNanos;
        if (elapsed <= 0) {
            return -1;
        }
        return runStepsTotal / (elapsed / 1_000_000_000.0);
    }

    public long currentRunSteps() {
        return runStepsTotal;
    }

    // --- assembling / loading -------------------------------------------

    public record AssembleResult(boolean success, String message, EncodedProgram encoded, ResolvedProgram resolved) {
    }

    /**
     * Assembles {@code source} in-process (lexer to parser to resolver to
     * encoder, no temp files) and, on success, loads the result into RAM
     * and resets PC/SP. On failure, RAM is left untouched.
     */
    public AssembleResult assemble(String source) {
        if (source == null || source.isBlank()) {
            return new AssembleResult(false, "No assembly source to assemble.", null, null);
        }
        try {
            AssemblerService.Result assembled = new AssemblerService().assemble(source, "workbench.asm");
            ResolvedProgram resolved = assembled.resolved();
            EncodedProgram encoded = assembled.encoded();

            motherboard.reset();
            traceEntries.clear();
            loadImage(assembled.image());
            motherboard.getCpu().getSpecialRegisters().getPC().set(0);
            motherboard.getCpu().getSpecialRegisters().getSP().set(Architecture.STACK_BASE_ADDRESS);

            log("Assembly succeeded and machine code loaded into RAM.");
            fireStateChanged();
            return new AssembleResult(true, "Source parses and encodes cleanly.", encoded, resolved);
        } catch (LexerException | ParserException | ResolutionException | EncodingException ex) {
            return new AssembleResult(false, ex.getMessage(), null, null);
        }
    }

    public record BinaryResult(boolean success, String message, byte[] image) {
    }

    /**
     * Assembles {@code source} into a flat binary image without touching the
     * running machine. The image is exactly what {@link #assemble} would put
     * in RAM from address 0 (each encoded word written as 8 little-endian
     * bytes at its address, gaps zero-filled), so it round-trips through
     * {@link #loadBinary}.
     */
    public BinaryResult assembleToBinary(String source) {
        if (source == null || source.isBlank()) {
            return new BinaryResult(false, "No assembly source to assemble.", null);
        }
        try {
            byte[] image = new AssemblerService().assemble(source, "workbench.asm").image();
            return new BinaryResult(true, "Assembled " + image.length + " bytes.", image);
        } catch (LexerException | ParserException | ResolutionException | EncodingException ex) {
            return new BinaryResult(false, ex.getMessage(), null);
        }
    }

    // writes the image into RAM as 8-byte little-endian words from address 0; the last partial word is zero-padded
    private void loadImage(byte[] image) {
        Word wordBuf = new Word();
        Address addrBuf = new Address();
        for (int offset = 0; offset < image.length; offset += Architecture.INSTRUCTION_BYTES) {
            long raw = 0;
            for (int byteIndex = 0; byteIndex < Architecture.INSTRUCTION_BYTES && offset + byteIndex < image.length; byteIndex++) {
                raw |= ((long) (image[offset + byteIndex] & 0xFF)) << (byteIndex * 8);
            }
            addrBuf.set(offset);
            wordBuf.set(raw);
            motherboard.getSystemBus().writeWord(addrBuf, wordBuf);
        }
    }

    public void loadBinary(Path file) throws IOException {
        motherboard.reset();
        traceEntries.clear();
        byte[] binary = Files.readAllBytes(file);

        loadImage(binary);
        motherboard.getCpu().getSpecialRegisters().getPC().set(0);
        motherboard.getCpu().getSpecialRegisters().getSP().set(Architecture.STACK_BASE_ADDRESS);
        log("Loaded binary file: " + file);
        fireStateChanged();
    }

    // --- memory / registers ---------------------------------------------

    public long readMemoryWord(int address) {
        Word word = new Word();
        try {
            motherboard.getSystemBus().readWord(new Address(address), word);
        } catch (RuntimeException ex) {
            word.set(0);
        }
        return word.getAsLong();
    }

    public void writeMemoryWord(int address, long value) {
        motherboard.getSystemBus().writeWord(new Address(address), new Word(value));
        fireStateChanged();
    }

    public Instruction decodeInstruction(long rawWord) {
        Instruction instruction = new Instruction();
        motherboard.getCpu().getInstructionDecoder().decodeNullable(new Word(rawWord), instruction);
        return instruction;
    }

    public Long resolveWatchValue(String token) {
        String normalized = token.toLowerCase();
        switch (normalized) {
            case "pc":
                return (long) motherboard.getCpu().getSpecialRegisters().getPC().getAsInt();
            case "sp":
                return (long) motherboard.getCpu().getSpecialRegisters().getSP().getAsInt();
            case "ir":
                return motherboard.getCpu().getSpecialRegisters().getIR().getAsLong();
            case "flags": {
                long flags = 0;
                var special = motherboard.getCpu().getSpecialRegisters();
                if (special.isZero()) flags |= 1;
                if (special.isNegative()) flags |= 2;
                if (special.isCarry()) flags |= 4;
                if (special.isOverflow()) flags |= 8;
                return flags;
            }
            default:
                if (normalized.matches("r\\d+")) {
                    int registerIndex = Integer.parseInt(normalized.substring(1));
                    if (registerIndex < 0 || registerIndex >= Architecture.GPR_COUNT) {
                        return null;
                    }
                    Word value = new Word();
                    motherboard.getCpu().getRegisterFile().read(registerIndex, value);
                    return value.getAsLong();
                }
        }
        Integer address = NumberFormats.parseFlexibleInteger(token);
        if (address == null || address < 0 || address >= Architecture.MEMORY_SIZE_BYTES) {
            return null;
        }
        return readMemoryWord(address);
    }

    public int findMemoryValue(long target) {
        for (int address = 0; address < Architecture.MEMORY_SIZE_BYTES; address += Architecture.INSTRUCTION_BYTES) {
            if (readMemoryWord(address) == target) {
                return address;
            }
        }
        return -1;
    }

    // --- benchmark ---------------------------------------------------

    public record BenchmarkResult(double hz, double mips, long steps, double elapsedSeconds, boolean capped) {
    }

    /** Runs the currently-loaded program on a throwaway core, off the EDT. */
    public void benchmarkCoreAsync(java.util.function.Consumer<BenchmarkResult> onDone) {
        int wordCount = Math.min(1024, Architecture.MEMORY_FREE_END / Architecture.INSTRUCTION_BYTES);
        long[] image = new long[wordCount];
        for (int i = 0; i < image.length; i++) {
            image[i] = readMemoryWord(i * Architecture.INSTRUCTION_BYTES);
        }
        int startSp = Architecture.STACK_BASE_ADDRESS;

        Thread worker = new Thread(() -> {
            int stepCap = 5_000_000;
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
            double elapsedSec = elapsed / 1_000_000_000.0;
            double hz = steps / elapsedSec;
            double mips = hz / 1_000_000.0;
            long finalSteps = steps;
            boolean finalCapped = capped;
            javax.swing.SwingUtilities.invokeLater(() -> onDone.accept(new BenchmarkResult(hz, mips, finalSteps, elapsedSec, finalCapped)));
        }, "core-benchmark");
        worker.setDaemon(true);
        worker.start();
    }

    private long runProgramOnce(Motherboard bench, long[] image, int startSp, int cap) {
        bench.reset();
        for (int i = 0; i < image.length; i++) {
            bench.getSystemBus().writeWord(new Address(i * Architecture.INSTRUCTION_BYTES), new Word(image[i]));
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

    // --- trace / listeners -----------------------------------------------

    private void recordTrace(String phase, int pcBefore, long rawWord, int pcAfter) {
        String decoded = disassembler.disassemble(rawWord);
        String entry = phase + " @" + String.format("0x%04X", pcBefore) + " -> " + decoded + " | next " + String.format("0x%04X", pcAfter);
        traceEntries.addFirst(entry);
        while (traceEntries.size() > MAX_TRACE_ENTRIES) {
            traceEntries.removeLast();
        }
    }

    private void log(String message) {
        for (WorkbenchListener listener : listeners) {
            listener.onLog(message);
        }
    }

    private void fireStateChanged() {
        for (WorkbenchListener listener : listeners) {
            listener.onStateChanged();
        }
    }
}
