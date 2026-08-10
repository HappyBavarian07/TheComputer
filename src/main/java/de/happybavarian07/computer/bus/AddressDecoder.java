package de.happybavarian07.computer.bus;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.exceptions.bus.BusConfigurationException;
import de.happybavarian07.computer.util.Architecture;

import java.util.ArrayList;
import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 13:31
 */
public class AddressDecoder {
    private final List<DeviceMapping> deviceMappings;

    public AddressDecoder() {
        this.deviceMappings = new ArrayList<>();
    }

    public void registerDevice(Address startAddress, Address endAddress, BusDevice device) {
        if (startAddress.getAsInt() > endAddress.getAsInt() || startAddress.getAsInt() < 0 || endAddress.getAsInt() > Architecture.MEMORY_SIZE_BYTES - 1)
            throw new BusConfigurationException("Device '" + device.getName() + "' tried to request invalid Address range from " +
                    startAddress.getAsHexaDecString() + " (" + startAddress.getAsInt() + ") to " + endAddress.getAsHexaDecString() + " (" + endAddress.getAsInt() + ").");

        BusDevice overlappingDevice = findDeviceFromAddress(startAddress, endAddress);
        if (overlappingDevice != null)
            throw new BusConfigurationException("Address collision between '" + device.getName() + "' (NEW) and '" + overlappingDevice.getName() + "' (EXISTING)");

        deviceMappings.add(new DeviceMapping(startAddress.getAsInt(), endAddress.getAsInt(), device));
    }

    private BusDevice findDeviceFromAddress(Address startAddress, Address endAddress) {
        for (int i = 0; i < deviceMappings.size(); i++) {
            if (startAddress.getAsInt() <= deviceMappings.get(i).endAddress && endAddress.getAsInt() >= deviceMappings.get(i).startAddress) {
                return deviceMappings.get(i).device;
            }
        }
        return null;
    }

    public BusDevice findDevice(Address address) {
        if (address.getAsInt() < 0 || address.getAsInt() > Architecture.MEMORY_SIZE_BYTES - 1)
            throw new BusConfigurationException("Tried to find Device with an invalid Address " +
                    address.getAsHexaDecString() + " (" + address.getAsInt() + ").");

        return findDeviceFromAddress(address, address);
    }


    record DeviceMapping(int startAddress, int endAddress, BusDevice device) {
    }
}
