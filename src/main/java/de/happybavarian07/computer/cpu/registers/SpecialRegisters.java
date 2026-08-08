package de.happybavarian07.computer.cpu.registers;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 20:49
 */
public class SpecialRegisters {
    private final Address pc; // program counter
    private final Address sp; // stack pointer
    private final Word ir; // instruction register
    private final Bit flagZ, flagN, flagC, flagV; // flags (zero, negative, carry, overflow)

    public SpecialRegisters() {
        pc = new Address(0);
        sp = new Address(0);
        ir = new Word(0);
        flagZ = new Bit(false);
        flagN = new Bit(false);
        flagC = new Bit(false);
        flagV = new Bit(false);
    }

    public void readPC(Address destination) {
        destination.set(pc.getAsArray());
    }

    public void writePC(Address source) {
        pc.set(source.getAsArray());
    }

    public void readSP(Address destination) {
        destination.set(sp.getAsArray());
    }

    public void writeSP(Address source) {
        sp.set(source.getAsArray());
    }

    public void readIR(Word destination) {
        destination.set(ir.getAsArray());
    }

    public void writeIR(Word source) {
        ir.set(source.getAsArray());
    }

    public void writeFlags(Bit newFlagZ, Bit newFlagN, Bit newFlagC, Bit newFlagV) {
        writeFlags(newFlagZ.getAsBool(), newFlagN.getAsBool(), newFlagC.getAsBool(), newFlagV.getAsBool());
    }

    public void writeFlags(boolean newFlagZ, boolean newFlagN, boolean newFlagC, boolean newFlagV) {
        flagZ.set(newFlagZ);
        flagN.set(newFlagN);
        flagC.set(newFlagC);
        flagV.set(newFlagV);
    }

    public boolean isZero() {
        return flagZ.getAsBool();
    }

    public boolean isNegative() {
        return flagN.getAsBool();
    }

    public boolean isCarry() {
        return flagC.getAsBool();
    }

    public boolean isOverflow() {
        return flagV.getAsBool();
    }

    public void getFlagZ(Bit destination) {
        destination.set(flagZ.getAsBool());
    }

    public void getFlagN(Bit destination) {
        destination.set(flagN.getAsBool());
    }

    public void getFlagC(Bit destination) {
        destination.set(flagC.getAsBool());
    }

    public void getFlagV(Bit destination) {
        destination.set(flagV.getAsBool());
    }

    public void reset() {
        pc.set(0);
        sp.set(0);
        ir.set(0);
        writeFlags(false, false, false, false);
    }
}
