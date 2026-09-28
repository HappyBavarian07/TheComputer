package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.util.Architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 20:22
 */
public class AssemblerEncoder {
    private final ByteWordPacker byteWordPacker;
    private final DirectiveDataEmitter directiveDataEmitter;
    private final InstructionWordEncoder instructionWordEncoder;

    public AssemblerEncoder() {
        byteWordPacker = new ByteWordPacker();
        directiveDataEmitter = new DirectiveDataEmitter();
        instructionWordEncoder = new InstructionWordEncoder();
    }
    public EncodedProgram encodeProgram(ResolvedProgram resolvedProgram) {
        Map<Integer, EncodedWord> wordsByAddress = new HashMap<>();
        ByteSink byteSink = new ByteSink(new HashMap<>());

        for (ResolvedStatement statement : resolvedProgram.statements()) {
            if(statement.sourceStatement() instanceof InstructionStatement instructionStatement) {
                EncodedWord encodedWord = instructionWordEncoder.encode(statement);
                putEncodedWord(wordsByAddress, encodedWord, statement, resolvedProgram);
            } else if (statement.sourceStatement() instanceof DirectiveStatement directiveStatement) {
                directiveDataEmitter.emit(statement, byteSink);
            }
        }
        for (EncodedWord encodedWord : byteWordPacker.packBytesToWords(byteSink)) {
            putEncodedWord(wordsByAddress, encodedWord, null, resolvedProgram);
        }
        List<EncodedWord> encodedWords = new ArrayList<>(wordsByAddress.values());
        encodedWords.sort(null);
        return new EncodedProgram(encodedWords);
    }

    // Flat byte image of memory from address 0. Instructions occupy 8 bytes, data is byte-precise, overlaps are errors.
    public byte[] buildImage(ResolvedProgram resolvedProgram) {
        Map<Integer, Integer> bytes = new TreeMap<>();
        for (ResolvedStatement statement : resolvedProgram.statements()) {
            if (statement.sourceStatement() instanceof InstructionStatement) {
                long raw = instructionWordEncoder.encode(statement).rawWord();
                for (int i = 0; i < Architecture.INSTRUCTION_BYTES; i++) {
                    putByte(bytes, statement.address() + i, (int) ((raw >>> (i * 8)) & 0xFF), statement);
                }
            } else if (statement.sourceStatement() instanceof DirectiveStatement) {
                ByteSink statementBytes = new ByteSink(new HashMap<>());
                directiveDataEmitter.emit(statement, statementBytes);
                for (Map.Entry<Integer, Integer> entry : statementBytes.addressToValue().entrySet()) {
                    putByte(bytes, entry.getKey(), entry.getValue(), statement);
                }
            }
        }
        if (bytes.isEmpty()) return new byte[0];

        int end = ((TreeMap<Integer, Integer>) bytes).lastKey() + 1;
        int size = (end + Architecture.INSTRUCTION_BYTES - 1) / Architecture.INSTRUCTION_BYTES * Architecture.INSTRUCTION_BYTES;
        byte[] image = new byte[size];
        for (Map.Entry<Integer, Integer> entry : bytes.entrySet()) {
            image[entry.getKey()] = (byte) (int) entry.getValue();
        }
        return image;
    }

    private void putByte(Map<Integer, Integer> bytes, int address, int value, ResolvedStatement statement) {
        if (bytes.putIfAbsent(address, value) != null) {
            throw new EncodingException(statement.sourceStatement().span(), "overlapping output at address " + address);
        }
    }

    private void putEncodedWord(Map<Integer, EncodedWord> wordsByAddress, EncodedWord encodedWord, ResolvedStatement statement, ResolvedProgram resolvedProgram) {
        EncodedWord existing = wordsByAddress.putIfAbsent(encodedWord.byteAddress(), encodedWord);
        if (existing != null) {
            if (statement != null) {
                throw new EncodingException(statement.sourceStatement().span(), "multiple emitted words at address " + encodedWord.byteAddress());
            }
            var span = resolvedProgram.originalProgram().span();
            if (span != null) {
                throw new EncodingException(span, "multiple emitted words at address " + encodedWord.byteAddress());
            }
            throw new EncodingException(resolvedProgram.originalProgram().sourcePath(), 1, 1, "multiple emitted words at address " + encodedWord.byteAddress());
        }
    }
}
