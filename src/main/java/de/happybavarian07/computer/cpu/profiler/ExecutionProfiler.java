package de.happybavarian07.computer.cpu.profiler;

import de.happybavarian07.computer.isa.OpCode;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

/*
 * @Author HappyBavarian07
 * @Date September 13, 2026 | 21:15
 */
public class ExecutionProfiler {
    private static final int OPCODE_COUNT = 256;

    private static final String[] CATEGORY_NAMES = {
            "System & Control",
            "Arithmetic",
            "Bitwise & Shifts",
            "Branches & Subroutines",
            "Stack Operations",
            "Memory Operations"
    };

    private boolean enabled;
    private boolean measurementActive;

    private long totalInstructions;
    private long totalElapsedNs;

    private long fetchNs;
    private long decodeNs;
    private long conditionNs;
    private long executeNs;
    private long pcAdvanceNs;

    private final long[] opcodeCounts = new long[OPCODE_COUNT];
    private final long[] categoryCounts = new long[CATEGORY_NAMES.length];

    private long measurementStartNs;

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public void startMeasurement() {
        if (!enabled || measurementActive) {
            return;
        }

        measurementStartNs = System.nanoTime();
        measurementActive = true;
    }

    public void stopMeasurement() {
        if (!enabled || !measurementActive) {
            return;
        }

        totalElapsedNs += System.nanoTime() - measurementStartNs;
        measurementActive = false;
    }

    public void recordFetch(long deltaNs) {
        if (!enabled) {
            return;
        }

        fetchNs += deltaNs;
    }

    public void recordDecode(long deltaNs) {
        if (!enabled) {
            return;
        }

        decodeNs += deltaNs;
    }

    public void recordCondition(long deltaNs) {
        if (!enabled) {
            return;
        }

        conditionNs += deltaNs;
    }

    public void recordExecute(long deltaNs, int opcode) {
        if (!enabled) {
            return;
        }

        executeNs += deltaNs;
        recordOpcode(opcode);
    }

    public void recordPcAdvance(long deltaNs) {
        if (!enabled) {
            return;
        }

        pcAdvanceNs += deltaNs;
    }

    public void recordFetch(Runnable runnable) {
        if (!enabled) {
            runnable.run();
            return;
        }

        fetchNs += timeMethod(runnable);
    }

    public void recordDecode(Runnable runnable) {
        if (!enabled) {
            runnable.run();
            return;
        }

        decodeNs += timeMethod(runnable);
    }

    public boolean recordCondition(BooleanSupplier supplier) {
        if (!enabled) {
            return supplier.getAsBoolean();
        }

        long startNs = System.nanoTime();
        boolean result = supplier.getAsBoolean();
        conditionNs += System.nanoTime() - startNs;

        return result;
    }

    public void recordExecute(Runnable runnable, int opcode) {
        if (!enabled) {
            runnable.run();
            return;
        }

        executeNs += timeMethod(runnable);
        recordOpcode(opcode);
    }

    public void recordPcAdvance(Runnable runnable) {
        if (!enabled) {
            runnable.run();
            return;
        }

        pcAdvanceNs += timeMethod(runnable);
    }

    /**
     * Records one CPU instruction.
     * This should be called exactly once at the end of Cpu.step().
     */
    public void recordInstruction() {
        if (!enabled) {
            return;
        }

        totalInstructions++;
    }

    private void recordOpcode(int opcode) {
        if (opcode < 0 || opcode >= OPCODE_COUNT) {
            throw new IllegalArgumentException("Invalid opcode: " + opcode);
        }

        opcodeCounts[opcode]++;

        int category = getCategoryIndex(opcode);
        if (category >= 0) {
            categoryCounts[category]++;
        }
    }

    /**
     * Returns the category index based on the ISA opcode layout.
     *
     * 0x00 -> System & Control
     * 0x10 -> Arithmetic
     * 0x20 -> Bitwise & Shifts
     * 0x30 -> Branches & Subroutines
     * 0x40 -> Stack Operations
     * 0x50 -> Memory Operations
     */
    private int getCategoryIndex(int opcode) {
        int category = (opcode & 0xF0) >>> 4;

        return switch (category) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 5;
            default -> -1;
        };
    }

    public long timeMethod(Runnable runnable) {
        long startNs = System.nanoTime();
        runnable.run();
        return System.nanoTime() - startNs;
    }

    public void reset() {
        totalInstructions = 0;
        totalElapsedNs = 0;

        fetchNs = 0;
        decodeNs = 0;
        conditionNs = 0;
        executeNs = 0;
        pcAdvanceNs = 0;

        measurementStartNs = 0;
        measurementActive = false;

        Arrays.fill(opcodeCounts, 0);
        Arrays.fill(categoryCounts, 0);
    }

    public long getTotalInstructions() {
        return totalInstructions;
    }

    public long getTotalElapsedNs() {
        return totalElapsedNs;
    }

    public long getFetchNs() {
        return fetchNs;
    }

    public long getDecodeNs() {
        return decodeNs;
    }

    public long getConditionNs() {
        return conditionNs;
    }

    public long getExecuteNs() {
        return executeNs;
    }

    public long getPcAdvanceNs() {
        return pcAdvanceNs;
    }

    public long getOpcodeCount(OpCode opcode) {
        return opcodeCounts[opcode.binaryValue().intValue()];
    }

    public long[] getOpcodeCounts() {
        return opcodeCounts.clone();
    }

    public long getCategoryCount(int category) {
        if (category < 0 || category >= categoryCounts.length) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }

        return categoryCounts[category];
    }

    public double getInstructionsPerSecond() {
        if (totalElapsedNs <= 0 || totalInstructions <= 0) {
            return 0.0;
        }

        return totalInstructions * 1_000_000_000.0 / totalElapsedNs;
    }

    public double getMips() {
        return getInstructionsPerSecond() / 1_000_000.0;
    }

    public double getAverageNsPerInstruction() {
        if (totalInstructions <= 0) {
            return 0.0;
        }

        return (double) totalElapsedNs / totalInstructions;
    }

    public static String humanReadableRate(double hz) {
        if (hz >= 1_000_000_000.0) {
            return String.format("%.3f GHz", hz / 1_000_000_000.0);
        }
        if (hz >= 1_000_000.0) {
            return String.format("%.3f MHz", hz / 1_000_000.0);
        }
        if (hz >= 1_000.0) {
            return String.format("%.3f kHz", hz / 1_000.0);
        }
        return String.format("%.3f Hz", hz);
    }

    public void printSummary() {
        long stageTotalNs =
                fetchNs +
                        decodeNs +
                        conditionNs +
                        executeNs +
                        pcAdvanceNs;

        long unaccountedNs = Math.max(0, totalElapsedNs - stageTotalNs);

        System.out.println("=== Execution Profile ===");
        System.out.println();

        System.out.printf("Instructions:        %,d%n", totalInstructions);
        System.out.printf("Elapsed Time:        %.3f ms%n", totalElapsedNs / 1_000_000.0);
        double ips = getInstructionsPerSecond();
        System.out.printf("Instructions/sec:    %,.2f (%s)%n", ips, humanReadableRate(ips));
        System.out.printf("Effective MIPS:      %.3f%n", getMips());
        System.out.printf("Average/instruction: %.2f ns%n", getAverageNsPerInstruction());

        System.out.println();

        System.out.println("=== Pipeline Timing ===");
        System.out.printf("%-16s %12s %9s%n", "Stage", "Time", "Share");
        System.out.println("-------------------------------------------");

        printStage("Fetch", fetchNs, stageTotalNs);
        printStage("Decode", decodeNs, stageTotalNs);
        printStage("Condition", conditionNs, stageTotalNs);
        printStage("Execute", executeNs, stageTotalNs);
        printStage("PC Advance", pcAdvanceNs, stageTotalNs);

        if (totalElapsedNs > stageTotalNs) {
            double percentage = totalElapsedNs > 0
                    ? unaccountedNs * 100.0 / totalElapsedNs
                    : 0.0;

            System.out.printf(
                    "%-16s %9.3f ms %8.2f%%%n",
                    "Unaccounted",
                    unaccountedNs / 1_000_000.0,
                    percentage
            );
        }

        System.out.println();

        printCategorySummary();
        printOpcodeSummary();

        System.out.println();
    }

    private void printStage(String name, long timeNs, long stageTotalNs) {
        double percentage = stageTotalNs > 0
                ? timeNs * 100.0 / stageTotalNs
                : 0.0;

        System.out.printf(
                "%-16s %9.3f ms %8.2f%%%n",
                name,
                timeNs / 1_000_000.0,
                percentage
        );
    }

    private void printCategorySummary() {
        System.out.println("=== Instruction Categories ===");
        System.out.printf("%-24s %14s %9s%n", "Category", "Instructions", "Share");
        System.out.println("----------------------------------------------------");

        long totalExecuted = getTotalExecutedOpcodes();

        for (int i = 0; i < categoryCounts.length; i++) {
            long count = categoryCounts[i];

            if (count == 0) {
                continue;
            }

            double percentage = totalExecuted > 0
                    ? count * 100.0 / totalExecuted
                    : 0.0;

            System.out.printf(
                    "%-24s %,14d %8.2f%%%n",
                    CATEGORY_NAMES[i],
                    count,
                    percentage
            );
        }
    }

    private void printOpcodeSummary() {
        System.out.println();
        System.out.println("=== Opcode Usage ===");
        System.out.printf("%-10s %-8s %14s %9s%n", "Opcode", "Hex", "Count", "Share");
        System.out.println("------------------------------------------------");

        long totalExecuted = getTotalExecutedOpcodes();

        for (OpCode opcode : OpCode.values()) {
            long count = getOpcodeCount(opcode);

            if (count == 0) {
                continue;
            }

            double percentage = totalExecuted > 0
                    ? count * 100.0 / totalExecuted
                    : 0.0;

            System.out.printf(
                    "%-10s 0x%02X %,14d %8.2f%%%n",
                    opcode.name(),
                    opcode.binaryValue().intValue(),
                    count,
                    percentage
            );
        }
    }

    private long getTotalExecutedOpcodes() {
        long total = 0;

        for (long count : opcodeCounts) {
            total += count;
        }

        return total;
    }
}