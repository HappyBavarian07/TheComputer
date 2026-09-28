package de.happybavarian07.computer.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import de.happybavarian07.computer.gui.theme.Theme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public final class ComputerWorkbenchLauncher {
    private ComputerWorkbenchLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installTheme();
            WorkbenchFrame frame = new WorkbenchFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Installs FlatLaf's dark look-and-feel for modern widget rendering
     * (real flat buttons, HiDPI scaling, consistent chrome across
     * platforms), then layers the existing "Calm Slate" palette on top via
     * FlatLaf's global {@code @}-prefixed keys so the app keeps its color
     * identity instead of FlatLaf's stock blue accent.
     */
    private static void installTheme() {
        FlatDarkLaf.setup();

        UIManager.put("@background", Theme.BG_BASE);
        UIManager.put("@foreground", Theme.TEXT);
        UIManager.put("@accentColor", Theme.ACCENT);
        UIManager.put("@selectionBackground", Theme.ACCENT);
        UIManager.put("@selectionForeground", Color.WHITE);

        UIManager.put("TextArea.background", Theme.BG_SURFACE);
        UIManager.put("TextArea.foreground", Theme.TEXT);
        UIManager.put("TextArea.caretForeground", Theme.ACCENT);
        UIManager.put("TextField.background", Theme.BG_SURFACE);
        UIManager.put("TextField.foreground", Theme.TEXT);
        UIManager.put("TextPane.background", Theme.BG_DEEP);
        UIManager.put("TextPane.foreground", Theme.TEXT_BRIGHT);
        UIManager.put("TextPane.caretForeground", Theme.ACCENT);
        UIManager.put("Table.gridColor", Theme.BORDER);
        UIManager.put("Table.background", Theme.BG_BASE);
        UIManager.put("Table.foreground", Theme.TEXT);
        UIManager.put("Table.selectionBackground", Theme.ACCENT);
        UIManager.put("TitledBorder.titleColor", Theme.TITLE);
        UIManager.put("Label.font", new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 14));
        UIManager.put("TextArea.font", Theme.MONO);
        UIManager.put("Table.font", Theme.MONO);
    }
}
