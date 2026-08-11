package de.happybavarian07.computer.bus;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.exceptions.bus.BusFaultException;
import de.happybavarian07.computer.memory.ram.RamBusDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 14:53
 */
class SystemBusTest {
    private SystemBus systemBus;
    private RamBusDevice ramDevice;
    private Address address;
    private Word wordSource;
    private Word wordDestination;

    @BeforeEach
    void setUp() {
        systemBus = new SystemBus();
        ramDevice = new RamBusDevice();
        address = new Address(0);
        wordSource = new Word(0);
        wordDestination = new Word(0);

        systemBus.registerDevice(new Address(0x0000), new Address(0xEFFF), ramDevice);
    }

    @Test
    void testSystemBusReadWrite() {
        address.set(0x1000);
        wordSource.set(0xDEADBEEF);

        systemBus.write(address, wordSource);
        systemBus.read(address, wordDestination);

        assertEquals((int) 0xDEADBEEFL, wordDestination.getAsInt());
    }

    @Test
    void testUnmappedAddressFault() {
        address.set(0xF000);
        wordSource.set(0x12345678);

        assertThrows(BusFaultException.class, () -> systemBus.read(address, wordDestination));
        assertThrows(BusFaultException.class, () -> systemBus.write(address, wordSource));
    }

    @Test
    void testMultiDeviceRouting() {
        MockRomDevice romDevice = new MockRomDevice();
        systemBus.registerDevice(new Address(0xF000), new Address(0xF7FF), romDevice);

        address.set(0xF000);
        systemBus.read(address, wordDestination);
        assertEquals(0xCAFEBABE, wordDestination.getAsInt());
    }

    @Test
    void testRandomFuzzingOnBus() {
        java.util.Random random = new java.util.Random(12345);
        for (int i = 0; i < 500; i++) {
            int randomVal = random.nextInt();
            int randomAddr = random.nextInt(0xE000);
            address.set(randomAddr);
            wordSource.set(randomVal);

            systemBus.write(address, wordSource);
            systemBus.read(address, wordDestination);

            assertEquals(randomVal, wordDestination.getAsInt(), "SystemBus Fuzzing failed at address " + randomAddr);
        }
    }

    @Test
    void testSystemBusReset() {
        address.set(0x1000);
        wordSource.set(0x42);
        systemBus.write(address, wordSource);

        systemBus.reset();

        systemBus.read(address, wordDestination);
        assertEquals(0, wordDestination.getAsInt());
    }

    private static class MockRomDevice implements BusDevice {
        @Override
        public void read(Address address, Word destination) {
            destination.set(0xCAFEBABE);
        }

        @Override
        public void write(Address address, Word source) {
            throw new UnsupportedOperationException("Cannot write to ROM");
        }

        @Override
        public String getName() {
            return "MOCK_ROM";
        }
    }
}
