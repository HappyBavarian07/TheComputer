package de.happybavarian07.computer.assembler.lexer.impl;

import de.happybavarian07.computer.assembler.lexer.Lexer;
import de.happybavarian07.computer.assembler.lexer.Token;
import de.happybavarian07.computer.assembler.lexer.TokenKind;
import de.happybavarian07.computer.exceptions.assembler.LexerException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public class IndexedLexer implements Lexer {
    private char[] buffer = new char[0];
    private String filePath;
    private int index = 0;
    private int line = 1;
    private int column = 1;
    private int snippetStartIndex = 0;
    private final int tabWidth;

    private final Deque<Token> lookahead = new ArrayDeque<>();

    public IndexedLexer() { this(4); }

    public IndexedLexer(int tabWidth) { this.tabWidth = Math.max(1, tabWidth); }

    @Override
    public Token next() {
        if (!lookahead.isEmpty()) {
            return lookahead.pollFirst();
        }
        return lexOne();
    }

    @Override
    public Token peek() {
        return peek(1);
    }

    @Override
    public Token peek(int n) {
        if (n <= 0) throw new IllegalArgumentException("peek(n) requires n>0");
        ensureLookahead(n);
        int i = 1;
        for (Token t : lookahead) {
            if (i == n) return t;
            i++;
        }
        return null;
    }

    @Override
    public List<Token> tokenizeAll() {
        List<Token> tokens = new ArrayList<>();
        Token t = next();
        while (t != null && t.tokenKind() != TokenKind.EOF) {
            tokens.add(t);
            t = next();
        }
        tokens.add(t);
        return tokens;
    }

    @Override
    public void reset(String source, String filePath) {
        if (source == null) source = "";
        this.buffer = source.toCharArray();
        this.filePath = filePath;
        this.index = 0;
        this.line = 1;
        this.column = 1;
        this.snippetStartIndex = 0;
        this.lookahead.clear();
    }

    private void ensureLookahead(int n) {
        while (lookahead.size() < n) {
            Token t = lexOne();
            lookahead.addLast(t);
            if (t != null && t.tokenKind() == TokenKind.EOF) break;
        }
    }

    private Token lexOne() {
        skipWhitespaceAndComments();
        if (eof()) {
            return new Token(TokenKind.EOF, "", OptionalLong.empty(), Optional.empty(), line, column, index, index);
        }

        int startLine = line;
        int startColumn = column;
        int startIndex = index;
        char c = peekChar();

        if (c == '\n' || c == '\r') {
            consumeNewline();
            return new Token(TokenKind.NEWLINE, "\n", OptionalLong.empty(), Optional.empty(), startLine, startColumn, startIndex, index);
        }

        if (isLetter(c) || c == '.' || c == '_') {
            String lexeme = collectIdentifierOrDirective();
        TokenKind kind;
        if (isRegisterLexeme(lexeme)) {
            kind = TokenKind.REGISTER;
        } else if (lexeme.startsWith(".")) {
            kind = TokenKind.DIRECTIVE;
        } else if (lexeme.endsWith(":")) {
            kind = TokenKind.LABEL_DEF;
        } else {
            kind = TokenKind.IDENT;
        }
        return new Token(kind, lexeme, OptionalLong.empty(), Optional.empty(), startLine, startColumn, startIndex, index);
        }

        if (isDigit(c)) {
            String num = collectNumber();
            OptionalLong value = parseNumberValue(num);
            return new Token(TokenKind.NUMBER, num, value, Optional.empty(), startLine, startColumn, startIndex, index);
        }

        if (c == '"' || c == '\'') {
            String s = collectString();
            if (s == null) {
                String snippet = currentLineSnippet();
                throw new LexerException(filePath, startLine, startColumn, snippet, 1, "unterminated string", tabWidth);
            }
            return new Token(TokenKind.STRING, s, OptionalLong.empty(), Optional.of(s), startLine, startColumn, startIndex, index);
        }

        return switch (c) {
            case ',' -> {
                advance();
                yield new Token(TokenKind.COMMA, ",", OptionalLong.empty(), Optional.empty(), startLine, startColumn, startIndex, index);
            }
            case ':' -> {
                advance();
                yield new Token(TokenKind.COLON, ":", OptionalLong.empty(), Optional.empty(), startLine, startColumn, startIndex, index);
            }
            case ';' -> {
                consumeUntilNewline();
                yield lexOne();
            }
            default -> {
                String snippet = currentLineSnippet();
                throw new LexerException(filePath, startLine, startColumn, snippet, 1, "unexpected character: '" + c + "'", tabWidth);
            }
        };
    }

    private void skipWhitespaceAndComments() {
        while (!eof()) {
            char c = peekChar();
            if (c == ' ') {
                advance();
            } else if (c == '\t') {
                advance();
            } else if (c == ';' || c == '#') {
                consumeUntilNewline();
            } else if (c == '/' && peekNextChar() == '/') {
                consumeUntilNewline();
            } else {
                break;
            }
        }
    }

    private void consumeUntilNewline() {
        while (!eof() && peekChar() != '\n' && peekChar() != '\r') {
            advance();
        }
    }

    private void consumeNewline() {
        if (eof()) return;
        if (peekChar() == '\r') {
            index++;
            if (!eof() && peekChar() == '\n') {
                index++;
            }
        } else if (peekChar() == '\n') {
            index++;
        } else {
            return;
        }
        line++;
        column = 1;
        snippetStartIndex = index;
    }

    private String collectIdentifierOrDirective() {
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = peekChar();
            if (isLetterOrDigitOrUnderscore(c) || c == '.') {
                sb.append(c);
                advance();
                continue;
            }
            if (c == ':') {
                sb.append(c);
                advance();
                break;
            }
            break;
        }
        return sb.toString();
    }

    private String collectNumber() {
        StringBuilder sb = new StringBuilder();
        if (peekChar() == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X' || peekNextChar() == 'b' || peekNextChar() == 'B')) {
            sb.append(peekChar());
            advance();
            sb.append(peekChar());
            advance();
            char p = Character.toLowerCase(sb.charAt(1));
            while (!eof()) {
                char c = peekChar();
                if (c == '_') {
                    sb.append(c);
                    advance();
                    continue;
                }
                if (p == 'x' && isHexDigit(c)) {
                    sb.append(c);
                    advance();
                    continue;
                }
                if (p == 'b' && (c == '0' || c == '1')) {
                    sb.append(c);
                    advance();
                    continue;
                }
                break;
            }
            return sb.toString();
        }
        while (!eof()) {
            char c = peekChar();
            if (isDigit(c) || c == '_') {
                sb.append(c);
                advance();
            } else break;
        }
        return sb.toString();
    }

    private String collectString() {
        char quote = peekChar();
        advance();
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = peekChar();
            if (c == quote) {
                advance();
                return sb.toString();
            }
            if (c == '\\') {
                advance();
                if (eof()) break;
                char esc = peekChar();
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('\"');
                    case '\'' -> sb.append('\'');
                    case '0' -> sb.append('\0');
                    default -> sb.append(esc);
                }
                advance();
                continue;
            }
            if (c == '\n' || c == '\r') return null;
            sb.append(c);
            advance();
        }
        return null;
    }

    private String currentLineSnippet() {
        int i = snippetStartIndex;
        int end = i;
        while (end < buffer.length && buffer[end] != '\n' && buffer[end] != '\r') end++;
        return new String(buffer, i, end - i);
    }

    private OptionalLong parseNumberValue(String lexeme) {
        if (lexeme == null || lexeme.isEmpty()) return OptionalLong.empty();
        String s = lexeme.replace("_", "");
        try {
            if (s.length() > 1 && s.charAt(0) == '0' && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                String digits = s.substring(2);
                if (digits.isEmpty()) return OptionalLong.empty();
                long v = Long.parseUnsignedLong(digits, 16);
                return OptionalLong.of(v);
            }
            if (s.length() > 1 && s.charAt(0) == '0' && (s.charAt(1) == 'b' || s.charAt(1) == 'B')) {
                String digits = s.substring(2);
                if (digits.isEmpty()) return OptionalLong.empty();
                long v = Long.parseUnsignedLong(digits, 2);
                return OptionalLong.of(v);
            }
            long v = Long.parseLong(s, 10);
            return OptionalLong.of(v);
        } catch (NumberFormatException ex) {
            return OptionalLong.empty();
        }
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isLetter(char c) {
        return Character.isLetter(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetterOrDigitOrUnderscore(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean isRegisterLexeme(String lexeme) {
        if (lexeme == null || lexeme.isEmpty()) return false;
        String s = lexeme;
        if (s.endsWith(":")) s = s.substring(0, s.length() - 1);
        if (s.isEmpty()) return false;
        if (s.length() >= 2 && (s.charAt(0) == 'R' || s.charAt(0) == 'r')) {
            for (int i = 1; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
            return true;
        }

        String up = s.toUpperCase();
        return up.equals("PC") || up.equals("SP") || up.equals("IR") || up.equals("FLAGS");
    }

    private char peekChar() {
        if (index >= buffer.length) return '\0';
        return buffer[index];
    }

    private char peekNextChar() {
        int p = index + 1;
        if (p >= buffer.length) return '\0';
        return buffer[p];
    }

    private boolean eof() {
        return index >= buffer.length;
    }

    private void advance() {
        if (index >= buffer.length) return;
        char ch = buffer[index++];
        if (ch == '\n') {
            line++;
            column = 1;
        } else if (ch == '\t') {
            column += (tabWidth - ((column - 1) % tabWidth));
        } else {
            column++;
        }
    }
}
