package de.happybavarian07.computer.assembler;

import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 13:01
 */
public interface Lexer {
    Token next();

    Token peek();

    Token peek(int n);

    List<Token> tokenizeAll();

    void reset(String source, String filePath);
}
