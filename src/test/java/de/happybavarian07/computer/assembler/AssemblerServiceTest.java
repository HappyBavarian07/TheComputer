package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import de.happybavarian07.computer.exceptions.assembler.ResolutionException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblerServiceTest {
    private final AssemblerService service = new AssemblerService();

    private byte[] image(String source) {
        return service.assemble(source, "test.asm").image();
    }

    @Test
    void dataDirectivesAreBytePreciseAndInstructionsFollowAligned() {
        byte[] image = image(".org 0x100\n.word 0x11223344\n.word 0x55667788\nhalt\n");
        assertEquals(0x110, image.length);
        assertEquals(0x44, image[0x100] & 0xFF);
        assertEquals(0x11, image[0x103] & 0xFF);
        assertEquals(0x88, image[0x104] & 0xFF);
        assertEquals(0x55, image[0x107] & 0xFF);
        assertEquals(0x03, image[0x10F] & 0xFF); // halt opcode in the top byte
    }

    @Test
    void byteAndAsciiNeedNoAlignmentThenAlignPadsToInstructionBoundary() {
        byte[] image = image(".byte 1, 2, 3\n.ascii \"ab\"\n.align 8\nhalt\n");
        assertEquals(1, image[0]);
        assertEquals(3, image[2]);
        assertEquals('a', image[3]);
        assertEquals('b', image[4]);
        assertEquals(0x03, image[15] & 0xFF);
        assertEquals(16, image.length);
    }

    @Test
    void instructionAtUnalignedAddressIsResolveError() {
        ResolutionException ex = assertThrows(ResolutionException.class, () -> image(".byte 1\nhalt\n"));
        assertTrue(ex.getMessage().contains("not 8-byte aligned"), ex.getMessage());
    }

    @Test
    void alignRejectsNonPowerOfTwo() {
        assertThrows(ResolutionException.class, () -> image(".align 3\nhalt\n"));
    }

    @Test
    void overlappingOutputIsAnError() {
        assertThrows(EncodingException.class, () -> image("halt\n.org 4\n.word 1\n"));
    }

    @Test
    void wordOutsideThirtyTwoBitRangeIsAnError() {
        assertThrows(EncodingException.class, () -> image(".word 4294967296\n"));
    }

    @Test
    void cliWritesTheSameBytesAsTheFacade(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        String source = ".org 0x100\n.word 1\n.word 2\nhalt\n";
        Path asm = dir.resolve("p.asm");
        Path bin = dir.resolve("p.bin");
        Files.writeString(asm, source);

        int exit = new de.happybavarian07.computer.assembler.cli.AssemblerCli().handleCommandInput(new String[]{asm.toString(), "-o", bin.toString()});

        assertEquals(0, exit);
        assertArrayEquals(image(source), Files.readAllBytes(bin));
        assertTrue(new File(bin.toString()).length() > 0);
    }
}
