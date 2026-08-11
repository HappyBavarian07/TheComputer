package de.happybavarian07.computer.assembler.resolver;

import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.OperandKind;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.Statement;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.EmptyStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.InstructionStatement;
import de.happybavarian07.computer.assembler.parser.model.statement.LabelStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import de.happybavarian07.computer.util.Architecture;

import java.io.ByteArrayOutputStream;
import java.util.*;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 15:34
 */
public class SymbolResolver {
    public SymbolResolver() {
    }

    public Pass1Result pass1(Program program) {
        SymbolTable symbolTable = new SymbolTable();
        int locationCounter = 0;
        Map<Statement, Integer> statementAddresses = new HashMap<>();
        for (Statement statement : program.statements()) {
            statementAddresses.putIfAbsent(statement, locationCounter);

            if (statement instanceof LabelStatement labelStatement) {
                String name = labelStatement.name();
                if (symbolTable.exists(name)) {
                    throw new ResolutionException(statement.span(), "duplicate label '" + name + "'");
                }
                symbolTable.map(name, locationCounter);
            }
            if (statement instanceof InstructionStatement) {
                locationCounter += 4;
            } else if (statement instanceof DirectiveStatement directiveStatement) {
                locationCounter = applyDirective(locationCounter, directiveStatement);
                if (".org".equalsIgnoreCase(directiveStatement.name())) {
                    statementAddresses.put(statement, locationCounter);
                }
            }
        }

        return new Pass1Result(symbolTable, statementAddresses);
    }

    public Pass2Result pass2(Program program, Pass1Result pass1Result) {
        SymbolTable symbolTable = pass1Result.symbolTable;
        Map<Statement, Integer> statementAddresses = pass1Result.statementAddresses;

        List<ResolvedStatement> outResolvedStatements = new ArrayList<>();
        for (Statement statement : program.statements()) {
            int address = statementAddresses.get(statement);

            List<ResolvedOperand> resolvedOperands = new ArrayList<>();

            if (statement instanceof InstructionStatement instructionStatement) {
                instructionStatement.operands().forEach(operand -> resolvedOperands.add(resolveOperand(operand, symbolTable)));
            } else if (statement instanceof DirectiveStatement directiveStatement) {
                resolvedOperands.addAll(expandDirective(directiveStatement, address, symbolTable));
            }

            outResolvedStatements.add(new ResolvedStatement(statement, address, resolvedOperands));
        }

        return new Pass2Result(outResolvedStatements);
    }

    public ResolvedProgram resolve(Program program) {
        Pass1Result pass1Result = pass1(program);
        Pass2Result pass2Result = pass2(program, pass1Result);
        return new ResolvedProgram(program, pass1Result.symbolTable, pass2Result.resolvedStatements);
    }


    private List<ResolvedOperand> expandDirective(DirectiveStatement directiveStatement, int baseAddress, SymbolTable symbolTable) {
        String name = directiveStatement.name().toLowerCase(Locale.ROOT);
        List<ResolvedOperand> out = new ArrayList<>();
        switch (name) {
            case ".org": {
                int addr = parseOrgAddress(directiveStatement);
                if (addr != baseAddress) {
                    throw new ResolutionException(directiveStatement.span(), ".org address mismatch with pass1 assignment");
                }
                return out;
            }
            case ".word": {
                int addr = baseAddress;
                for (Operand arg : directiveStatement.arguments()) {
                    ResolvedOperand ro = resolveOperand(arg, symbolTable);
                    if (ro.kind() != OperandKind.NUMBER && ro.kind() != OperandKind.LABEL) {
                        throw new ResolutionException(arg.span(), ".word requires numeric or label argument");
                    }
                    Integer val = ro.resolvedNumericValue();
                    if (val == null) {
                        throw new ResolutionException(arg.span(), ".word could not resolve value");
                    }
                    if ((addr % 4) != 0) {
                        throw new ResolutionException(directiveStatement.span(), "unaligned .word at address " + addr);
                    }
                    out.add(new ResolvedOperand(arg, OperandKind.NUMBER, arg.text(), val));
                    addr += 4;
                }
                return out;
            }
            case ".byte": {
                int addr = baseAddress;
                for (Operand arg : directiveStatement.arguments()) {
                    ResolvedOperand ro = resolveOperand(arg, symbolTable);
                    if (ro.kind() != OperandKind.NUMBER && ro.kind() != OperandKind.LABEL) {
                        throw new ResolutionException(arg.span(), ".byte requires numeric or label argument");
                    }
                    Integer val = ro.resolvedNumericValue();
                    if (val == null) {
                        throw new ResolutionException(arg.span(), ".byte could not resolve value");
                    }
                    int byteVal = val & 0xFF;
                    out.add(new ResolvedOperand(arg, OperandKind.NUMBER, Integer.toString(byteVal), byteVal));
                    addr += 1;
                }
                return out;
            }
            case ".ascii": {
                if (directiveStatement.arguments().size() != 1) {
                    throw new ResolutionException(directiveStatement.span(), ".ascii expects exactly one string argument");
                }
                Operand sArg = directiveStatement.arguments().getFirst();
                if (sArg.kind() != OperandKind.STRING) {
                    throw new ResolutionException(sArg.span(), ".ascii expects a string argument");
                }
                byte[] bytes = decodeStringLiteral(sArg.text());
                int addr = baseAddress;
                for (byte b : bytes) {
                    int ub = b & 0xFF;
                    out.add(new ResolvedOperand(sArg, OperandKind.NUMBER, Integer.toString(ub), ub));
                    addr += 1;
                }
                return out;
            }
            default:
                return out;
        }
    }

    private byte[] decodeStringLiteral(String text) {
        String s = text;
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> baos.write('\n');
                    case 'r' -> baos.write('\r');
                    case 't' -> baos.write('\t');
                    case '\\' -> baos.write('\\');
                    case '"' -> baos.write('\"');
                    case '\'' -> baos.write('\'');
                    case '0' -> baos.write(0);
                    default -> baos.write(n);
                }
            } else {
                baos.write((byte) c);
            }
        }
        return baos.toByteArray();
    }

    private ResolvedOperand resolveOperand(Operand sourceOperand, SymbolTable symbolTable) {
        switch (sourceOperand.kind()) {
            case REGISTER: {
                String text = sourceOperand.text();
                Integer idx;
                try {
                    String digits = text.replaceAll("(?i)r", "");
                    if (!digits.isEmpty()) {
                        idx = Integer.parseInt(digits);
                    } else {
                        idx = null;
                    }
                } catch (NumberFormatException ignored) {
                    idx = null;
                }
                return new ResolvedOperand(sourceOperand, sourceOperand.kind(), sourceOperand.text(), idx);
            }
            case NUMBER: {
                Number n = sourceOperand.numericValue();
                if (n == null) {
                    throw new ResolutionException(sourceOperand.span(), "expected numeric literal");
                }
                int value = n.intValue();
                return new ResolvedOperand(sourceOperand, sourceOperand.kind(), sourceOperand.text(), value);
            }
            case LABEL: {
                String name = sourceOperand.text();
                Integer location = symbolTable.getLocation(name);
                if (location == null) {
                    throw new ResolutionException(sourceOperand.span(), "unknown symbol '" + name + "'");
                }
                return new ResolvedOperand(sourceOperand, sourceOperand.kind(), sourceOperand.text(), location);
            }
            case STRING: {
                return new ResolvedOperand(sourceOperand, sourceOperand.kind(), sourceOperand.text(), null);
            }
            default:
                throw new ResolutionException(sourceOperand.span(), "unsupported operand kind: " + sourceOperand.kind());
        }
    }

    private int applyDirective(int locationCounter, DirectiveStatement directiveStatement) {
        String name = directiveStatement.name().toLowerCase(Locale.ROOT);
        if (".word".equalsIgnoreCase(name)) {
            return locationCounter + 4 * directiveStatement.arguments().size();
        }
        if (".byte".equalsIgnoreCase(name)) {
            return locationCounter + directiveStatement.arguments().size();
        }
        if (".ascii".equalsIgnoreCase(name)) {
            if (directiveStatement.arguments().size() != 1) {
                return locationCounter;
            }
            Operand arg = directiveStatement.arguments().getFirst();
            if (arg.kind() != OperandKind.STRING) {
                return locationCounter;
            }
            byte[] bytes = decodeStringLiteral(arg.text());
            return locationCounter + bytes.length;
        }
        if (".org".equalsIgnoreCase(name)) {
            int address = parseOrgAddress(directiveStatement);
            if (address < 0 || address >= Architecture.MEMORY_SIZE_BYTES) {
                throw new ResolutionException(directiveStatement.span(),
                        ".org address out of range; expected 0.." + (Architecture.MEMORY_SIZE_BYTES - 1));
            }

            return address;
        }

        return locationCounter;
    }

    private int parseOrgAddress(DirectiveStatement directiveStatement) {
        if (directiveStatement.arguments().size() != 1) {
            throw new ResolutionException(directiveStatement.span(), ".org expects exactly one argument");
        }

        var argument = directiveStatement.arguments().getFirst();
        if (!(argument.numericValue() instanceof Number number)) {
            throw new ResolutionException(argument.span(), ".org requires a numeric argument");
        }

        return number.intValue();
    }

    public record Pass1Result(SymbolTable symbolTable, Map<Statement, Integer> statementAddresses) {
    }

    public record Pass2Result(List<ResolvedStatement> resolvedStatements) {
    }
}
