package de.happybavarian07.computer.assembler.encoder.model;

public enum OperandMapping {
    NONE(0), // nothing
    RD_RS1_RS2(3), // destination reg, source1 reg, source2 reg
    RD_RS1_IMM32(3), // destination reg, source reg, target address
    RD_RS1(2), // destination reg, source reg
    RD_IMM32(2), // destination reg, target address
    IMM32_RD(2), // target address, destination reg
    RD_RS1_OFFSET32(3), // destination reg, source reg, target address
    IMM32_ONLY(1), // target address
    RS1_ONLY(1), // source reg
    RD_ONLY(1); // destination reg

    private final int arity;

    OperandMapping(int arity) {
        this.arity = arity;
    }

    public int arity() {
        return arity;
    }
}
