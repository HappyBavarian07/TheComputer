package de.happybavarian07.computer.gui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Central palette and font definitions for the workbench GUI (Calm Slate theme).
 *
 * <p>Colors were previously duplicated as inline {@code new Color(...)} literals
 * across the workbench. Centralizing them here is the single source of truth: a
 * palette change happens in one place, and every panel reads the same values.
 */
public final class Theme {

    private Theme() {
    }

    // --- surfaces / backgrounds ---
    /** Application base background. */
    public static final Color BG_BASE = new Color(20, 23, 29);
    /** Surface for inputs, cells, and raised panels. */
    public static final Color BG_SURFACE = new Color(30, 34, 42);
    /** Deepest background (code editor). */
    public static final Color BG_DEEP = new Color(16, 18, 23);
    /** Chip / stage-label background. */
    public static final Color BG_CHIP = new Color(38, 43, 52);
    /** Button background. */
    public static final Color BG_BUTTON = new Color(46, 52, 63);

    // --- accent ---
    /** Single primary accent (caret, focus, selection, opcodes). */
    public static final Color ACCENT = new Color(91, 143, 214);
    /** Muted accent fill (enabled chips). */
    public static final Color ACCENT_FILL = new Color(56, 84, 140);

    // --- borders ---
    /** Grid / line borders. */
    public static final Color BORDER = new Color(54, 60, 71);

    // --- text ---
    /** Primary text. */
    public static final Color TEXT = new Color(214, 219, 228);
    /** Brighter text (values). */
    public static final Color TEXT_BRIGHT = new Color(222, 227, 235);
    /** Button text. */
    public static final Color TEXT_BUTTON = new Color(232, 236, 242);
    /** Secondary text. */
    public static final Color TEXT_SECONDARY = new Color(154, 164, 178);
    /** Faint / placeholder text. */
    public static final Color TEXT_FAINT = new Color(107, 116, 130);
    /** Titled-border and heading accent. */
    public static final Color TITLE = new Color(127, 168, 221);

    // --- syntax highlighting ---
    public static final Color SYN_OPCODE = new Color(91, 143, 214);
    public static final Color SYN_LABEL = new Color(127, 168, 221);
    public static final Color SYN_DIRECTIVE = new Color(207, 159, 99);
    public static final Color SYN_REGISTER = new Color(157, 147, 196);
    public static final Color SYN_NUMBER = new Color(207, 138, 138);
    public static final Color SYN_STRING = new Color(130, 171, 134);
    public static final Color SYN_COMMENT = new Color(107, 116, 130);

    // --- error emphasis ---
    public static final Color ERROR_FG = new Color(214, 150, 150);
    public static final Color ERROR_BG = new Color(92, 42, 46);

    // --- fonts ---
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font MONO_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 12);
}
