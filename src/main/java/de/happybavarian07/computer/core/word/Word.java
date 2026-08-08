package de.happybavarian07.computer.core.word;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.bit.FixedWidthBits;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 13:10
 */
public final class Word extends FixedWidthBits {
    public Word() {
        super(Architecture.WORD_BITS);
    }

    public Word(Bit[] bitArray) {
        super(Architecture.WORD_BITS, bitArray);
    }

    public Word(Number number) {
        super(Architecture.WORD_BITS, number);
    }
}
