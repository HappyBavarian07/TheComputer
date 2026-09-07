package de.happybavarian07.computer.cpu.alu;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 19:15
 */
public enum AluOp {
    ADD(0x0), // A + B
    SUB(0x1), // A - B
    MUL(0x2), // A * B
    DIV(0x3), // A / B
    MOD(0x3), // A % B
    AND(0x4), // A & B
    OR(0x5), // A \/ B
    XOR(0x6), // A ^ B
    NOT(0x7), // -A
    SHL(0x8), // A << B
    SHR(0x9), // A >> B
    NOP(0xA); // No-Op
    // ADD, SUB, MUL, DIV, MOD, AND, OR, XOR, NOT, SHL, SHR

    private final Number opCode;

    AluOp(Number opCode) {
        this.opCode = opCode;
    }

    public Number opCode() {
        return opCode;
    }

    public String opCodeBinary() {
        return Long.toBinaryString(opCode.longValue());
    }
}
