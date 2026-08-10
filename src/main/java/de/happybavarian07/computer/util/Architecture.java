package de.happybavarian07.computer.util;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 19:35
 */
public class Architecture {

    private Architecture() {}

    // Data unit sizes in bits
    public static final int BYTE_BITS = 8; // Architecture.BYTE_BITS
    public static final int ADDRESS_BITS = 16; // Architecture.ADDRESS_BITS
    public static final int WORD_BITS = 32; // Architecture.WORD_BITS

    // System sizes
    public static final int GPR_COUNT = 32; // General-purpose Registers // Architecture.GENERAL_REGISTER_COUNT
    public static final int MEMORY_SIZE_BYTES = 65536; // Architecture.MEMORY_SIZE_BYTES
    public static final int MEMORY_FREE_END = MEMORY_SIZE_BYTES - 0x1000; // Reserved Space for BIOS and MMIO   // Architecture.MEMORY_FREE_END
}
