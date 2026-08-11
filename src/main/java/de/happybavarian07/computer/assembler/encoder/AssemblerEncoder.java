package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
