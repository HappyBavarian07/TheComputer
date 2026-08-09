package de.happybavarian07.computer.memory.ram;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 14:54
 */
class RamBusDeviceTest {
    private RamBusDevice ramBusDevice;
    private Address address;
    private Word wordSource;
    private Word wordDestination;

    @BeforeEach
    void setUp() {
        ramBusDevice = new RamBusDevice();
        address = new Address(0);
        wordSource = new Word(0);
        wordDestination = new Word(0);
    }

    @Test
    void testRamBusDeviceReadWrite() {
        address.set(0x0100);
        wordSource.set(0x01234567);

        ramBusDevice.write(address, wordSource);
        ramBusDevice.read(address, wordDestination);

        assertEquals(0x01234567, wordDestination.getAsInt());
    }

    @Test
    void testRamBusDeviceName() {
        assertEquals("RAM_BUS_01", ramBusDevice.getName());
    }
}
