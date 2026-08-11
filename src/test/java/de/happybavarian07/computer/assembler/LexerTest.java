package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.lexer.Lexer;
import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.TokenKind;
import de.happybavarian07.computer.exceptions.assembler.LexerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {
    private Lexer lexer;

    @BeforeEach
    void setUp() {
        lexer = new IndexedLexer();
    }

    @Test
    void tokenizesRegisterMove() {
        lexer.reset("mov r0, r1", "happypath");

        List<Token> tokens = lexer.tokenizeAll();

        assertTokenKinds(tokens, TokenKind.IDENT, TokenKind.REGISTER, TokenKind.COMMA, TokenKind.REGISTER, TokenKind.EOF);
        assertTokenLexemes(tokens, "mov", "r0", ",", "r1", "");
    }

    @Test
    void tokenizesNumbersAndRegisters() {
        lexer.reset("load R15, 0xDEAD_BEEF 0b1010 1_000", "numbers");

        List<Token> tokens = lexer.tokenizeAll();

        assertTokenKinds(tokens,
                TokenKind.IDENT,
                TokenKind.REGISTER,
                TokenKind.COMMA,
                TokenKind.NUMBER,
                TokenKind.NUMBER,
                TokenKind.NUMBER,
                TokenKind.EOF);
        assertEquals(0xDEADBEEFL, tokens.get(3).numberValue().orElseThrow());
        assertEquals(0b1010L, tokens.get(4).numberValue().orElseThrow());
        assertEquals(1000L, tokens.get(5).numberValue().orElseThrow());
    }

    @Test
    void skipsWhitespaceAndComments() {
        lexer.reset("  mov\tR15,PC ; comment\n# another comment\n", "comments");

        List<Token> tokens = lexer.tokenizeAll();

        assertTokenKinds(tokens, TokenKind.IDENT, TokenKind.REGISTER, TokenKind.COMMA, TokenKind.REGISTER, TokenKind.NEWLINE, TokenKind.NEWLINE, TokenKind.EOF);
        assertTokenLexemes(tokens, "mov", "R15", ",", "PC", "\n", "\n", "");
    }

    @Test
    void supportsPeekLookahead() {
        lexer.reset("mov r0, r1", "peek");

        assertEquals(TokenKind.IDENT, lexer.peek().tokenKind());
        assertEquals(TokenKind.REGISTER, lexer.peek(2).tokenKind());
        assertEquals(TokenKind.IDENT, lexer.next().tokenKind());
        assertEquals(TokenKind.REGISTER, lexer.peek().tokenKind());
    }

    @Test
    void reportsUnexpectedCharacters() {
        lexer.reset("mov @r0", "badchar");

        LexerException ex = assertThrows(LexerException.class, () -> lexer.tokenizeAll());

        assertTrue(ex.getMessage().contains("unexpected character"));
        assertTrue(ex.getMessage().contains("@"));
        assertEquals("badchar", ex.getFilePath());
    }

    @Test
    void reportsUnterminatedString() {
        lexer.reset("msg \"hello", "string");

        LexerException ex = assertThrows(LexerException.class, () -> lexer.tokenizeAll());

        assertTrue(ex.getMessage().contains("unterminated string"));
        assertEquals("string", ex.getFilePath());
    }

    private static void assertTokenKinds(List<Token> tokens, TokenKind... kinds) {
        assertIterableEquals(List.of(kinds), tokens.stream().map(Token::tokenKind).toList());
    }

    private static void assertTokenLexemes(List<Token> tokens, String... lexemes) {
        assertIterableEquals(List.of(lexemes), tokens.stream().map(Token::lexeme).toList());
    }
}
