package de.happybavarian07.computer.cpu.alu;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 19:15
 */
public enum AluOp {
    ADD(0), // A + B
    SUB(1), // A - B
    AND(2), // A & B
    OR(3), // A \/ B
    XOR(4), // A ^ B
    NOT(5), // -A
    SHL(6), // A << B
    SHR(7); // A >> B
    // ADD, SUB, AND, OR, XOR, NOT, SHL, SHR

    private final int opCode;

    AluOp(int opCode) {
        this.opCode = opCode;
    }

    public int opCode() {
        return opCode;
    }

    public String opCodeBinary() {
        return Long.toBinaryString(opCode);
    }
}
