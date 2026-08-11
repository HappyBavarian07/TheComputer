package de.happybavarian07.computer.memory.ram;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.arithmetic.WordAdderSubtractor;
import de.happybavarian07.computer.core.byteclass.Byte;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 01:17
 */
public class Ram {
    private final Byte[] memory;
    private final int capacityBytes;

    public Ram() {
        this(Architecture.MEMORY_SIZE_BYTES);
    }

    public Ram(int capacityBytes) {
        if (capacityBytes <= 0 || capacityBytes > Architecture.MEMORY_SIZE_BYTES) {
            throw new IllegalArgumentException("Invalid RAM capacity: " + capacityBytes);
        }
        this.capacityBytes = capacityBytes;
        this.memory = new Byte[capacityBytes];
        for (int i = 0; i < capacityBytes; i++) {
            this.memory[i] = new Byte(0);
        }
    }

    public void reset() {
        for (int i = 0; i < capacityBytes; i++) {
            this.memory[i].set(0);
        }
    }

    public int getCapacityBytes() {
        return capacityBytes;
    }

    public void readByte(Address address, Byte destination) {
        int addr = address.getAsInt();
        if (addr < 0 || addr >= capacityBytes) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + addr);
        }

        destination.set(memory[addr].getAsArray());
    }

    public void writeByte(Address address, Byte source) {
        int addr = address.getAsInt();
        if (addr < 0 || addr >= capacityBytes) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + addr);
        }

        memory[addr].set(source.getAsArray());
    }

    public void readWord(Address address, Word destination) {
        int baseAddr = address.getAsInt();
        if (baseAddr < 0 || baseAddr >= capacityBytes - 3) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + baseAddr);
        }

        for (int k = 0; k < 4; k++) {
            int wordSliceStart = (3 - k) * 8;

            for (int j = 0; j < 8; j++) {
                destination.set(wordSliceStart + j, memory[baseAddr + k].get(j));
            }
        }
    }

    public void writeWord(Address address, Word source) {
        int baseAddr = address.getAsInt();
        if (baseAddr < 0 || baseAddr >= capacityBytes - 3) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + baseAddr);
        }
        for (int k = 0; k < 4; k++) {
            int wordSliceStart = (3 - k) * 8;

            for (int j = 0; j < 8; j++) {
                memory[baseAddr + k].set(j, source.get(wordSliceStart + j));
            }
        }
    }
}
