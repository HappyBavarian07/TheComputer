package de.happybavarian07.computer.disassembler;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DisassemblerTest {
    private final Disassembler disassembler = new Disassembler();

    private static long assembleSingle(String line) {
        DefaultParser parser = new DefaultParser(new IndexedLexer());
        parser.reset(line + "\n", "test.asm");
        var program = parser.parse();
        var resolved = new SymbolResolver().resolve(program);
        List<EncodedWord> words = new AssemblerEncoder().encodeProgram(resolved).words();
        return words.getFirst().rawWord();
    }

    @Test
    void doesNotThrowOnTopBitImmediate() {
        assertDoesNotThrow(() -> disassembler.disassemble(0x0000000080000000L));
        assertDoesNotThrow(() -> disassembler.disassemble(0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    void unknownOpcodeRendersAsWord() {
        assertEquals(".word 0xFFFFFFFFFFFFFFFF", disassembler.disassemble(0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    void roundTripsEveryImmediateKind() {
        String[] lines = {
                "movi r1, -5", "movi r1, 5", "movi r1, -2147483648", "movi r1, 2147483647",
                "addi r1, r2, -1", "subi r1, r2, -1", "cmpi r1, -7",
                "andi r1, r2, 0xFFFFFFFF", "ori r1, r2, 0x80000000", "xori r1, r2, 0",
                "shli r1, r2, 63", "shri r1, r2, 1",
                "jmp 0xFFFFFFFF", "call 0x100", "loadw r1, 0x200", "storew 0x200, r1",
                "loadr r1, r2, -8", "storer r1, r2, 16",
                "mov r1, sp", "mov sp, r1", "addi sp, sp, -16", "subi r3, sp, 8",
        };
        for (String line : lines) {
            long word = assembleSingle(line);
            String text = disassembler.disassemble(word);
            assertEquals(word, assembleSingle(text), line + " -> " + text);
        }
    }
}
