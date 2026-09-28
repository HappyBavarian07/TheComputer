package de.happybavarian07.computer.gui.util;

/** Small parsing/formatting helpers shared across workbench panels. */
public final class NumberFormats {
    private NumberFormats() {
    }

    /** Accepts {@code 0x...}, {@code 0b...}, plain decimal, or bare hex digits. */
    public static Integer parseFlexibleInteger(String value) {
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

    public static String formatRegisterValue(int value) {
        return value + " (0x" + Integer.toHexString(value).toUpperCase() + ")";
    }

    public static String formatRegisterValue(long value) {
        return value + " (0x" + Long.toHexString(value).toUpperCase() + ")";
    }
}
