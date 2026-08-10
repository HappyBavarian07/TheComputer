package de.happybavarian07.computer.isa;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:48
 */
public enum OpCode {
    // Data Move
    MOV(0x01),
    LOAD(0x02),
    STORE(0x03),

    // Arithmetic
    ADD(0x04),
    SUB(0x05),

    // Logic
    AND(0x06),
    OR(0x07),
    XOR(0x08),
    NOT(0x09),

    // Branching
    JMP(0x0A),
    JZ(0x0B),
    JNZ(0x0C),

    // Stack
    PUSH(0x0D),
    POP(0x0E),

    // System
    NOP(0x00),
    HALT(0x3F);


    private final int binaryValue;

    OpCode(int binValue) {
        this.binaryValue = binValue;
    }

    public int binaryValue() {
        return binaryValue;
    }

    public static OpCode fromBinaryValue(int binaryValue) {
        for (int i = 0; i < values().length; i++) {
            if(values()[i].binaryValue == binaryValue) return values()[i];
        }
        throw new IllegalInstructionException("Invalid Instruction value received.");
    }
}
