package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.core.address.Address;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.Cpu;
import de.happybavarian07.computer.util.Architecture;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledProgramsTest {
    @Test
    void everyBundledProgramAssemblesAndRunsToHaltWithoutFault() throws Exception {
        File[] programs = new File("src/main/resources/programs").listFiles((dir, name) -> name.endsWith(".asm"));
        assertTrue(programs != null && programs.length >= 5, "bundled programs not found");

        for (File program : programs) {
            byte[] image = new AssemblerService().assemble(Files.readString(program.toPath()), program.getName()).image();
            Cpu cpu = new Cpu();
            Word word = new Word();
            Address address = new Address();
            for (int offset = 0; offset < image.length; offset += Architecture.INSTRUCTION_BYTES) {
                long raw = 0;
                for (int i = 0; i < Architecture.INSTRUCTION_BYTES; i++) {
                    raw |= ((long) (image[offset + i] & 0xFF)) << (i * 8);
                }
                address.set(offset);
                word.set(raw);
                cpu.getSystemBus().writeWord(address, word);
            }
            int steps = 0;
            while (!cpu.isHalted() && steps++ < 1_000_000) {
                cpu.step();
            }
            assertTrue(cpu.isHalted(), program.getName() + " did not halt");
            assertFalse(cpu.isFaulted(), program.getName() + " faulted: " + cpu.getFaultReason());
        }
    }
}
