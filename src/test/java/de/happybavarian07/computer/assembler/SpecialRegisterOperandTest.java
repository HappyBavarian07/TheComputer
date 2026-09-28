package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialRegisterOperandTest {
    private static void assemble(String source) {
        DefaultParser parser = new DefaultParser(new IndexedLexer());
        parser.reset(source + "\n", "test.asm");
        var resolved = new SymbolResolver().resolve(parser.parse());
        new AssemblerEncoder().encodeProgram(resolved);
    }

    @Test
    void spIsAcceptedInMovAddiSubi() {
        assertDoesNotThrow(() -> assemble("mov r1, sp"));
        assertDoesNotThrow(() -> assemble("mov sp, r1"));
        assertDoesNotThrow(() -> assemble("addi sp, sp, -16"));
        assertDoesNotThrow(() -> assemble("subi r2, sp, 8"));
    }

    @Test
    void spIsRejectedElsewhereWithDiagnostic() {
        EncodingException ex = assertThrows(EncodingException.class, () -> assemble("add r1, sp, r2"));
        assertTrue(ex.getMessage().contains("only allowed in mov, addi and subi"), ex.getMessage());
    }

    @Test
    void pcIrFlagsGiveDiagnosticInsteadOfNullPointer() {
        for (String reg : new String[]{"pc", "ir", "flags"}) {
            EncodingException ex = assertThrows(EncodingException.class, () -> assemble("mov r1, " + reg), reg);
            assertTrue(ex.getMessage().contains("'" + reg + "' cannot be used as an operand"), ex.getMessage());
        }
    }
}
