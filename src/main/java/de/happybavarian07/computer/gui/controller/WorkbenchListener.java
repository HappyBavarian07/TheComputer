package de.happybavarian07.computer.gui.controller;

/**
 * Observer for {@link WorkbenchController} state changes. Panels implement
 * this (or a lambda) instead of reaching into the CPU directly, so all
 * mutation flows through the controller and every view stays in sync.
 */
public interface WorkbenchListener {
    /** Registers, memory, flags, PC, or halted state may have changed. */
    void onStateChanged();

    /** A human-readable line for the execution log. */
    void onLog(String message);
}
