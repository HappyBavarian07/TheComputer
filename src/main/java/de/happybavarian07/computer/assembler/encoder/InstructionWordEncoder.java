package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.isa.Condition;
import de.happybavarian07.computer.isa.OpCode;
import de.happybavarian07.computer.util.Architecture;

import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 20:40
 */
public class InstructionWordEncoder {
    public EncodedWord encode(ResolvedStatement resolvedStatement) {
        if (!(resolvedStatement.sourceStatement() instanceof InstructionStatement sourceStatement))
            throw new EncodingException(resolvedStatement.sourceStatement().span(), "tried to encode non instruction statement as an instruction");

        OpCode opCode = OpCode.valueOfNullable(sourceStatement.opcode());
        if (opCode == null) throw new EncodingException(sourceStatement.span(), "unknown opcode");

        int arity = opCode.arity();
        if (resolvedStatement.operands().size() != arity)
            throw new EncodingException(sourceStatement.span(), "wrong operand count");

        // Returns ALWAYS Condition when empty or null
        Condition cond = Condition.valueOfSafe(sourceStatement.condition());

        List<ResolvedOperand> operands = resolvedStatement.operands();
        OperandMapping operandMapping = opCode.operandMapping();
        long rd = 0, rs1 = 0, rs2 = 0, imm32 = 0;
        switch (operandMapping) {
            case NONE -> {
            }
            case RD_RS1_RS2 -> {
                rd = reg(operands.get(0));
                rs1 = reg(operands.get(1));
                rs2 = reg(operands.get(2));
            }
            case RD_RS1_IMM32, RD_RS1_OFFSET32 -> {
                rd = reg(operands.get(0));
                rs1 = reg(operands.get(1));
                imm32 = imm(operands.get(2));
            }
            case RD_RS1 -> {
                rd = reg(operands.get(0));
                rs1 = reg(operands.get(1));
            }
            case RD_IMM32 -> {
                rd = reg(operands.get(0));
                imm32 = imm(operands.get(1));
            }
            case IMM32_RD -> {
                imm32 = imm(operands.get(0));
                rd = reg(operands.get(1));
            }
            case IMM32_ONLY -> {
                imm32 = imm(operands.getFirst());
            }
            case RS1_ONLY -> {
                rs1 = reg(operands.getFirst());
            }
            case RD_ONLY -> {
                rd = reg(operands.getFirst());
            }
            default -> {
                throw new EncodingException(sourceStatement.span(), "unknown operand mapping '" + operandMapping + "'");
            }
        }

        // for opcode do 0xFF and shl 56
        // for cond do 0xF and shl 52
        // for rdidx do 0x3F and shl 46
        // for rs1idx do 0x3F and shl 40
        // for rs2idx do 0x3F and shl 34
        // for reserved space do 0x3 and shl 32
        // for addr do 32-bit mask (0xFFFFFFFF)
        long rawWord =
                ((opCode.binaryValue().longValue() & 0xFF) << 56) |
                        ((cond.binaryValue().longValue() & 0xFF) << 52) |
                        ((rd & 0x3F) << 46) |
                        ((rs1 & 0x3F) << 40) |
                        ((rs2 & 0x3F) << 34) |
                        (imm32 & 0xFFFFFFFFL);

        return new EncodedWord(resolvedStatement.address(), rawWord);
    }

    public int reg(ResolvedOperand operand) {
        int resolvedNumericValue = operand.resolvedNumericValue();
        if (resolvedNumericValue < 0 || resolvedNumericValue > Architecture.GPR_COUNT - 1)
            throw new EncodingException(operand.sourceOperand().span(), "register out of '0.." + (Architecture.GPR_COUNT - 1) + "'");

        return resolvedNumericValue;
    }

    public int imm(ResolvedOperand operand) {
        int resolvedNumericValue = operand.resolvedNumericValue();
        if (resolvedNumericValue < 0 || resolvedNumericValue > Architecture.MEMORY_SIZE_BYTES - 1)
            throw new EncodingException(operand.sourceOperand().span(), "'imm16' out of '0.." + (Architecture.MEMORY_SIZE_BYTES - 1) + "'");

        return resolvedNumericValue;
    }
}
