package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
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

        List<ResolvedOperand> operands = resolvedStatement.operands();
        OperandMapping operandMapping = opCode.operandMapping();
        int rd = 0, rs = 0, imm16 = 0;
        switch (operandMapping) {
            case NONE -> {
            }
            case RD_RS -> {
                rd = reg(operands.get(0));
                rs = reg(operands.get(1));
            }
            case RD_ONLY -> {
                rd = reg(operands.getFirst());
            }
            case RS_ONLY -> {
                rs = reg(operands.getFirst());
            }
            case RD_IMM16 -> {
                rd = reg(operands.get(0));
                imm16 = imm(operands.get(1));
            }
            case IMM16_RD -> {
                imm16 = imm(operands.get(0));
                rd = reg(operands.get(1));
            }
            case IMM16_ONLY -> {
                imm16 = imm(operands.getFirst());
            }
            default -> {
                throw new EncodingException(sourceStatement.span(), "unknown operand mapping '" + operandMapping + "'");
            }
        }

        int rawWord =
                ((opCode.binaryValue() & 0x3F) << 26) |
                        ((rd & 0x1F) << 21) |
                        ((rs & 0x1F) << 16) |
                        (imm16 & 0xFFFF);

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
