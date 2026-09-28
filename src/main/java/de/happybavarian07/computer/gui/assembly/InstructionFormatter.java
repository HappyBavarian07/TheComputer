package de.happybavarian07.computer.gui.assembly;

import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.isa.OpCode;

import java.util.Map;

/** Human-readable syntax and descriptions for the instruction reference. */
public final class InstructionFormatter {
    private InstructionFormatter() {
    }

    private static final Map<OperandMapping, String> OPERAND_SYNTAX = Map.of(
            OperandMapping.NONE, "",
            OperandMapping.RD_RS1_RS2, "rd, rs1, rs2",
            OperandMapping.RD_RS1_IMM32, "rd, rs1, imm32",
            OperandMapping.RD_RS1, "rd, rs1",
            OperandMapping.RD_IMM32, "rd, imm32",
            OperandMapping.IMM32_RD, "imm32, rd",
            OperandMapping.RD_RS1_OFFSET32, "rd, rs1, offset32",
            OperandMapping.IMM32_ONLY, "imm32",
            OperandMapping.RS1_ONLY, "rs1",
            OperandMapping.RD_ONLY, "rd"
    );

    public static String operandSyntax(OperandMapping mapping) {
        return OPERAND_SYNTAX.getOrDefault(mapping, "");
    }

    public static String buildSyntax(OpCode opCode) {
        String operands = operandSyntax(opCode.operandMapping());
        return operands.isEmpty() ? opCode.name().toLowerCase() : opCode.name().toLowerCase() + " " + operands;
    }

    public static String describeOpcode(OpCode opCode) {
        return switch (opCode) {
            case NOP -> "No operation.";
            case MOV -> "Copy value from source register to destination register.";
            case MOVI -> "Load 32-bit immediate constant directly into register.";
            case HALT -> "Halt CPU execution.";
            case ADD -> "Add source registers into destination register (rd = rs1 + rs2).";
            case ADDI -> "Add immediate to source register into destination register (rd = rs1 + imm).";
            case SUB -> "Subtract source registers (rd = rs1 - rs2).";
            case SUBI -> "Subtract immediate from source register (rd = rs1 - imm).";
            case MUL -> "Multiply source registers into destination register (rd = rs1 * rs2).";
            case DIV -> "Divide source registers (quotient into destination register).";
            case MOD -> "Modulo division remainder into destination register.";
            case CMP -> "Compare registers and update flags (rd - rs1).";
            case CMPI -> "Compare register with immediate and update flags (rd - imm).";
            case AND -> "Bitwise AND between source registers.";
            case ANDI -> "Bitwise AND with immediate bitmask.";
            case OR -> "Bitwise OR between source registers.";
            case ORI -> "Bitwise OR with immediate bitmask.";
            case XOR -> "Bitwise XOR between source registers.";
            case XORI -> "Bitwise XOR with immediate bitmask.";
            case NOT -> "Bitwise invert source register into destination register.";
            case SHL -> "Logical shift left by register amount.";
            case SHLI -> "Logical shift left by immediate bit count.";
            case SHR -> "Logical unsigned right shift by register amount.";
            case SHRI -> "Logical unsigned right shift by immediate bit count.";
            case JMP -> "Jump to address.";
            case CALL -> "Call subroutine at immediate address.";
            case RET -> "Return from subroutine.";
            case JMPR -> "Jump to address in register.";
            case CALLR -> "Call subroutine at address in register.";
            case PUSH -> "Push register value onto stack.";
            case POP -> "Pop value from stack into register.";
            case LOADB -> "Load 8-bit byte from memory.";
            case LOADH -> "Load 16-bit halfword from memory.";
            case LOADI -> "Load 32-bit int from memory.";
            case LOADW -> "Load 64-bit word from memory.";
            case STOREB -> "Store 8-bit byte into memory.";
            case STOREH -> "Store 16-bit halfword into memory.";
            case STOREI -> "Store 32-bit int into memory.";
            case STOREW -> "Store 64-bit word into memory.";
            case LOADR -> "Load from memory at register base plus offset.";
            case STORER -> "Store into memory at register base plus offset.";
        };
    }
}
