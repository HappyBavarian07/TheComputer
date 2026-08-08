package de.happybavarian07.computer.cpu.registers;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 00:57
 */
class SpecialRegistersTest {

    private SpecialRegisters specialRegisters;
    private Address addrDest;
    private Address addrSrc;
    private Word wordDest;
    private Word wordSrc;
    private Bit flagBit;

    @BeforeEach
    void setUp() {
        specialRegisters = new SpecialRegisters();
        addrDest = new Address(0);
        addrSrc = new Address(0);
        wordDest = new Word(0);
        wordSrc = new Word(0);
        flagBit = new Bit(false);
    }

    @Test
    void testPcReadWrite() {
        addrSrc.set(0x1234);
        specialRegisters.writePC(addrSrc);
        specialRegisters.readPC(addrDest);
        assertEquals(0x1234, addrDest.getAsInt());
    }

    @Test
    void testSpReadWrite() {
        addrSrc.set(0xFFFE);
        specialRegisters.writeSP(addrSrc);
        specialRegisters.readSP(addrDest);
        assertEquals(0xFFFE, addrDest.getAsInt());
    }

    @Test
    void testIrReadWrite() {
        wordSrc.set(0xDEADBEEF);
        specialRegisters.writeIR(wordSrc);
        specialRegisters.readIR(wordDest);
        assertEquals((int) 0xDEADBEEFL, wordDest.getAsInt());
    }

    @Test
    void testFlagsWriteAndRead() {
        specialRegisters.writeFlags(true, false, true, false);

        assertTrue(specialRegisters.isZero());
        assertFalse(specialRegisters.isNegative());
        assertTrue(specialRegisters.isCarry());
        assertFalse(specialRegisters.isOverflow());

        specialRegisters.getFlagZ(flagBit);
        assertTrue(flagBit.getAsBool());

        specialRegisters.getFlagN(flagBit);
        assertFalse(flagBit.getAsBool());
    }

    @Test
    void testReset() {
        addrSrc.set(0x1000);
        specialRegisters.writePC(addrSrc);
        addrSrc.set(0x8000);
        specialRegisters.writeSP(addrSrc);
        wordSrc.set(0x12345678);
        specialRegisters.writeIR(wordSrc);
        specialRegisters.writeFlags(true, true, true, true);

        specialRegisters.reset();

        specialRegisters.readPC(addrDest);
        assertEquals(0, addrDest.getAsInt());

        specialRegisters.readSP(addrDest);
        assertEquals(0, addrDest.getAsInt());

        specialRegisters.readIR(wordDest);
        assertEquals(0, wordDest.getAsInt());

        assertFalse(specialRegisters.isZero());
        assertFalse(specialRegisters.isNegative());
        assertFalse(specialRegisters.isCarry());
        assertFalse(specialRegisters.isOverflow());
    }
}
