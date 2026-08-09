package de.happybavarian07.computer.bus;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 13:29
 */
public interface BusDevice {
    void read(Address address, Word destination);

    void write(Address address, Word source);

    default String getName() {
        return getClass().getSimpleName();
    }
}
