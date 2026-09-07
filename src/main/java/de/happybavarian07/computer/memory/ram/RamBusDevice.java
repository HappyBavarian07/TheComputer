package de.happybavarian07.computer.memory.ram;

import de.happybavarian07.computer.bus.BusDevice;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 14:49
 */
public class RamBusDevice implements BusDevice {
    private final Ram ram;

    public RamBusDevice() {
        this.ram = new Ram(Architecture.MEMORY_FREE_END + 1);
    }

    public RamBusDevice(int capacityBytes) {
        this.ram = new Ram(capacityBytes);
    }

    @Override
    public void read(Address address, Word destination, int byteCount) {
        ram.read(address, destination, byteCount);
    }

    @Override
    public void write(Address address, Word source, int byteCount) {
        ram.write(address, source, byteCount);
    }

    @Override
    public void reset() {
        ram.reset();
    }

    @Override
    public String getName() {
        return "RAM_BUS_01";
    }
}
