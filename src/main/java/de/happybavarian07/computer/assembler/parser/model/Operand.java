package de.happybavarian07.computer.assembler.parser.model;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 14:56
 */
public record Operand(OperandKind kind, String text, Number numericValue, SourceSpan span) {
}
