package de.happybavarian07.computer.core.word;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 13:10
 */
public final class Word extends FixedWidthBits {
    public Word(Bit[] bitArray) {
        super(32, bitArray);
    }
}
