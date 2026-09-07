package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.exceptions.isa.IllegalInstructionException;

import java.util.Objects;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:48
 */
public enum OpCode {
    // Cat0: System & Control (5/16)
    NOP(0x00, OperandMapping.NONE),
    MOV(0x01, OperandMapping.RD_RS1), // move r1 to rd
    MOVI(0x02, OperandMapping.RD_IMM32), // move imm32 value to rd
    HALT(0x03, OperandMapping.NONE), // halt execution

    // Cat1: Arithmetic
    ADD(0x10, OperandMapping.RD_RS1_RS2), // rs1 + rs2 = rd
    ADDI(0x11, OperandMapping.RD_RS1_IMM32), // rs1 + imm32 = rd
    SUB(0x12, OperandMapping.RD_RS1_RS2), // rs1 - rs2 = rd
    SUBI(0x13, OperandMapping.RD_RS1_IMM32), // rs1 - imm32 = rd
    MUL(0x14, OperandMapping.RD_RS1_RS2), // rs1 * rs2 = rd
    DIV(0x15, OperandMapping.RD_RS1_RS2), // rs1 / rs2 = rd
    MOD(0x16, OperandMapping.RD_RS1_RS2), // rs1 % rs2 = rd
    CMP(0x17, OperandMapping.RD_RS1), // rd - rs1, update flags
    CMPI(0x18, OperandMapping.RD_IMM32), // rd - imm32val, update flags

    // Cat2: Bitwise & Shifts
    AND(0x20, OperandMapping.RD_RS1_RS2), // rs1 & rs2 = rd
    ANDI(0x21, OperandMapping.RD_RS1_IMM32), // rs1 & imm32 = rd
    OR(0x22, OperandMapping.RD_RS1_RS2), // rs1 | rs2 = rd
    ORI(0x23, OperandMapping.RD_RS1_IMM32), // rs1 | imm32 = rd
    XOR(0x24, OperandMapping.RD_RS1_RS2), // rs1 ⊕ rs2 = rd
    XORI(0x25, OperandMapping.RD_RS1_IMM32), // rs1 ⊕ imm32 = rd
    NOT(0x26, OperandMapping.RD_RS1), // rs1 =~ rd
    SHL(0x27, OperandMapping.RD_RS1_RS2), // rs1 << rs2 = rd
    SHLI(0x28, OperandMapping.RD_RS1_IMM32), // rs1 << imm32 = rd
    SHR(0x29, OperandMapping.RD_RS1_RS2), // rs1 >>> rs2 = rd
    SHRI(0x2A, OperandMapping.RD_RS1_IMM32), // rs1 >>> imm32 = rd

    // Cat3: Branches & Subroutines
    JMP(0x30, OperandMapping.IMM32_ONLY), // jump to imm32
    CALL(0x31, OperandMapping.IMM32_ONLY), // save current pc, jump to imm32
    RET(0x32, OperandMapping.NONE), // return to pc
    JMPR(0x33, OperandMapping.RS1_ONLY), // jump to rs1
    CALLR(0x34, OperandMapping.RS1_ONLY), // save current pc, jump to imm32

    // Cat4: Stack Operations
    PUSH(0x40, OperandMapping.RS1_ONLY),
    POP(0x41, OperandMapping.RD_ONLY),

    // Cat5: Memory Operations
    LOADB(0x50, OperandMapping.RD_IMM32), // read 1 byte, save to rd
    LOADH(0x51, OperandMapping.RD_IMM32), // read 2 byte, save to rd
    LOADI(0x52, OperandMapping.RD_IMM32), // read 4 byte, save to rd
    LOADW(0x53, OperandMapping.RD_IMM32), // read all (8) byte, save to rd
    STOREB(0x54, OperandMapping.IMM32_RD), // read rd, save to 1 bytes
    STOREH(0x55, OperandMapping.IMM32_RD), // read rd, save to 2 bytes
    STOREI(0x56, OperandMapping.IMM32_RD), // read rd, save to 4 bytes
    STOREW(0x57, OperandMapping.IMM32_RD), // read rd, save to all (8) bytes
    // Register-Indirect
    LOADR(0x58, OperandMapping.RD_RS1_OFFSET32),
    STORER(0x59, OperandMapping.RD_RS1_OFFSET32);


    private final Number binaryValue;
    private final OperandMapping operandMapping;

    OpCode(int binValue, OperandMapping operandMapping) {
        this.binaryValue = binValue;
        this.operandMapping = operandMapping;
    }

    public Number binaryValue() {
        return binaryValue;
    }

    public OperandMapping operandMapping() {
        return operandMapping;
    }

    public int arity() {
        return operandMapping.arity();
    }

    public static OpCode fromBinaryValue(Number binaryValue) {
        int target = binaryValue.intValue();
        for (OpCode op : values()) {
            if (op.binaryValue.intValue() == target) return op;
        }
        throw new IllegalInstructionException("Invalid Instruction value: " + target);
    }

    public static OpCode valueOfNullable(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
