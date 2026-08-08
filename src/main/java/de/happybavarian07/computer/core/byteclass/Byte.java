package de.happybavarian07.computer.core.byteclass;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 12:56
 */
public final class Byte extends FixedWidthBits {
    public Byte() {
        super(Architecture.BYTE_BITS);
    }

    public Byte(Bit[] bitArray) {
        super(Architecture.BYTE_BITS, bitArray);
    }

    public Byte(Number number) {
        super(Architecture.BYTE_BITS, number);
    }
}