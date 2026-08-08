package de.happybavarian07.computer.core.byteclass;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 12:56
 */
public final class Byte extends FixedWidthBits {
    public Byte() {
        super(8);
    }

    public Byte(Bit[] bitArray) {
        super(8, bitArray);
    }

    public Byte(Number number) {
        super(8, number);
    }
}