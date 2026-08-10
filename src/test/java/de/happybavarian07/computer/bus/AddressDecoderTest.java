package de.happybavarian07.computer.bus;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.exceptions.bus.BusConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 14:52
 */
class AddressDecoderTest {
    private AddressDecoder decoder;
    private DummyBusDevice device1;
    private DummyBusDevice device2;

    @BeforeEach
    void setUp() {
        decoder = new AddressDecoder();
        device1 = new DummyBusDevice("DEVICE_1");
        device2 = new DummyBusDevice("DEVICE_2");
    }

    @Test
    void testRegisterDeviceAndFind() {
        Address start = new Address(0x0000);
        Address end = new Address(0x7FFF);
        decoder.registerDevice(start, end, device1);

        Address lookupAddr = new Address(0x1000);
        BusDevice found = decoder.findDevice(lookupAddr);
        assertEquals(device1, found);
    }

    @Test
    void testUnmappedAddressReturnsNull() {
        Address start = new Address(0x0000);
        Address end = new Address(0x00FF);
        decoder.registerDevice(start, end, device1);

        Address lookupAddr = new Address(0x0100);
        assertNull(decoder.findDevice(lookupAddr));
    }

    @Test
    void testAddressCollision() {
        Address start1 = new Address(0x0000);
        Address end1 = new Address(0x1000);
        decoder.registerDevice(start1, end1, device1);

        Address start2 = new Address(0x0800);
        Address end2 = new Address(0x2000);

        assertThrows(BusConfigurationException.class, () -> decoder.registerDevice(start2, end2, device2));
    }

    @Test
    void testInvalidAddressRange() {
        Address start = new Address(0x5000);
        Address end = new Address(0x1000); // Invalid: start > end

        assertThrows(BusConfigurationException.class, () -> decoder.registerDevice(start, end, device1));
    }

    private static class DummyBusDevice implements BusDevice {
        private final String name;

        public DummyBusDevice(String name) {
            this.name = name;
        }

        @Override
        public void read(Address address, Word destination) {
        }

        @Override
        public void write(Address address, Word source) {
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
