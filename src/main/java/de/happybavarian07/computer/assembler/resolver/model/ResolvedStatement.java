package de.happybavarian07.computer.assembler.resolver.model;

import de.happybavarian07.computer.assembler.parser.model.Statement;

import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 15:39
 */
public record ResolvedStatement(Statement sourceStatement, int address, List<ResolvedOperand> operands) {
}
