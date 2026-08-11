package de.happybavarian07.computer.assembler.lexer;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 12:49
 */
public enum TokenKind {
    EOF, NEWLINE, IDENT, DIRECTIVE, LABEL_DEF, OPCODE, REGISTER, NUMBER, STRING, COMMA, COLON, COMMENT, ERROR;
}
