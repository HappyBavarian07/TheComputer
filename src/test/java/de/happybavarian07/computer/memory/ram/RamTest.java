package de.happybavarian07.computer.memory.ram;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.byteclass.Byte;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 01:36
 */
class RamTest {
    private Ram ram;
    private Address address;
    private Byte byteDestination, byteSource;
    private Word wordDestination, wordSource;

    @BeforeEach
    void setUp() {
        ram = new Ram();
        address = new Address(0);
        byteDestination = new Byte(0);
        byteSource = new Byte(0);
        wordDestination = new Word(0);
        wordSource = new Word(0);
    }

    @Test
    void testByteReadWrite() {
        byteSource.set(0x42);
        address.set(0x0000);
        ram.writeByte(address, byteSource);
        ram.readByte(address, byteDestination);
        assertEquals(0x42, byteDestination.getAsInt());

        byteSource.set(0xAB);
        address.set(0x8000);
        ram.writeByte(address, byteSource);
        ram.readByte(address, byteDestination);
        assertEquals(0xAB, byteDestination.getAsInt());

        byteSource.set(0xFF);
        address.set(0xFFFF);
        ram.writeByte(address, byteSource);
        ram.readByte(address, byteDestination);
        assertEquals(0xFF, byteDestination.getAsInt());
    }

    @Test
    void testWorldLittleEndian() {
        wordSource.set(0x12345678);
        address.set(0x1000);
        Address offsetAddress = new Address();
        ram.writeWord(address, wordSource);
        ram.readByte(address, byteDestination);
        assertEquals(0x78, byteDestination.getAsInt(), "Byte 0 doesnt match.");
        address.offset(1, offsetAddress);
        ram.readByte(offsetAddress, byteDestination);
        assertEquals(0x56, byteDestination.getAsInt(), "Byte 1 doesnt match.");
        address.offset(2, offsetAddress);
        ram.readByte(offsetAddress, byteDestination);
        assertEquals(0x34, byteDestination.getAsInt(), "Byte 2 doesnt match.");
        address.offset(3, offsetAddress);
        ram.readByte(offsetAddress, byteDestination);
        assertEquals(0x12, byteDestination.getAsInt(), "Byte 3 doesnt match.");

        ram.readWord(address, wordDestination);
        System.out.println("Ram at memory address " + address.getAsHexaDecString() + ": " + wordDestination.getAsHexaDecString());
    }

    @Test
    void testWordReadWrite() {
        wordSource.set(0xDEADBEEF);
        address.set(0x2000);
        ram.writeWord(address, wordSource);
        ram.readWord(address, wordDestination);
        assertEquals((int) 0xDEADBEEFL, wordDestination.getAsInt());
    }

    @Test
    void testOutOfBounds() {
        address.set(65535);
        assertThrows(IndexOutOfBoundsException.class, () -> ram.readWord(address, wordDestination));
        assertThrows(IndexOutOfBoundsException.class, () -> ram.writeWord(address, wordSource));

        address.set(65533);
        assertThrows(IndexOutOfBoundsException.class, () -> ram.readWord(address, wordDestination));
    }

    @Test
    void testReset() {
        address.set(0x1000);
        byteSource.set(0xFF);
        ram.writeByte(address, byteSource);

        address.set(0x2000);
        wordSource.set(0x12345678);
        ram.writeWord(address, wordSource);

        ram.reset();

        address.set(0x1000);
        ram.readByte(address, byteDestination);
        assertEquals(0, byteDestination.getAsInt());

        address.set(0x2000);
        ram.readWord(address, wordDestination);
        assertEquals(0, wordDestination.getAsInt());
    }

    @Test
    void testRandomFuzzing() {
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < 1000; i++) {
            int randomVal = random.nextInt();
            int randomAddr = random.nextInt(65532);
            address.set(randomAddr);
            wordSource.set(randomVal);
            ram.writeWord(address, wordSource);
            ram.readWord(address, wordDestination);
            assertEquals(randomVal, wordDestination.getAsInt(), "Fuzzing failed at address " + randomAddr);
        }
    }
}