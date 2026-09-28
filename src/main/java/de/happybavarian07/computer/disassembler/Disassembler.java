package de.happybavarian07.computer.disassembler;

import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.isa.Condition;
import de.happybavarian07.computer.isa.Instruction;
import de.happybavarian07.computer.isa.InstructionDecoder;
import de.happybavarian07.computer.util.Architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date September 18, 2026 | 07:08
 */
public class Disassembler {
    private InstructionDecoder instructionDecoder;

    public Disassembler() {
        this.instructionDecoder = new InstructionDecoder();
    }

    public String disassemble(Word word) {
        Instruction instruction = new Instruction();
        instructionDecoder.decodeNullable(word, instruction);

        if (instruction.opCode() != null) {
            String mnemonic = instruction.opCode().name().toLowerCase();
            if (instruction.condition() != null && instruction.condition() != Condition.AL) {
                mnemonic += instruction.condition().name().toLowerCase();
            }

            int rd = instruction.regDestIndex();
            int rs1 = instruction.regSource1Index();
            int rs2 = instruction.regSource2Index();
            String imm32 = instruction.opCode().immediateKind().format(instruction.immediateAddr());
            String operandString = switch (instruction.opCode().operandMapping()) {
                case NONE -> "";
                case RD_RS1_RS2 -> reg(rd) + ", " + reg(rs1) + ", " + reg(rs2);
                case RD_RS1_IMM32, RD_RS1_OFFSET32 -> reg(rd) + ", " + reg(rs1) + ", " + imm32;
                case RD_RS1 -> reg(rd) + ", " + reg(rs1);
                case RD_IMM32 -> reg(rd) + ", " + imm32;
                case IMM32_RD -> imm32 + ", " + reg(rd);
                case IMM32_ONLY -> imm32;
                case RS1_ONLY -> reg(rs1);
                case RD_ONLY -> reg(rd);
            };
            return (mnemonic + " " + operandString).trim();
        } else {
            return ".word " + word.getAsHexaDecString();
        }
    }

    private static String reg(int index) {
        return index == Architecture.SP_REGISTER_INDEX ? "sp" : "r" + index;
    }

    public String disassemble(long rawWord) {
        return disassemble(new Word(rawWord));
    }

    public List<DisassembledInstruction> disassembleRange(byte[] memory, int startAddress, int byteCount) {
        if (memory == null || byteCount <= 0 || startAddress < 0 || startAddress >= memory.length) {
            return Collections.emptyList();
        }

        List<DisassembledInstruction> instructions = new ArrayList<>();
        int limit = Math.min(memory.length, startAddress + byteCount);
        Word currentWord = new Word();
        Instruction instruction = new Instruction();

        for (int addr = startAddress; addr <= limit - 8; addr += 8) {
            long raw = 0L;
            for (int k = 0; k < 8; k++) {
                raw |= ((long) (memory[addr + k] & 0xFF)) << (k * 8);
            }
            currentWord.set(raw);
            instructionDecoder.decodeNullable(currentWord, instruction);

            String renderedText = disassemble(currentWord);

            instructions.add(new DisassembledInstruction(
                    addr,
                    raw,
                    instruction.opCode(),
                    instruction.condition(),
                    instruction.regDestIndex(),
                    instruction.regSource1Index(),
                    instruction.regSource2Index(),
                    instruction.immediateAddr() & 0xFFFFFFFFL,
                    renderedText
            ));
        }

        return instructions;
    }
}
