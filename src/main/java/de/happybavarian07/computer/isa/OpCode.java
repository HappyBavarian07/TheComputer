package de.happybavarian07.computer.isa;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:48
 */
public enum OpCode {
    // System & Data Movement
    NOP(0x00),
    MOV(0x01),
    LOAD(0x02),
    LOADR(0x03),
    STORE(0x04),
    STORER(0x05),

    // Arithmetic & Logic
    ADD(0x10),
    SUB(0x11),
    AND(0x12),
    OR(0x13),
    XOR(0x14),
    NOT(0x15),
    SHL(0x16),
    SHR(0x17),

    // Control Flow & Branching
    JMP(0x20),
    JZ(0x21),
    JNZ(0x22),

    // Stack
    PUSH(0x30),
    POP(0x31),
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
