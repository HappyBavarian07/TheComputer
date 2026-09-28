package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.encoder.model.OperandMapping;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.isa.Condition;
import de.happybavarian07.computer.isa.ImmediateKind;
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
                rd = reg(operands.get(0), opCode);
                rs1 = reg(operands.get(1), opCode);
                rs2 = reg(operands.get(2), opCode);
            }
            case RD_RS1_IMM32, RD_RS1_OFFSET32 -> {
                rd = reg(operands.get(0), opCode);
                rs1 = reg(operands.get(1), opCode);
                imm32 = imm(operands.get(2), opCode);
            }
            case RD_RS1 -> {
                rd = reg(operands.get(0), opCode);
                rs1 = reg(operands.get(1), opCode);
            }
            case RD_IMM32 -> {
                rd = reg(operands.get(0), opCode);
                imm32 = imm(operands.get(1), opCode);
            }
            case IMM32_RD -> {
                imm32 = imm(operands.get(0), opCode);
                rd = reg(operands.get(1), opCode);
            }
            case IMM32_ONLY -> {
                imm32 = imm(operands.getFirst(), opCode);
            }
            case RS1_ONLY -> {
                rs1 = reg(operands.getFirst(), opCode);
            }
            case RD_ONLY -> {
                rd = reg(operands.getFirst(), opCode);
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

    public int reg(ResolvedOperand operand, OpCode opCode) {
        Long value = operand.resolvedNumericValue();
        String text = operand.text();
        if (value == null)
            throw new EncodingException(operand.sourceOperand().span(), "'" + text + "' cannot be used as an operand; use r0..r" + (Architecture.GPR_COUNT - 1) + (opCode.allowsSpOperand() ? " or sp" : ""));

        if (value == Architecture.SP_REGISTER_INDEX) {
            if (!opCode.allowsSpOperand())
                throw new EncodingException(operand.sourceOperand().span(), "'sp' is only allowed in mov, addi and subi, not in " + opCode.name().toLowerCase());
            return Architecture.SP_REGISTER_INDEX;
        }

        if (value < 0 || value > Architecture.GPR_COUNT - 1)
            throw new EncodingException(operand.sourceOperand().span(), "register out of '0.." + (Architecture.GPR_COUNT - 1) + "'");

        return value.intValue();
    }

    // the range depends on the opcode: see ImmediateKind
    public long imm(ResolvedOperand operand, OpCode opCode) {
        Long resolvedNumericValue = operand.resolvedNumericValue();
        ImmediateKind kind = opCode.immediateKind();
        if (resolvedNumericValue == null || !kind.fits(resolvedNumericValue))
            throw new EncodingException(operand.sourceOperand().span(), "immediate '" + operand.text() + "' out of range for " + opCode.name().toLowerCase() + ": expected " + kind.describeRange());

        return resolvedNumericValue;
    }
}
