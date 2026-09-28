package de.happybavarian07.computer.isa;

/**
 * How the 32-bit immediate field of an instruction is interpreted. One kind per {@link OpCode}; the assembler range
 * check, the CPU extension to 64 bits and the disassembler formatting all read it from here.
 */
public enum ImmediateKind {
    NONE(0L, 0L, "no immediate"),
    SIGNED32(Integer.MIN_VALUE, Integer.MAX_VALUE, "signed 32-bit"),
    UNSIGNED32(0L, 0xFFFFFFFFL, "unsigned 32-bit"),
    SHIFT(0L, 63L, "shift amount"),
    ADDRESS(0L, 0xFFFFFFFFL, "32-bit address");

    private final long min;
    private final long max;
    private final String description;

    ImmediateKind(long min, long max, String description) {
        this.min = min;
        this.max = max;
        this.description = description;
    }

    public long min() {
        return min;
    }

    public long max() {
        return max;
    }

    public boolean fits(long value) {
        return value >= min && value <= max;
    }

    public String describeRange() {
        return description + " " + min + ".." + max;
    }

    // raw = the 32 field bits as decoded; SIGNED32 sign-extends to 64 bits, everything else zero-extends
    public long extend(int raw) {
        return this == SIGNED32 ? raw : raw & 0xFFFFFFFFL;
    }

    // text that the assembler reads back to the same field bits
    public String format(int raw) {
        if (this == SIGNED32 && raw < 0) {
            return Integer.toString(raw);
        }
        return String.format("0x%08X", raw);
    }
}
