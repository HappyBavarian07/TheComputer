package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.Parser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.isa.OpCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EncoderTest {
    private Parser parser;
    private SymbolResolver resolver;
    private AssemblerEncoder encoder;

    @BeforeEach
    void setUp() {
        parser = new DefaultParser(new IndexedLexer());
        resolver = new SymbolResolver();
        encoder = new AssemblerEncoder();
    }

    @Test
    void encode_nop_and_halt() {
        String src = "nop\nhalt";
        EncodedProgram ep = encode(src);
        List<EncodedWord> words = ep.words();

        EncodedWord w0 = words.stream().filter(w -> w.byteAddress() == 0).findFirst().orElseThrow();
        EncodedWord w4 = words.stream().filter(w -> w.byteAddress() == 4).findFirst().orElseThrow();

        int expectedNop = (OpCode.NOP.binaryValue() << 26);
        int expectedHalt = (OpCode.HALT.binaryValue() << 26);

        assertEquals(expectedNop, w0.rawWord());
        assertEquals(expectedHalt, w4.rawWord());
    }

    @Test
    void encode_mov_registers() {
        String src = "mov r1, r2";
        EncodedProgram ep = encode(src);
        EncodedWord w0 = ep.words().stream().filter(w -> w.byteAddress() == 0).findFirst().orElseThrow();

        int expected = (OpCode.MOV.binaryValue() << 26) | (1 << 21) | (2 << 16);
        assertEquals(expected, w0.rawWord());
    }

    @Test
    void pack_ascii_into_single_word() {
        // ASCII bytes: 65, 10, 0
        String src = ".ascii \"A\\n\\0\"";
        EncodedProgram ep = encode(src);
        List<EncodedWord> words = ep.words();
        assertEquals(1, words.size());

        EncodedWord w0 = words.get(0);
        int expected = (65) | (10 << 8) | (0 << 16) | (0 << 24);
        assertEquals(0, w0.byteAddress());
        assertEquals(expected, w0.rawWord());
    }

    @Test
    void directive_instruction_collision_throws() {
        // nop at address 0; .org 0 and .word 1 cause a collision at address 0
        String src = "nop\n.org 0\n.word 1";
        assertThrows(EncodingException.class, () -> encode(src));
    }

    private EncodedProgram encode(String src) {
        parser.reset(src, "file.asm");
        Program program = parser.parse();
        ResolvedProgram rp = resolver.resolve(program);
        return encoder.encodeProgram(rp);
    }
}
