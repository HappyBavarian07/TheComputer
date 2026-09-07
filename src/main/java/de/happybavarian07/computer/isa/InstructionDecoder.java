package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.core.word.Word;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 16:09
 */
public class InstructionDecoder {
    public void decode(Word sourceWord, Instruction decodedInstruction) {
        // for opcode do shr 56 and 0xFF
        // for cond do shr 52 and 0xF
        // for rdidx do shr 46 and 0x3F
        // for rs1idx do shr 40 and 0x3F
        // for rs2idx do shr 34 and 0x3F
        // for reserved space do shr 32 and 0x3
        // for addr do 32-bit mask (0xFFFFFFFF)
        long wordValue = sourceWord.getAsLong();
        OpCode opCode = OpCode.fromBinaryValue((int) (wordValue >>> 56) & 0xFF);
        Condition cond = Condition.fromBinaryValue((int) (wordValue >>> 52) & 0xF);
        int regDestIndex = Math.toIntExact((wordValue >>> 46) & 0x3F);
        int regSource1Index = Math.toIntExact((wordValue >>> 40) & 0x3F);
        int regSource2Index = Math.toIntExact((wordValue >>> 34) & 0x3F);
        int immediateAddr = Math.toIntExact(wordValue & 0xFFFFFFFFL);
        decodedInstruction.set(opCode, cond, regDestIndex, regSource1Index, regSource2Index, immediateAddr);
    }
}
