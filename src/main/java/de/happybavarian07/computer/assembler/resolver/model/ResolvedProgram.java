package de.happybavarian07.computer.assembler.resolver.model;

import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.resolver.SymbolTable;

import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 15:41
 */
public record ResolvedProgram(Program originalProgram, SymbolTable symbols, List<ResolvedStatement> statements) {
}
