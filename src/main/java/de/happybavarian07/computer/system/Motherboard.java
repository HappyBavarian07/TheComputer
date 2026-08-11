package de.happybavarian07.computer.system;

import de.happybavarian07.computer.bus.SystemBus;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.cpu.Cpu;
import de.happybavarian07.computer.memory.ram.RamBusDevice;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 23:16
 */
public class Motherboard {
    private final SystemBus systemBus;
    private final Cpu cpu;
    private final RamBusDevice ramBusDevice;

    public Motherboard() {
        this.systemBus = new SystemBus();
        this.ramBusDevice = new RamBusDevice(Architecture.MEMORY_FREE_END + 1);
        this.systemBus.registerDevice(new Address(0x0000), new Address(Architecture.MEMORY_FREE_END), ramBusDevice);
        this.cpu = new Cpu();
    }

    public void powerOn() {
        reset();
    }

    public void reset() {
        systemBus.reset();
        cpu.reset();
    }

    public void stepSystem() {
        cpu.step();
    }

    public void runSystem() {
        cpu.run();
    }

    public Cpu getCpu() {
        return cpu;
    }

    public SystemBus getSystemBus() {
        return systemBus;
    }

    public RamBusDevice getRamBusDevice() {
        return ramBusDevice;
    }
}
