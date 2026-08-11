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
    private final Word regSrcValue;
    private final Word regDestValue;
    private final Address workingAddress;
    private final Word workingResult;

    private boolean isHalted;

    public Cpu(SystemBus systemBus) {
        registerFile = new RegisterFile();
        specialRegisters = new SpecialRegisters();
        alu = new Alu();
        this.systemBus = systemBus;
        instructionDecoder = new InstructionDecoder();

        currentInstruction = new Instruction();
        regSrcValue = new Word();
        regDestValue = new Word();
        workingAddress = new Address();
        workingResult = new Word();

    }

    public Cpu() {
        this(new SystemBus());
        systemBus.registerDevice(new Address(0x0000), new Address(Architecture.MEMORY_FREE_END), new RamBusDevice());
    }

    public void step() {
        // read instruction from pc address via systembus
        // decode
        // execute and write back aka whole opcode logic
        // increment pc by 4 bytes if not branching instruction
        specialRegisters.readPC(workingAddress);
        systemBus.read(workingAddress, specialRegisters.getIR());
        instructionDecoder.decode(specialRegisters.getIR(), currentInstruction);
        boolean pcUpdate = true;
        switch (currentInstruction.opCode()) {
            case MOV -> {
                registerFile.read(currentInstruction.regSourceIndex(), workingResult);
                registerFile.write(currentInstruction.regDestIndex(), workingResult);
            }
            case LOAD -> {
                workingAddress.set(currentInstruction.immediateAddr());
                systemBus.read(workingAddress, workingResult);
                registerFile.write(currentInstruction.regDestIndex(), workingResult);
            }
            case LOADR -> {
                registerFile.read(currentInstruction.regSourceIndex(), regSrcValue);
                workingAddress.set(regSrcValue.getAsInt() & 0xFFFF);
                systemBus.read(workingAddress, workingResult);
            }
            case STORE -> {
                workingAddress.set(currentInstruction.immediateAddr());
                registerFile.read(currentInstruction.regSourceIndex(), regSrcValue);
                systemBus.write(workingAddress, regSrcValue);
            }
            case STORER -> {
                registerFile.read(currentInstruction.regDestIndex(), regDestValue);
                workingAddress.set(regDestValue.getAsInt() & 0xFFFF);
                registerFile.read(currentInstruction.regSourceIndex(), regSrcValue);
                systemBus.write(workingAddress, regSrcValue);
            }
            case ADD, SUB, AND, OR, XOR, NOT, SHL, SHR -> {
                registerFile.read(currentInstruction.regSourceIndex(), regSrcValue);
                if (!currentInstruction.opCode().equals(OpCode.SHL) && !currentInstruction.opCode().equals(OpCode.SHR))
                    registerFile.read(currentInstruction.regDestIndex(), regDestValue);
                AluOp aluOp = AluOp.NOP;
                switch (currentInstruction.opCode()) {
                    case ADD -> aluOp = AluOp.ADD;
                    case SUB -> aluOp = AluOp.SUB;
                    case AND -> aluOp = AluOp.AND;
                    case OR -> aluOp = AluOp.OR;
                    case XOR -> aluOp = AluOp.XOR;
                    case NOT -> aluOp = AluOp.NOT;
                    case SHL -> aluOp = AluOp.SHL;
                    case SHR -> aluOp = AluOp.SHR;
                }
                alu.execute(
                        regSrcValue,
                        (!currentInstruction.opCode().equals(OpCode.SHL) && !currentInstruction.opCode().equals(OpCode.SHR)) ? regDestValue : null,
                        aluOp,
                        workingResult,
                        specialRegisters.getFlagZBit(), specialRegisters.getFlagNBit(), specialRegisters.getFlagCBit(), specialRegisters.getFlagVBit()
                );
                registerFile.write(currentInstruction.regDestIndex(), workingResult);
            }
            case JMP -> {
                specialRegisters.getPC().set(currentInstruction.immediateAddr());
                pcUpdate = false;
            }
            case JZ -> {
                if (specialRegisters.isZero()) {
                    specialRegisters.getPC().set(currentInstruction.immediateAddr());
                    pcUpdate = false;
                }
            }
            case JNZ -> {
                if (!specialRegisters.isZero()) {
                    specialRegisters.getPC().set(currentInstruction.immediateAddr());
                    pcUpdate = false;
                }
            }
            case PUSH -> {
                int spVal = specialRegisters.getSP().getAsInt();
                int newSp = spVal - 4;
                if (newSp < Architecture.STACK_LIMIT_ADDRESS) {
                    isHalted = true;
                    throw new StackOverflowException("Tried to push data past max stack size.");
                }
                registerFile.read(currentInstruction.regSourceIndex(), workingResult);
                specialRegisters.getSP().set(newSp);
                workingAddress.set(specialRegisters.getSP());
                systemBus.write(workingAddress, workingResult);
            }
            case POP -> {
                int spVal = specialRegisters.getSP().getAsInt();
                if (spVal > Architecture.MEMORY_FREE_END - 4) {
                    isHalted = true;
                    throw new StackOverflowException("Tried to pop from empty stack.");
                }
                workingAddress.set(specialRegisters.getSP());
                systemBus.read(workingAddress, workingResult);
                registerFile.write(currentInstruction.regDestIndex(), workingResult);
                specialRegisters.getSP().add(4);
            }
            case HALT -> {
                isHalted = true;
                pcUpdate = false;
            }
            default -> {
            }
        }

        if(pcUpdate) {
            specialRegisters.getPC().add(4);
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
        regSrcValue.set(0);
        regDestValue.set(0);
        workingAddress.set(0);
        workingResult.set(0);

        isHalted = false;
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
