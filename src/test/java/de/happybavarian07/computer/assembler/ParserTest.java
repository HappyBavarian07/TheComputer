package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.Parser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.LabelStatement;
import de.happybavarian07.computer.exceptions.assembler.ParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
    private Parser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultParser(new IndexedLexer());
    }

    @Test
    void parsesLabelAndInstruction() {
        parser.reset("start: mov r0, r1", "parse1.asm");

        Program program = parser.parse();
        List<Statement> statements = program.statements();

        assertEquals(2, statements.size());
        LabelStatement label = assertInstanceOf(LabelStatement.class, statements.get(0));
        InstructionStatement instruction = assertInstanceOf(InstructionStatement.class, statements.get(1));
        assertEquals("start", label.name());
        assertEquals("MOV", instruction.opcode());
        assertEquals(2, instruction.operandCount());
    }

    @Test
    void validatesInstructionArity() {
        parser.reset("mov r0", "parse2.asm");

        assertThrows(ParserException.class, () -> parser.parse());
    }

    @Test
    void rejectsUnknownOpcodes() {
        parser.reset("mvo r0, r1", "parse3.asm");

        assertThrows(ParserException.class, () -> parser.parse());
    }
}
