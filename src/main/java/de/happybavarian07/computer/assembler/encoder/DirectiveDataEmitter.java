package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.util.Architecture;

import java.util.List;
import java.util.Locale;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 21:09
 */
public class DirectiveDataEmitter {
    public void emit(ResolvedStatement resolvedStatement, ByteSink byteSink) {
        if (!(resolvedStatement.sourceStatement() instanceof DirectiveStatement sourceStatement))
            throw new EncodingException(resolvedStatement.sourceStatement().span(), "tried to encode non directive statement as a directive");

        String name = sourceStatement.name();
        List<ResolvedOperand> operands = resolvedStatement.operands();

        int baseAddr = resolvedStatement.address();

        if (baseAddr < 0 || baseAddr > Architecture.MEMORY_SIZE_BYTES - 1)
            throw new EncodingException(sourceStatement.span(), "base address outside '0.." + (Architecture.MEMORY_SIZE_BYTES - 1) + "'");

        if (baseAddr % 4 != 0)
            throw new EncodingException(sourceStatement.span(), "base address not aligned to 4");

        switch (name.toLowerCase(Locale.ROOT)) {
            case ".word" -> {
                for (ResolvedOperand operand : operands) {
                    if (operand.resolvedNumericValue() == null)
                        throw new EncodingException(operand.sourceOperand().span(), "operand must have non-null value");

                    int value = operand.resolvedNumericValue();

                    for (int i = 0; i < 4; i++) {
                        int currentAddr = baseAddr + i;
                        int byteOffset = i * 8;
                        checkAndAddValueToSink(currentAddr, (value >>> byteOffset) & 0xFF, byteSink, operand.sourceOperand().span());
                    }

                    baseAddr += 4;
                }
            }
            case ".byte", ".ascii" -> {
                for (ResolvedOperand operand : operands) {
                    if (operand.resolvedNumericValue() == null)
                        throw new EncodingException(operand.sourceOperand().span(), "operand must have non-null value");

                    int value = operand.resolvedNumericValue();

                    checkAndAddValueToSink(baseAddr, value & 0xFF, byteSink, operand.sourceOperand().span());

                    baseAddr += 1;
                }
            }
        }
    }

    private void checkAndAddValueToSink(int currentAddr, int value, ByteSink byteSink, SourceSpan spanForErrors) {
        if (currentAddr < 0 || currentAddr > Architecture.MEMORY_SIZE_BYTES - 1)
            throw new EncodingException(spanForErrors, "current address outside '0.." + (Architecture.MEMORY_SIZE_BYTES - 1) + "'");
        if (byteSink.containsAddress(currentAddr))
            throw new EncodingException(spanForErrors, "duplicate byte write");
        if(value < 0 || value > 255)
            throw new EncodingException(spanForErrors, "byte value must be in '0..255', got " + value);

        byteSink.addressToValue().put(currentAddr, value);
    }
}
