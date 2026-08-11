package de.happybavarian07.computer.assembler.parser;

import de.happybavarian07.computer.assembler.lexer.Lexer;
import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.TokenKind;
import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.OperandKind;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.EmptyStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.LabelStatement;
import de.happybavarian07.computer.exceptions.assembler.ParserException;
import de.happybavarian07.computer.isa.OpCode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultParser implements Parser {
    private static final EnumSet<OperandKind> R = EnumSet.of(OperandKind.REGISTER);
    private static final EnumSet<OperandKind> N = EnumSet.of(OperandKind.NUMBER, OperandKind.LABEL);
    private static final EnumSet<OperandKind> S = EnumSet.of(OperandKind.STRING);

    private static final Map<String, List<EnumSet<OperandKind>>> OPCODE_SIGNATURES = new HashMap<>();

    static {
        OPCODE_SIGNATURES.put("NOP", List.of());
        OPCODE_SIGNATURES.put("HALT", List.of());
        OPCODE_SIGNATURES.put("MOV", List.of(R, R));
        OPCODE_SIGNATURES.put("LOAD", List.of(R, N));
        OPCODE_SIGNATURES.put("LOADR", List.of(R, R));
        OPCODE_SIGNATURES.put("STORE", List.of(N, R));
        OPCODE_SIGNATURES.put("STORER", List.of(R, R));
        OPCODE_SIGNATURES.put("ADD", List.of(R, R));
        OPCODE_SIGNATURES.put("SUB", List.of(R, R));
        OPCODE_SIGNATURES.put("AND", List.of(R, R));
        OPCODE_SIGNATURES.put("OR", List.of(R, R));
        OPCODE_SIGNATURES.put("XOR", List.of(R, R));
        OPCODE_SIGNATURES.put("NOT", List.of(R));
        OPCODE_SIGNATURES.put("SHL", List.of(R, N));
        OPCODE_SIGNATURES.put("SHR", List.of(R, N));
        OPCODE_SIGNATURES.put("JMP", List.of(N));
        OPCODE_SIGNATURES.put("JZ", List.of(N));
        OPCODE_SIGNATURES.put("JNZ", List.of(N));
        OPCODE_SIGNATURES.put("PUSH", List.of(R));
        OPCODE_SIGNATURES.put("POP", List.of(R));
    }

    private final Lexer lexer;
    private String currentSource;
    private String currentFilePath;

    public DefaultParser(Lexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public Program parse() {
        lexer.reset(currentSource == null ? "" : currentSource, currentFilePath);
        Program program = new Program(currentFilePath);
        skipNewLines();

        while (peek().tokenKind() != TokenKind.EOF) {
            Statement statement = parseStatement();
            if (statement != null) {
                program.addStatement(statement);
            }
            skipNewLines();
        }

        List<Statement> statements = program.statements();
        if (!statements.isEmpty()) {
            SourceSpan first = statements.getFirst().span();
            SourceSpan last = statements.getLast().span();
            program.setSpan(new SourceSpan(
                    currentFilePath,
                    first.startLine(),
                    first.startColumn(),
                    last.endLine(),
                    last.endColumn()
            ));
        }
        return program;
    }

    @Override
    public Statement parseStatement() {
        Token token = peek();
        return switch (token.tokenKind()) {
            case LABEL_DEF -> parseLabelStatement();
            case DIRECTIVE -> parseDirectiveStatement();
            case IDENT -> parseInstructionStatement();
            case NEWLINE -> parseEmptyStatement();
            case EOF -> null;
            default -> throw error(token, "expected label, directive, or instruction");
        };
    }

    @Override
    public void reset(String source, String filePath) {
        this.currentSource = source;
        this.currentFilePath = filePath;
    }

    private Statement parseLabelStatement() {
        Token labelTok = expect(TokenKind.LABEL_DEF, "expected label");
        String labelName = labelTok.lexeme();
        if (labelName.endsWith(":")) {
            labelName = labelName.substring(0, labelName.length() - 1);
        }
        return new LabelStatement(labelName, spanOf(labelTok, labelTok));
    }

    private Statement parseInstructionStatement() {
        Token opTok = expect(TokenKind.IDENT, "expected opcode");
        String opName = opTok.lexeme().toUpperCase(Locale.ROOT);
        validateOpcode(opTok, opName);

        List<ParsedOperand> parsedOperands = parseOperandsForInstruction(opTok, opName);
        List<Operand> operands = parsedOperands.stream().map(p -> p.operand).toList();
        validateInstructionOperands(opTok, opName, operands);

        Token end = parsedOperands.isEmpty() ? opTok : parsedOperands.getLast().token;
        return new InstructionStatement(opName, operands, spanOf(opTok, end));
    }

    private Statement parseDirectiveStatement() {
        Token dirTok = expect(TokenKind.DIRECTIVE, "expected directive");
        List<ParsedOperand> parsedArgs = new ArrayList<>();

        while (canStartOperand(peek().tokenKind())) {
            parsedArgs.add(parseOperand());
            if (peek().tokenKind() == TokenKind.COMMA) {
                next();
                if (!canStartOperand(peek().tokenKind())) {
                    throw error(peek(), "expected operand after comma");
                }
            } else {
                break;
            }
        }

        List<Operand> args = parsedArgs.stream().map(p -> p.operand).toList();
        if (".ascii".equalsIgnoreCase(dirTok.lexeme())) {
            if (args.size() != 1 || !S.contains(args.getFirst().kind())) {
                throw error(dirTok, ".ascii expects exactly one string argument");
            }
        }

        Token end = parsedArgs.isEmpty() ? dirTok : parsedArgs.getLast().token;
        return new DirectiveStatement(dirTok.lexeme(), args, spanOf(dirTok, end));
    }

    private Statement parseEmptyStatement() {
        Token token = expect(TokenKind.NEWLINE, "expected newline");
        return new EmptyStatement(spanOf(token, token));
    }

    private List<ParsedOperand> parseOperandsForInstruction(Token opTok, String opName) {
        List<EnumSet<OperandKind>> signature = OPCODE_SIGNATURES.get(opName);
        List<ParsedOperand> operands = new ArrayList<>();
        int arity = signature == null ? 0 : signature.size();

        if (arity == 0) {
            return operands;
        }

        for (int i = 0; i < arity; i++) {
            if (i > 0) {
                expect(TokenKind.COMMA, "expected comma between operands");
            }
            if (!canStartOperand(peek().tokenKind())) {
                throw error(peek(), "expected operand " + (i + 1));
            }
            operands.add(parseOperand());
        }

        if (canStartOperand(peek().tokenKind()) || peek().tokenKind() == TokenKind.COMMA) {
            throw error(opTok, "too many operands for opcode " + opName);
        }

        return operands;
    }

    private void validateInstructionOperands(Token opTok, String opName, List<Operand> operands) {
        List<EnumSet<OperandKind>> signature = OPCODE_SIGNATURES.get(opName);
        if (signature == null) {
            return;
        }
        if (operands.size() != signature.size()) {
            throw error(opTok, "opcode " + opName + " expects " + signature.size() + " operands");
        }
        for (int i = 0; i < signature.size(); i++) {
            Operand operand = operands.get(i);
            EnumSet<OperandKind> allowed = signature.get(i);
            if (!allowed.contains(operand.kind())) {
                throw error(opTok, "operand " + (i + 1) + " for opcode " + opName + " must be one of " + allowed);
            }
        }
    }

    private ParsedOperand parseOperand() {
        Token token = next();
        Operand operand = switch (token.tokenKind()) {
            case REGISTER -> new Operand(OperandKind.REGISTER, token.lexeme(), null, spanOf(token, token));
            case NUMBER -> new Operand(
                    OperandKind.NUMBER,
                    token.lexeme(),
                    token.numberValue().isPresent() ? token.numberValue().getAsLong() : null,
                    spanOf(token, token)
            );
            case STRING -> new Operand(OperandKind.STRING, token.lexeme(), null, spanOf(token, token));
            case IDENT -> new Operand(OperandKind.LABEL, token.lexeme(), null, spanOf(token, token));
            default -> throw error(token, "expected operand");
        };
        return new ParsedOperand(operand, token);
    }

    private boolean canStartOperand(TokenKind tokenKind) {
        return tokenKind == TokenKind.REGISTER
                || tokenKind == TokenKind.NUMBER
                || tokenKind == TokenKind.STRING
                || tokenKind == TokenKind.IDENT;
    }

    private void validateOpcode(Token opTok, String opName) {
        try {
            OpCode.valueOf(opName);
        } catch (IllegalArgumentException ex) {
            throw error(opTok, "unknown opcode '" + opTok.lexeme() + "'");
        }
    }

    private void skipNewLines() {
        while (peek().tokenKind() == TokenKind.NEWLINE) {
            next();
        }
    }

    private Token expect(TokenKind kind, String message) {
        Token token = peek();
        if (token.tokenKind() != kind) {
            throw error(token, message + " but found " + token.tokenKind());
        }
        return next();
    }

    private Token peek() {
        return lexer.peek();
    }

    private Token next() {
        return lexer.next();
    }

    private SourceSpan spanOf(Token start, Token end) {
        int endColumnExclusive = end.startColumn() + Math.max(0, end.endIndex() - end.startIndex());
        return new SourceSpan(
                currentFilePath,
                start.startLine(),
                start.startColumn(),
                end.startLine(),
                Math.max(start.startColumn(), endColumnExclusive)
        );
    }

    private ParserException error(Token token, String message) {
        return new ParserException(currentFilePath, token.startLine(), token.startColumn(), message);
    }

    private record ParsedOperand(Operand operand, Token token) {
    }
}
