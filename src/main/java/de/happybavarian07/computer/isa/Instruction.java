package de.happybavarian07.computer.isa;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 15:56
 */
public class Instruction {
    private OpCode opCode;
    private Condition condition;
    private int regDestIndex;
    private int regSource1Index;
    private int regSource2Index;
    private int immediateAddr;

    public Instruction() {
        this(OpCode.NOP, Condition.AL, 0, 0, 0, 0);
    }

    public Instruction(OpCode opCode, Condition condition, int regDestIndex, int regSource1Index, int regSource2Index, int immediateAddr) {
        this.opCode = opCode;
        this.condition = condition;
        this.regDestIndex = regDestIndex;
        this.regSource1Index = regSource1Index;
        this.regSource2Index = regSource2Index;
        this.immediateAddr = immediateAddr;
    }

    public void set(OpCode opCode, Condition condition, int regDestIndex, int regSource1Index, int regSource2Index, int immediateAddr) {
        this.opCode = opCode;
        this.condition = condition;
        this.regDestIndex = regDestIndex;
        this.regSource1Index = regSource1Index;
        this.regSource2Index = regSource2Index;
        this.immediateAddr = immediateAddr;
    }

    public OpCode opCode() {
        return opCode;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

    public Condition condition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public int regDestIndex() {
        return regDestIndex;
    }

    public void setRegDestIndex(int regDestIndex) {
        this.regDestIndex = regDestIndex;
    }

    public int regSource1Index() {
        return regSource1Index;
    }

    public void setRegSource1Index(int regSource1Index) {
        this.regSource1Index = regSource1Index;
    }

    public int regSource2Index() {
        return regSource2Index;
    }

    public void setRegSource2Index(int regSource2Index) {
        this.regSource2Index = regSource2Index;
    }

    public int immediateAddr() {
        return immediateAddr;
    }

    public void setImmediateAddr(int immediateAddr) {
        this.immediateAddr = immediateAddr;
    }

    public void reset() {
        this.opCode = null;
        this.condition = null;
        this.regSource1Index = 0;
        this.regSource2Index = 0;
        this.regDestIndex = 0;
        this.immediateAddr = 0;
    }
}
