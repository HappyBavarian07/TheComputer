package de.happybavarian07.computer.gui;

import javax.swing.SwingUtilities;

public final class ComputerWorkbenchLauncher {
    private ComputerWorkbenchLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ComputerWorkbench frame = new ComputerWorkbench();
            frame.setVisible(true);
        });
    }
}
