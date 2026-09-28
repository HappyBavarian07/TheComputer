package de.happybavarian07.computer.memory.ram;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.arithmetic.WordAdderSubtractor;
import de.happybavarian07.computer.core.byteclass.Byte;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

import java.util.Arrays;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 01:17
 */
public class Ram {
    private final byte[] memory;
    private final int capacityBytes;

    public Ram() {
        this(Architecture.MEMORY_SIZE_BYTES);
    }

    public Ram(int capacityBytes) {
        if (capacityBytes <= 0 || capacityBytes > Architecture.MEMORY_SIZE_BYTES) {
            throw new IllegalArgumentException("Invalid RAM capacity: " + capacityBytes);
        }
        this.capacityBytes = capacityBytes;
        this.memory = new byte[capacityBytes];
        reset();
    }

    public void reset() {
        Arrays.fill(this.memory, (byte) 0);
    }

    public int getCapacityBytes() {
        return capacityBytes;
    }

    public void readByte(Address address, Byte destination) {
        int addr = address.getAsInt();
        if (addr < 0 || addr >= capacityBytes) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + addr);
        }

        destination.set(memory[addr]);
    }

    public void writeByte(Address address, Byte source) {
        int addr = address.getAsInt();
        if (addr < 0 || addr >= capacityBytes) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + addr);
        }

        memory[addr] = (byte) (source.getAsInt() & 0xFF);
    }

    public void readWord(Address address, Word destination) {
        read(address, destination, Architecture.INSTRUCTION_BYTES);
    }

    public void writeWord(Address address, Word source) {
        write(address, source, Architecture.INSTRUCTION_BYTES);
    }

    public void read(Address address, Word destination, int byteCount) {
        int baseAddr = address.getAsInt();
        // Need byteCount bytes for a Word: valid baseAddr is 0 .. capacityBytes-byteCount
        if (baseAddr < 0 || baseAddr > capacityBytes - byteCount) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + baseAddr);
        }

        long raw = 0L;
        for (int k = 0; k < byteCount; k++) {
            long b = memory[baseAddr + k] & 0xFFL;
            raw |= (b << (k * 8)); // little-endian
        }
        destination.set(raw);
    }

    public void write(Address address, Word source, int byteCount) {
        int baseAddr = address.getAsInt();
        // Need byteCount bytes for a Word: valid baseAddr is 0 .. capacityBytes-byteCount
        if (baseAddr < 0 || baseAddr > capacityBytes - byteCount) {
            throw new IndexOutOfBoundsException("Tried to access RAM outside address space: " + baseAddr);
        }
        for (int k = 0; k < byteCount; k++) {
            memory[baseAddr + k] = (byte) (source.getAsLong() >>> (k * 8) & 0xFF);
        }
    }
}
