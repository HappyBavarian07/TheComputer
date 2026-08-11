package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.TokenKind;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.Parser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.EmptyStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.LabelStatement;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolResolverTest {
    private Parser parser;
    private SymbolResolver resolver;

    @BeforeEach
    void setUp() {
        parser = new DefaultParser(new IndexedLexer());
        resolver = new SymbolResolver();
    }


    @Test
    void pass1CollectLabelsAndAddresses() {
        parser.reset("start: mov r0, r1\n nop", "pass1.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        List<Integer> addressList = new ArrayList<>(p1.statementAddresses().values());

        assertEquals(0, p1.symbolTable().getLocation("start"));
        assertTrue(addressList.contains(4));
    }

    @Test
    void pass1DuplicateLabels() {
        parser.reset("a: nop\na: nop", "pass1.asm");
        Program program = parser.parse();

        assertThrows(ResolutionException.class, () -> resolver.pass1(program));
    }

    @Test
    void pass1OrgSetsAddress() {
        parser.reset(".org 16\n nop", "pass1.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);

        assertTrue(p1.statementAddresses().containsValue(16));
    }

    @Test
    void pass2ResolveForwardLabel() {
        parser.reset("jmp target\n target: nop", "file.asm");
        Program program = parser.parse();

        ResolvedProgram rp = resolver.resolve(program);

        ResolvedStatement rs = rp.statements().getFirst();
        assertInstanceOf(InstructionStatement.class, rs.sourceStatement());
        List<ResolvedOperand> ops = rs.operands();
        assertEquals(1, ops.size());
        assertEquals(4, ops.getFirst().resolvedNumericValue());
    }

    @Test
    void pass2UnknownLabelThrows() {
        parser.reset("jmp missing", "file.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        assertThrows(ResolutionException.class, () -> resolver.resolve(program));
    }

    @Test
    void pass2WordAndByteExpansion() {
        parser.reset(".word 1, 2\n.byte 3, 255", "file.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        ResolvedProgram rp = resolver.resolve(program);

        // first statement is .word
        ResolvedStatement wordStmt = rp.statements().stream()
                .filter(s -> s.sourceStatement() instanceof DirectiveStatement ds && ds.name().equalsIgnoreCase(".word"))
                .findFirst().orElseThrow();
        List<ResolvedOperand> wordOps = wordStmt.operands();
        assertEquals(2, wordOps.size());
        assertEquals(1, wordOps.get(0).resolvedNumericValue());
        assertEquals(2, wordOps.get(1).resolvedNumericValue());

        ResolvedStatement byteStmt = rp.statements().stream()
                .filter(s -> s.sourceStatement() instanceof DirectiveStatement ds && ds.name().equalsIgnoreCase(".byte"))
                .findFirst().orElseThrow();
        List<ResolvedOperand> byteOps = byteStmt.operands();
        assertEquals(2, byteOps.size());
        assertEquals(3, byteOps.get(0).resolvedNumericValue());
        assertEquals(255, byteOps.get(1).resolvedNumericValue());
    }

    @Test
    void pass2AsciiExpansionAndEscapes() {
        String src = ".ascii \"A\\n\"";
        parser.reset(src, "file.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        SymbolResolver.Pass2Result p2 = resolver.pass2(program, p1);
        String diagnostics = diagnostics(src, program, p1, p2);

        ResolvedStatement asciiStmt = p2.resolvedStatements().stream()
                .filter(s -> s.sourceStatement() instanceof DirectiveStatement ds && ds.name().equalsIgnoreCase(".ascii"))
                .findFirst().orElseThrow();
        List<ResolvedOperand> ops = asciiStmt.operands();
        assertEquals(2, ops.size(), diagnostics);
        assertEquals(65, ops.get(0).resolvedNumericValue(), diagnostics);
        assertEquals(10, ops.get(1).resolvedNumericValue(), diagnostics);
    }

    @Test
    void pass2UnalignedWordThrows() {
        parser.reset(".org 1\n.word 1", "file.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        assertThrows(ResolutionException.class, () -> resolver.resolve(program));
    }

    @Test
    void endToEndIntegration() {
        String src =
                """
                        start:     nop
                        .org 16
                        data: .word start
                        jmp data
                        """;
        parser.reset(src, "file.asm");
        Program program = parser.parse();

        SymbolResolver.Pass1Result p1 = resolver.pass1(program);
        SymbolResolver.Pass2Result p2 = resolver.pass2(program, p1);
        String diagnostics = diagnostics(src, program, p1, p2);
        System.out.println(diagnostics);

        assertEquals(0, p1.symbolTable().getLocation("start"), diagnostics);
        assertTrue(p1.symbolTable().exists("data"), diagnostics);

        ResolvedStatement jmpStmt = p2.resolvedStatements().stream()
                .filter(s -> s.sourceStatement() instanceof InstructionStatement ins && ins.opcode().equalsIgnoreCase("JMP"))
                .findFirst().orElseThrow();
        assertEquals(p1.symbolTable().getLocation("data"), jmpStmt.operands().getFirst().resolvedNumericValue(), diagnostics);
    }

    private String diagnostics(String source, Program program, SymbolResolver.Pass1Result pass1, SymbolResolver.Pass2Result pass2) {
        StringBuilder out = new StringBuilder();
        out.append("src:\n");
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            out.append(i + 1).append(": ").append(escapeForDisplay(lines[i])).append('\n');
        }

        IndexedLexer lexer = new IndexedLexer();
        lexer.reset(source, program.sourcePath());
        List<Token> tokens = lexer.tokenizeAll();
        Map<Integer, List<String>> tokensByLine = new LinkedHashMap<>();
        for (Token token : tokens) {
            String tokenText;
            if (token.tokenKind() == TokenKind.NEWLINE) {
                tokenText = "\\n";
            } else {
                tokenText = escapeForDisplay(token.lexeme());
            }
            tokensByLine.computeIfAbsent(token.startLine(), ignored -> new ArrayList<>())
                    .add(token.tokenKind() + "('" + tokenText + "')");
        }
        out.append("tokens:\n");
        for (Map.Entry<Integer, List<String>> entry : tokensByLine.entrySet()) {
            out.append(entry.getKey()).append(": ").append(String.join(" ", entry.getValue())).append('\n');
        }

        out.append("parsed:\n");
        for (Statement statement : program.statements()) {
            out.append(statement.span().startLine())
                    .append(": ")
                    .append(formatParsedStatement(statement))
                    .append('\n');
        }

        out.append("resolved:\n");
        for (ResolvedStatement statement : pass2.resolvedStatements()) {
            out.append(statement.address())
                    .append(": ")
                    .append(formatResolvedStatement(statement))
                    .append('\n');
        }

        out.append("symbols: ").append(pass1.symbolTable()).append('\n');
        return out.toString();
    }

    private String formatParsedStatement(Statement statement) {
        if (statement instanceof LabelStatement labelStatement) {
            return "label " + labelStatement.name();
        }
        if (statement instanceof InstructionStatement instructionStatement) {
            return "insn " + instructionStatement.opcode() + " " + formatParsedOperands(instructionStatement.operands());
        }
        if (statement instanceof DirectiveStatement directiveStatement) {
            return "dir " + directiveStatement.name() + " " + formatParsedOperands(directiveStatement.arguments());
        }
        if (statement instanceof EmptyStatement) {
            return "empty";
        }
        return statement.kind().name().toLowerCase();
    }

    private String formatParsedOperands(List<Operand> operands) {
        if (operands.isEmpty()) {
            return "[]";
        }
        List<String> rendered = new ArrayList<>();
        for (Operand operand : operands) {
            if (operand.numericValue() != null) {
                rendered.add(operand.kind() + "(" + operand.text() + "=" + operand.numericValue() + ")");
            } else {
                rendered.add(operand.kind() + "(" + escapeForDisplay(operand.text()) + ")");
            }
        }
        return "[" + String.join(", ", rendered) + "]";
    }

    private String formatResolvedStatement(ResolvedStatement statement) {
        Statement source = statement.sourceStatement();
        if (source instanceof LabelStatement labelStatement) {
            return "label " + labelStatement.name() + " -> @" + statement.address();
        }
        if (source instanceof InstructionStatement instructionStatement) {
            return "insn " + instructionStatement.opcode() + " " + formatResolvedOperands(statement.operands());
        }
        if (source instanceof DirectiveStatement directiveStatement) {
            return "dir " + directiveStatement.name() + " " + formatResolvedOperands(statement.operands());
        }
        if (source instanceof EmptyStatement) {
            return "empty";
        }
        return source.kind().name().toLowerCase() + " " + formatResolvedOperands(statement.operands());
    }

    private String formatResolvedOperands(List<ResolvedOperand> operands) {
        if (operands.isEmpty()) {
            return "[]";
        }
        List<String> rendered = new ArrayList<>();
        for (ResolvedOperand operand : operands) {
            Integer value = operand.resolvedNumericValue();
            String text = escapeForDisplay(operand.text());
            if (value != null) {
                rendered.add(operand.kind() + "(" + text + "->" + value + ")");
            } else {
                rendered.add(operand.kind() + "(" + text + ")");
            }
        }
        return "[" + String.join(", ", rendered) + "]";
    }

    private String escapeForDisplay(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
