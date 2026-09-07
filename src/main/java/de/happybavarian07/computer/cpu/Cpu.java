package de.happybavarian07.computer.cpu;

import de.happybavarian07.computer.bus.SystemBus;
import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.alu.Alu;
import de.happybavarian07.computer.cpu.alu.AluOp;
import de.happybavarian07.computer.cpu.registers.RegisterFile;
import de.happybavarian07.computer.cpu.registers.SpecialRegisters;
import de.happybavarian07.computer.exceptions.stack.StackOverflowException;
import de.happybavarian07.computer.isa.Instruction;
import de.happybavarian07.computer.isa.InstructionDecoder;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.memory.ram.RamBusDevice;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 16:48
 */
public class Cpu {
    private final RegisterFile registerFile;
    private final SpecialRegisters specialRegisters;
    private final Alu alu;
    private final SystemBus systemBus;
    private final InstructionDecoder instructionDecoder;

    private final Instruction currentInstruction;
    private final Word regSrc1Value;
    private final Word regSrc2Value;
    private final Word regDestValue;
    private final Address workingAddress;
    private final Word workingResult;
    private final Word scratchReturnAddrWord;

    private boolean isHalted;

    public Cpu(SystemBus systemBus) {
        registerFile = new RegisterFile();
        specialRegisters = new SpecialRegisters();
        alu = new Alu();
        this.systemBus = systemBus;
        instructionDecoder = new InstructionDecoder();

        currentInstruction = new Instruction();
        regSrc1Value = new Word();
        regSrc2Value = new Word();
        regDestValue = new Word();
        workingAddress = new Address();
        workingResult = new Word();
        scratchReturnAddrWord = new Word();

    }

    public Cpu() {
        this(new SystemBus());
        systemBus.registerDevice(new Address(0x0000), new Address(Architecture.MEMORY_FREE_END), new RamBusDevice());
    }

    public void step() {
        // read instruction from pc address via systembus
        // decode
        // check condition
        // execute and write back aka whole opcode logic
        // increment pc by 8 bytes if not branching instruction
        specialRegisters.readPC(workingAddress);
        systemBus.readWord(workingAddress, specialRegisters.getIR());
        instructionDecoder.decode(specialRegisters.getIR(), currentInstruction);
        boolean pcUpdate = true;
        if (currentInstruction.condition().test(specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit())) {
            boolean writesRd = false;
            // split this up
            // reading data (first switch)
            // execute (second switch)
            // write (final ifs)
            switch (currentInstruction.opCode().operandMapping()) {
                case NONE -> {
                } // nothing
                case RD_RS1_RS2 -> {
                    registerFile.read(currentInstruction.regSource1Index(), regSrc1Value);
                    registerFile.read(currentInstruction.regSource2Index(), regSrc2Value);
                } // read r1, r2
                case RD_RS1_IMM32 -> {
                    registerFile.read(currentInstruction.regSource1Index(), regSrc1Value);
                    regSrc2Value.set(currentInstruction.immediateAddr());
                } // read r1, set src2 to imm32
                case RD_RS1, RS1_ONLY -> {
                    registerFile.read(currentInstruction.regSource1Index(), regSrc1Value);
                } // read r1
                case RD_IMM32 -> {
                    workingAddress.set(currentInstruction.immediateAddr());
                    regSrc2Value.set(currentInstruction.immediateAddr());
                    registerFile.read(currentInstruction.regDestIndex(), regDestValue);
                } // set workingAddress & src2 to imm32, read rd
                case IMM32_RD -> {
                    workingAddress.set(currentInstruction.immediateAddr());
                    registerFile.read(currentInstruction.regDestIndex(), regSrc1Value);
                } // set workingAddress to imm32, read rd into src1
                case RD_RS1_OFFSET32 -> {
                    registerFile.read(currentInstruction.regSource1Index(), regSrc1Value);
                    registerFile.read(currentInstruction.regDestIndex(), regDestValue);

                    int effectiveAddress = regSrc1Value.getAsInt() + currentInstruction.immediateAddr();
                    workingAddress.set(effectiveAddress);
                } // read r1, read rd, set workingAddress to imm32
                case IMM32_ONLY -> {
                    workingAddress.set(currentInstruction.immediateAddr());
                } // set workingAddress to imm32
                case RD_ONLY -> {
                } // nothing to read
            }


            switch (currentInstruction.opCode()) {
                // Cat0
                case MOV -> {
                    workingResult.set(regSrc1Value);
                    writesRd = true;
                }
                case MOVI -> {
                    workingResult.set(workingAddress.getAsInt());
                    writesRd = true;
                }
                case HALT -> {
                    isHalted = true;
                    pcUpdate = false;
                }
                // Cat1
                case ADD, ADDI, SUB, SUBI, MUL, DIV, MOD -> {
                    AluOp aluOp = AluOp.NOP;
                    switch (currentInstruction.opCode()) {
                        case ADD, ADDI -> aluOp = AluOp.ADD;
                        case SUB, SUBI -> aluOp = AluOp.SUB;
                        case MUL -> aluOp = AluOp.MUL;
                        case DIV -> aluOp = AluOp.DIV;
                        case MOD -> aluOp = AluOp.MOD;
                    }
                    alu.execute(regSrc1Value, regSrc2Value, aluOp, workingResult, specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit());
                    writesRd = true;
                }
                case CMP -> {
                    alu.execute(regDestValue, regSrc1Value, AluOp.SUB, workingResult, specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit());
                }
                case CMPI -> {
                    workingResult.set(workingAddress.getAsInt());
                    alu.execute(regDestValue, regSrc2Value, AluOp.SUB, workingResult, specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit());
                }
                // Cat2
                case AND, OR, XOR, NOT, SHL, SHR, /**/ ANDI, ORI, XORI, SHLI, SHRI -> {
                    AluOp aluOp = AluOp.NOP;
                    switch (currentInstruction.opCode()) {
                        case AND, ANDI -> aluOp = AluOp.AND;
                        case OR, ORI -> aluOp = AluOp.OR;
                        case XOR, XORI -> aluOp = AluOp.XOR;
                        case SHL, SHLI -> aluOp = AluOp.SHL;
                        case SHR, SHRI -> aluOp = AluOp.SHR;
                        case NOT -> aluOp = AluOp.NOT;
                    }
                    alu.execute(regSrc1Value, regSrc2Value, aluOp, workingResult, specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit());
                    writesRd = true;
                }
                // Cat3
                case JMP -> {
                    specialRegisters.getPC().set(workingAddress);
                    pcUpdate = false;
                }
                case CALL -> {
                    scratchReturnAddrWord.set(specialRegisters.getPC().getAsInt() + Architecture.INSTRUCTION_BYTES);
                    push(scratchReturnAddrWord);
                    specialRegisters.getPC().set(workingAddress);
                    pcUpdate = false;

                }
                case RET -> {
                    pop(scratchReturnAddrWord);
                    specialRegisters.getPC().set(scratchReturnAddrWord);
                    pcUpdate = false;
                }
                case JMPR -> {
                    specialRegisters.getPC().set(regSrc1Value.getAsLong() & 0xFFFFFFFFL);
                    pcUpdate = false;
                }
                case CALLR -> {
                    scratchReturnAddrWord.set(specialRegisters.getPC().getAsInt() + Architecture.INSTRUCTION_BYTES);
                    push(scratchReturnAddrWord);
                    specialRegisters.getPC().set(regSrc1Value.getAsLong() & 0xFFFFFFFFL);
                    pcUpdate = false;
                }
                // Cat4
                case PUSH -> {
                    push(regSrc1Value);
                }
                case POP -> {
                    pop(workingResult);
                    writesRd = true;
                }
                // Cat5
                case LOADB, LOADH, LOADI, LOADW -> {
                    systemBus.readWord(workingAddress, workingResult);
                    switch (currentInstruction.opCode()) {
                        case LOADB -> workingResult.set(workingResult.getAsLong() & 0xFFL);
                        case LOADH -> workingResult.set(workingResult.getAsLong() & 0xFFFFL);
                        case LOADI -> workingResult.set(workingResult.getAsLong() & 0xFFFFFFFFL);
                    }
                    writesRd = true;
                }
                case LOADR -> {
                    systemBus.readWord(workingAddress, workingResult);
                    writesRd = true;
                }
                case STOREB -> {
                    systemBus.write(workingAddress, regSrc1Value, 1);
                }
                case STOREH -> {
                    systemBus.write(workingAddress, regSrc1Value, 2);
                }
                case STOREI -> {
                    systemBus.write(workingAddress, regSrc1Value, 4);
                }
                case STOREW -> {
                    systemBus.write(workingAddress, regSrc1Value, Architecture.INSTRUCTION_BYTES);
                }
                case STORER -> {
                    systemBus.write(workingAddress, regDestValue, 8);
                }
                default -> {
                }
            }

            if (writesRd) {
                registerFile.write(currentInstruction.regDestIndex(), workingResult);
            }
        }

        if (pcUpdate) {
            specialRegisters.getPC().add(Architecture.INSTRUCTION_BYTES);
        }
    }

    public void run() {
        while (!isHalted) {
            step();
        }
    }

    public void reset() {
        registerFile.reset();
        specialRegisters.reset();

        currentInstruction.reset();
        regSrc1Value.set(0);
        regSrc2Value.set(0);
        regDestValue.set(0);
        workingAddress.set(0);
        workingResult.set(0);

        isHalted = false;
    }

    private void push(Word value) {
        int newSp = specialRegisters.getSP().getAsInt() - Architecture.INSTRUCTION_BYTES;
        if (newSp < Architecture.STACK_LIMIT_ADDRESS) {
            isHalted = true;
            throw new StackOverflowException("Tried to push data past max stack size.");
        }
        specialRegisters.getSP().set(newSp);
        workingAddress.set(specialRegisters.getSP());
        systemBus.writeWord(workingAddress, value);
    }

    private void pop(Word destination) {
        int currentSp = specialRegisters.getSP().getAsInt();
        if (currentSp > Architecture.STACK_BASE_ADDRESS) {
            isHalted = true;
            throw new StackOverflowException("Tried to pop from empty stack.");
        }
        workingAddress.set(specialRegisters.getSP());
        systemBus.readWord(workingAddress, destination);
        specialRegisters.getSP().add(Architecture.INSTRUCTION_BYTES);
    }

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    public SpecialRegisters getSpecialRegisters() {
        return specialRegisters;
    }

    public SystemBus getSystemBus() {
        return systemBus;
    }

    public Alu getAlu() {
        return alu;
    }

    public InstructionDecoder getInstructionDecoder() {
        return instructionDecoder;
    }

    public boolean isHalted() {
        return isHalted;
    }
}
