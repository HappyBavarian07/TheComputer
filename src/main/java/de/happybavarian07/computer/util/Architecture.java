package de.happybavarian07.computer.util;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 19:35
 */
public class Architecture {

    private Architecture() {
    }

    // Data unit sizes in bits
    public static final int BYTE_BITS = 8; // Architecture.BYTE_BITS
    public static final int INSTRUCTION_BYTES = 8; // Architecture.INSTRUCTION_BYTES
    public static final int ADDRESS_BITS = 32; // Architecture.ADDRESS_BITS
    public static final int WORD_BITS = 64; // Architecture.WORD_BITS
    public static final int WORD_BYTES = WORD_BITS / BYTE_BITS; // Architecture.WORD_BYTES

    // System sizes
    public static final int GPR_COUNT = 32; // General-purpose Registers // Architecture.GENERAL_REGISTER_COUNT
    public static final int MEMORY_SIZE_BYTES = 64 * 1024 * 1024; // 64 MiB // Architecture.MEMORY_SIZE_BYTES
    // Reserved Space for BIOS and MMIO
    public static final int MEMORY_FREE_END = MEMORY_SIZE_BYTES - 0x1000;   // Architecture.MEMORY_FREE_END

    // Stack
    public static final int STACK_BASE_ADDRESS = MEMORY_FREE_END - INSTRUCTION_BYTES; // Architecture.STACK_BASE_ADDRESS
    public static final int STACK_LIMIT_ADDRESS = MEMORY_SIZE_BYTES / 2; // Architecture.STACK_LIMIT_ADDRESS
}
