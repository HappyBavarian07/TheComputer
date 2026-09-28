package de.happybavarian07.computer.gui.controller;

import de.happybavarian07.computer.assembler.cli.AssemblerCli;
import de.happybavarian07.computer.core.word.Word;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchControllerBinaryTest {
    private static final String MATH = "movi r1, 42\nmovi r2, 8\nadd r3, r1, r2\nhalt\n";

    @Test
    void exportMatchesAssemblerCliForPlainPrograms() throws Exception {
        Path dir = Files.createTempDirectory("wb-binary-test");
        Path asm = dir.resolve("p.asm");
        Path cliBin = dir.resolve("p.bin");
        Files.writeString(asm, MATH);
        assertEquals(0, new AssemblerCli().handleCommandInput(new String[]{asm.toString(), "-o", cliBin.toString(), "-O"}));

        WorkbenchController.BinaryResult result = new WorkbenchController().assembleToBinary(MATH);

        assertTrue(result.success());
        assertArrayEquals(Files.readAllBytes(cliBin), result.image());
    }

    @Test
    void exportKeepsDirectiveDataAtAddressesNotDivisibleByEight() {
        byte[] image = new WorkbenchController()
                .assembleToBinary(".org 0x100\n.word 0x11223344\n.word 0x55667788\nhalt\n").image();

        assertEquals(0x110, image.length);
        assertArrayEquals(new byte[]{0x44, 0x33, 0x22, 0x11, (byte) 0x88, 0x77, 0x66, 0x55}, java.util.Arrays.copyOfRange(image, 0x100, 0x108));
        assertEquals(0x03, image[0x10F], "halt's opcode is the top byte of its little-endian word");
    }

    @Test
    void exportedBinaryRoundTripsThroughLoadBinaryAndRuns() throws Exception {
        Path bin = Files.createTempFile("wb-roundtrip", ".bin");
        Files.write(bin, new WorkbenchController().assembleToBinary(MATH).image());

        WorkbenchController fresh = new WorkbenchController();
        fresh.loadBinary(bin);
        fresh.getMotherboard().getCpu().run();

        Word r3 = new Word();
        fresh.getMotherboard().getCpu().getRegisterFile().read(3, r3);
        assertEquals(50, r3.getAsLong());
    }

    @Test
    void invalidOrBlankSourceYieldsNoImage() {
        WorkbenchController controller = new WorkbenchController();

        WorkbenchController.BinaryResult bad = controller.assembleToBinary("jnz nowhere\n");
        assertFalse(bad.success());
        assertNull(bad.image());
        assertTrue(bad.message().contains("unknown opcode"));

        assertFalse(controller.assembleToBinary("   ").success());
    }
}
