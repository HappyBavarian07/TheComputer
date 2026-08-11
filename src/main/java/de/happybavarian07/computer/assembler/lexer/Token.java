package de.happybavarian07.computer.assembler.lexer;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * @param line       1-based
 * @param column     1-based & start of token
 * @param startIndex byte/char index in source
 */ /*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 12:53
 */
public record Token(TokenKind tokenKind,
                    String lexeme,
                    OptionalLong numberValue,
                    Optional<String> literal,
                    int startLine, int startColumn,
                    int startIndex, int endIndex) {
}
