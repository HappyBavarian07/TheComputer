package de.happybavarian07.computer.core.address;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 13:19
 */
public class Address extends FixedWidthBits {
    public Address() {
        super(Architecture.ADDRESS_BITS);
    }

    public Address(Bit[] bitArray) {
        super(Architecture.ADDRESS_BITS, bitArray);
    }

    public Address(Number number) {
        super(Architecture.ADDRESS_BITS, number);
    }
}
