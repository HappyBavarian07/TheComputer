package de.happybavarian07.computer.isa;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:56
 */
public class Instruction {
    private OpCode opCode;
    private int regDestIndex;
    private int regSourceIndex;
    private int immediateAddr;

    public Instruction() {
        this(OpCode.NOP, 0, 0, 0);
    }

    public Instruction(OpCode opCode, int regDestIndex, int regSourceIndex, int immediateAddr) {
        this.opCode = opCode;
        this.regDestIndex = regDestIndex;
        this.regSourceIndex = regSourceIndex;
        this.immediateAddr = immediateAddr;
    }

    public void set(OpCode opCode, int regDestIndex, int regSourceIndex, int immediateAddr) {
        this.opCode = opCode;
        this.regDestIndex = regDestIndex;
        this.regSourceIndex = regSourceIndex;
        this.immediateAddr = immediateAddr;
    }

    public OpCode opCode() {
        return opCode;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

    public int regDestIndex() {
        return regDestIndex;
    }

    public void setRegDestIndex(int regDestIndex) {
        this.regDestIndex = regDestIndex;
    }

    public int regSourceIndex() {
        return regSourceIndex;
    }

    public void setRegSourceIndex(int regSourceIndex) {
        this.regSourceIndex = regSourceIndex;
    }

    public int immediateAddr() {
        return immediateAddr;
    }

    public void setImmediateAddr(int immediateAddr) {
        this.immediateAddr = immediateAddr;
    }

    public void reset() {
        this.opCode = null;
        this.regSourceIndex = 0;
        this.regDestIndex = 0;
        this.immediateAddr = 0;
    }
}
