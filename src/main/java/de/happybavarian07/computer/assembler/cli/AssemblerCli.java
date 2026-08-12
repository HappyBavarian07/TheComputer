package de.happybavarian07.computer.assembler.cli;

import de.happybavarian07.computer.assembler.encoder.AssemblerEncoder;
import de.happybavarian07.computer.assembler.encoder.model.EncodedProgram;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.assembler.lexer.Lexer;
import de.happybavarian07.computer.assembler.lexer.impl.IndexedLexer;
import de.happybavarian07.computer.assembler.parser.DefaultParser;
import de.happybavarian07.computer.assembler.parser.Parser;
import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.resolver.SymbolResolver;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedProgram;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

/*
 * @Author HappyBavarian07
 * @Date August 12, 2026 | 13:07
 */
public final class AssemblerCli {
    private Lexer lexer;
    private Parser parser;
    private SymbolResolver symbolResolver;
    private AssemblerEncoder encoder;

    public AssemblerCli() {
        lexer = new IndexedLexer();
        parser = new DefaultParser(lexer);
        symbolResolver = new SymbolResolver();
        encoder = new AssemblerEncoder();
    }

    public ParsedCommand parseArgs(String[] args) {
        // args structure: assemble, [options] (can be multiple args) and must follow pattern --[A-Za-z_-] or -A-Za-z (single letter), input file (without - or --), [-o outputfile] (can also be anywhere or nowhere)
        File inputFile = null;
        File outputFile = null;
        boolean dryRun = false, verbose = false, overwrite = false, help = false;
        String error = null;

        int i = 0;
        String token = "";

        while (i < args.length) {
            token = args[i];

            switch (token) {
                case "-h", "--help" -> {
                    help = true;
                    i += 1;
                    continue;
                }
                case "-d", "--dry-run" -> {
                    dryRun = true;
                    i += 1;
                    continue;
                }
                case "-v", "--verbose" -> {
                    verbose = true;
                    i += 1;
                    continue;
                }
                case "-O", "--overwrite" -> {
                    overwrite = true;
                    i += 1;
                    continue;
                }
                case "-o", "--output" -> {
                    if ((i + 1) > args.length) {
                        error = "missing value for output option";
                        break;
                    }
                    outputFile = new File(args[i + 1]);
                    i += 2;
                    continue;
                }
            }

            if (token.startsWith("--output=")) {
                outputFile = new File(token.substring(8));
                i += 1;
                continue;
            }
            if (token.startsWith("-")) {
                error = "unknown option: " + token;
                break;
            }
            if (inputFile != null) {
                error = "too many positional arguments";
                break;
            }
            inputFile = new File(token);
            i += 1;
        }

        return new ParsedCommand(inputFile, outputFile, dryRun, verbose, overwrite, help, error);
    }

    private int handleCommandInput(String[] args) {
        ParsedCommand cmd = parseArgs(args);
        File finalOutputFile = cmd.outputFile();

        if (cmd.help()) {
            printUsage();
            return 0;
        }
        if (cmd.error() != null) {
            System.err.println(cmd.error());
            printUsage();
            return 1;
        }
        if (cmd.inputFile() == null || !cmd.inputFile().exists()) {
            System.err.println("input file doesn't exist");
            return 1;
        }
        if (finalOutputFile == null) {
            finalOutputFile = replaceSuffix(cmd.inputFile, ".asm", ".bin");
        }
        if (finalOutputFile.exists() && !cmd.overwrite()) {
            System.err.println("output file already exists");
            return 1;
        }

        Path absolute = cmd.inputFile().toPath().toAbsolutePath().normalize();
        String source = "";
        try {
            source = Files.readString(absolute);
        } catch (IOException e) {
            System.err.println("error reading input file: " + e + ": " + e.getLocalizedMessage());
            return 2;
        }
        parser.reset(source, absolute.toString());
        try {
            Program program = parser.parse();
            ResolvedProgram resolvedProgram = symbolResolver.resolve(program);
            EncodedProgram encodedProgram = encoder.encodeProgram(resolvedProgram);

            if (cmd.verbose()) {
                printVerboseOutput(source, program, resolvedProgram);
            }
            if (cmd.dryRun()) {
                printEncodedWords(encodedProgram.words());
                return 0;
            }
            return writeBinary(encodedProgram, finalOutputFile);
        } catch (ResolutionException | EncodingException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }

    private void printEncodedWords(List<EncodedWord> words) {
        System.out.println("Encoded words:");
        System.out.printf("%-12s %-12s %-12s%n", "Address", "Word", "Hex");
        for (EncodedWord w : words) {
            System.out.printf("%-12d %-12d 0x%08X%n",
                    w.byteAddress(),
                    w.rawWord(),
                    Integer.toUnsignedLong(w.rawWord()));
        }
    }

    private void printVerboseOutput(String source, Program program, ResolvedProgram resolvedProgram) {
        printTokenDump(source, program);
        printResolvedDump(resolvedProgram);
    }

    private void printTokenDump(String source, Program program) {
        System.out.println("Tokens:");
        System.out.printf("%-6s %-18s %-20s%n", "Line", "Kind", "Lexeme");
        var lexer = new IndexedLexer();
        lexer.reset(source, program.sourcePath());
        var tokens = lexer.tokenizeAll();
        for (var token : tokens) {
            System.out.printf("%-6d %-18s %-20s%n",
                    token.startLine(),
                    token.tokenKind(),
                    token.lexeme());
        }
    }

    private void printResolvedDump(ResolvedProgram resolvedProgram) {
        System.out.println("Resolved statements:");
        System.out.printf("%-8s %-16s %-20s%n", "Address", "Source", "Operands");
        for (var statement : resolvedProgram.statements()) {
            String sourceName = statement.sourceStatement().getClass().getSimpleName();
            String operands = statement.operands().stream()
                    .map(op -> op.text() + "=" + op.resolvedNumericValue())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            System.out.printf("%-8d %-16s %-20s%n",
                    statement.address(),
                    sourceName,
                    operands);
        }
    }

    private int writeBinary(EncodedProgram encodedProgram, File finalOutputFile) {
        if (!finalOutputFile.exists()) {
            finalOutputFile.getParentFile().mkdirs();
            try {
                finalOutputFile.createNewFile();
            } catch (IOException e) {
                System.err.println("error creating final output file: " + finalOutputFile.getAbsolutePath());
                return 2;
            }
        }

        List<EncodedWord> words = encodedProgram.words();
        words.sort(Comparator.naturalOrder());

        int maxAddress = 0;

        for (EncodedWord word : words) {
            maxAddress = Math.max(maxAddress, word.byteAddress() + 4);
        }

        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            int currentAddress = 0;
            int wordIndex = 0;

            while (currentAddress < maxAddress) {
                if (wordIndex < words.size() && words.get(wordIndex).byteAddress() == currentAddress) {
                    int raw = words.get(wordIndex).rawWord();
                    byte byte0 = (byte) (raw & 0xFF);
                    byte byte1 = (byte) ((raw >>> 8) & 0xFF);
                    byte byte2 = (byte) ((raw >>> 16) & 0xFF);
                    byte byte3 = (byte) ((raw >>> 24) & 0xFF);
                    byteBuffer.write(new byte[]{byte0, byte1, byte2, byte3});
                    wordIndex += 1;
                } else {
                    byteBuffer.write(new byte[]{0x00, 0x00, 0x00, 0x00});
                }
                currentAddress += 4;
            }
            byteBuffer.writeTo(Files.newOutputStream(finalOutputFile.toPath()));
        } catch (IOException e) {
            System.err.println("error writing bytes to buffer: " + e + ": " + e.getLocalizedMessage());
            return 2;
        }
        return 0;
    }

    private File replaceSuffix(File inputFile, String suffix, String replacement) {
        String newName = inputFile.getName().replace(suffix, replacement);
        return new File(inputFile.getParent(), newName);
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  assemble <input.asm> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output <file>     write output to <file>");
        System.out.println("  -d, --dry-run           parse and resolve without writing output");
        System.out.println("  -v, --verbose           print parser, resolver, and encoder diagnostics");
        System.out.println("  -O, --overwrite         overwrite an existing output file");
        System.out.println("  -h, --help              show this help menu");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  assemble program.asm");
        System.out.println("  assemble program.asm -o build/program.bin");
        System.out.println("  assemble program.asm --dry-run --verbose");
    }

    public record ParsedCommand(File inputFile, File outputFile, boolean dryRun, boolean verbose, boolean overwrite,
                                boolean help, String error) {
    }
}
