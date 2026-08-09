package de.happybavarian07.computer.bus;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 14:13
 */
public class SystemBus {
    private AddressDecoder addressDecoder;
    private Address addressBus;
    private Word dataBus;
    private Bit readEnable, writeEnable;

    public SystemBus() {
        addressDecoder = new AddressDecoder();
        addressBus = new Address();
        dataBus = new Word();
        readEnable = new Bit(false);
        writeEnable = new Bit(false);
    }

    public void registerDevice(Address startAddress, Address endAddress, BusDevice device) {
        addressDecoder.registerDevice(startAddress, endAddress, device);
    }

    public AddressDecoder getAddressDecoder() {
        return addressDecoder;
    }

    public void read(Address address, Word destination) {
        if (readEnable.getAsBool() || writeEnable.getAsBool()) {
            throw new BusBusyException("Cannot initiate bus transaction: SystemBus is already active (readEnable=" + readEnable.getAsBool() + ", writeEnable=" + writeEnable.getAsBool() + ")");
        }
        try {
            readEnable.set(true);
            writeEnable.set(false);
            addressBus.set(address);
            BusDevice device = addressDecoder.findDevice(addressBus);
            if (device == null) {
                throw new BusFaultException("No device mapped at address " + addressBus.getAsHexaDecString());
            }
            device.read(addressBus, dataBus);
            destination.set(dataBus);
        } finally {
            readEnable.set(false);
        }
    }

    public void write(Address address, Word source) {
        if (readEnable.getAsBool() || writeEnable.getAsBool()) {
            throw new BusBusyException("Cannot initiate bus transaction: SystemBus is already active (readEnable=" + readEnable.getAsBool() + ", writeEnable=" + writeEnable.getAsBool() + ")");
        }
        try {
            readEnable.set(false);
            writeEnable.set(true);
            addressBus.set(address);
            dataBus.set(source);
            BusDevice device = addressDecoder.findDevice(addressBus);
            if (device == null) {
                throw new BusFaultException("No device mapped at address " + addressBus.getAsHexaDecString());
            }
            device.write(addressBus, dataBus);
        } finally {
            writeEnable.set(false);
        }
    }
}
