package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.exceptions.isa.IllegalInstructionException;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:48
 */
public enum OpCode {
    // System & Data Movement
    NOP(0x00, OperandMapping.NONE, 0),
    MOV(0x01, OperandMapping.RD_RS, 2),
    LOAD(0x02, OperandMapping.RD_IMM16, 2),
    LOADR(0x03, OperandMapping.RD_RS, 2),
    STORE(0x04, OperandMapping.IMM16_RD, 2),
    STORER(0x05, OperandMapping.RD_RS, 2),

    // Arithmetic & Logic
    ADD(0x10, OperandMapping.RD_RS, 2),
    SUB(0x11, OperandMapping.RD_RS, 2),
    AND(0x12, OperandMapping.RD_RS, 2),
    OR(0x13, OperandMapping.RD_RS, 2),
    XOR(0x14, OperandMapping.RD_RS, 2),
    NOT(0x15, OperandMapping.RD_ONLY, 1),
    SHL(0x16, OperandMapping.RD_IMM16, 2),
    SHR(0x17, OperandMapping.RD_IMM16, 2),

    // Control Flow & Branching
    JMP(0x20, OperandMapping.IMM16_ONLY, 1),
    JZ(0x21, OperandMapping.IMM16_ONLY, 1),
    JNZ(0x22, OperandMapping.IMM16_ONLY, 1),

    // Stack
    PUSH(0x30, OperandMapping.RS_ONLY, 1),
    POP(0x31, OperandMapping.RD_ONLY, 1),
    HALT(0x3F, OperandMapping.NONE, 0);


    private final int binaryValue;
    private final OperandMapping operandMapping;
    private final int arity;

    OpCode(int binValue, OperandMapping operandMapping, int arity) {
        this.binaryValue = binValue;
        this.operandMapping = operandMapping;
        this.arity = arity;
    }

    public int binaryValue() {
        return binaryValue;
    }

    public OperandMapping operandMapping() {
        return operandMapping;
    }

    public int arity() {
        return arity;
    }

    public static OpCode fromBinaryValue(int binaryValue) {
        for (int i = 0; i < values().length; i++) {
            if(values()[i].binaryValue == binaryValue) return values()[i];
        }
        throw new IllegalInstructionException("Invalid Instruction value received.");
    }

    public static OpCode valueOfNullable(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
