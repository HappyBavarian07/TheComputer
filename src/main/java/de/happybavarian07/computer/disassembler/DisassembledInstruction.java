package de.happybavarian07.computer.disassembler;

import de.happybavarian07.computer.isa.Condition;
import de.happybavarian07.computer.isa.OpCode;

/*
 * @Author HappyBavarian07
 * @Date September 18, 2026 | 07:08
 */
public record DisassembledInstruction(int address, long rawWord, OpCode opCode, Condition condition, int regDest, int regSrc1, int regSrc2, long imm32, String renderedText) {
}
