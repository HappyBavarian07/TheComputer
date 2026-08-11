package de.happybavarian07.computer.assembler.resolver.model;

import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.OperandKind;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 15:36
 */
public record ResolvedOperand(Operand sourceOperand, OperandKind kind, String text, Integer resolvedNumericValue) {
}
