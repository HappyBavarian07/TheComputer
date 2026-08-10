package de.happybavarian07.computer.isa;

import de.happybavarian07.computer.core.word.Word;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 16:09
 */
public class InstructionDecoder {
    public void decode(Word sourceWord, Instruction decodedInstruction) {
        // for opcode do shr 26 and 6-bit mask
        // for rdidx do shr 21 and 5-bit mask
        // for rsidx do shr 16 and 5-bit mask
        // for addr do 16-bit mask
        int wordValue = sourceWord.getAsInt();
        OpCode opCode = OpCode.fromBinaryValue((wordValue >>> 26) & 0x3F);
        int regDestIndex = (wordValue >>> 21) & 0x1F;
        int regSourceIndex = (wordValue >>> 16) & 0x1F;
        int immediateAddr = wordValue & 0xFF;
        decodedInstruction.set(opCode, regDestIndex, regSourceIndex, immediateAddr);
    }
}
