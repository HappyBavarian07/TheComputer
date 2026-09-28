package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;

/**
 * The one assembler pipeline (lexer, parser, resolver, encoder) used by the command line tool and the GUI.
 * The image is the flat byte layout of memory starting at address 0: instructions are 8 little-endian bytes,
 * data directives are byte-precise, gaps are zero. Failures surface as Lexer/Parser/Resolution/EncodingException.
 */
public final class AssemblerService {
    public record Result(byte[] image, Program program, ResolvedProgram resolved, EncodedProgram encoded) {
    }

    public Result assemble(String source, String sourcePath) {
        DefaultParser parser = new DefaultParser(new IndexedLexer());
        parser.reset(source, sourcePath);
        Program program = parser.parse();
        ResolvedProgram resolved = new SymbolResolver().resolve(program);
        AssemblerEncoder encoder = new AssemblerEncoder();
        byte[] image = encoder.buildImage(resolved);
        EncodedProgram encoded = encoder.encodeProgram(resolved);
        return new Result(image, program, resolved, encoded);
    }
}
