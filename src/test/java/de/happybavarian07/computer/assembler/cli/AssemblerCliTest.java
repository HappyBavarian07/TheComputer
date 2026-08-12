package de.happybavarian07.computer.assembler.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblerCliTest {

    @Test
    void parseArgsAcceptsShortAndLongFlags() {
        AssemblerCli cli = new AssemblerCli();

        AssemblerCli.ParsedCommand cmd = cli.parseArgs(new String[]{
                "-d",
                "-v",
                "-o",
                "build/out.bin",
                "program.asm"
        });

        assertEquals(new File("program.asm"), cmd.inputFile());
        assertEquals(new File("build/out.bin"), cmd.outputFile());
        assertTrue(cmd.dryRun());
        assertTrue(cmd.verbose());
        assertFalse(cmd.help());
        assertNull(cmd.error());
    }

    @Test
    void parseArgsRejectsUnknownOption() {
        AssemblerCli cli = new AssemblerCli();

        AssemblerCli.ParsedCommand cmd = cli.parseArgs(new String[]{"--nope", "program.asm"});

        assertNotNull(cmd.error());
        assertTrue(cmd.error().contains("unknown option"));
    }

    @Test
    void handleCommandInputReportsMissingInputFile() throws Exception {
        AssemblerCli cli = new AssemblerCli();
        Method method = AssemblerCli.class.getDeclaredMethod("handleCommandInput", String[].class);
        method.setAccessible(true);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));

        try {
            int exitCode = (Integer) method.invoke(cli, (Object) new String[]{"missing.asm"});
            assertEquals(1, exitCode);
            String output = err.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("input file doesn't exist"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void handleCommandInputDryRunPrintsEncodedWords() throws Exception {
        Path tempDir = Files.createTempDirectory("assembler-cli-");
        Path input = tempDir.resolve("program.asm");
        Files.writeString(input, "nop\nhalt");

        AssemblerCli cli = new AssemblerCli();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        try {
            int exitCode = cli.handleCommandInput(new String[]{input.toString(), "--dry-run", "--verbose"});
            assertEquals(0, exitCode);
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Encoded words:"));
            assertTrue(output.contains("Address"));
        } finally {
            System.setOut(originalOut);
        }
        System.out.println("Exit Code: " + cli.handleCommandInput(new String[]{input.toString(), "--dry-run", "--verbose"}));
    }
}
