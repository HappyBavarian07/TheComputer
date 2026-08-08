package de.happybavarian07.computer.core.address;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 13:19
 */
public class Address extends FixedWidthBits {
    public Address(Bit[] bitArray) {
        super(16, bitArray);
    }
}
