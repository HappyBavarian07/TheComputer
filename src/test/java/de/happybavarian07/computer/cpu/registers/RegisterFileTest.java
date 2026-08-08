package de.happybavarian07.computer.cpu.registers;

import de.happybavarian07.computer.core.bit.Bit;
import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.cpu.alu.Alu;
import de.happybavarian07.computer.cpu.alu.AluOp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 09, 2026 | 00:08
 */
public class RegisterFileTest {
    private RegisterFile registerFile;
    private Alu alu;
    private Word destinationToReadToo;
    private Word sourceToWriteFrom;
    private Word inA;
    private Word inB;
    private Word outResult;
    private Bit flagZ, flagN, flagC, flagV;

    @BeforeEach
    void setUp() {
        registerFile = new RegisterFile();
        destinationToReadToo = new Word(0);
        sourceToWriteFrom = new Word(0);
        alu = new Alu();
        inA = new Word(0);
        inB = new Word(0);
        outResult = new Word(0);
        flagZ = new Bit(false);
        flagN = new Bit(false);
        flagC = new Bit(false);
        flagV = new Bit(false);
    }

    @Test
    void testReadWrite() {
        Random random = new Random();

        for (int i = 0; i < 16; i++) {
            System.out.println("Running test for Register " + i + ":");
            for (int j = 0; j < 50; j++) { // (randomInt, regRead, true/false)
                int randomInt = random.nextInt(50000);
                sourceToWriteFrom.set(randomInt);
                registerFile.write(i, sourceToWriteFrom);
                registerFile.read(i, destinationToReadToo);
                assertEquals(randomInt, destinationToReadToo.getAsInt());
                System.out.print("(" + randomInt + "," + destinationToReadToo.getAsInt() + "," + (randomInt == destinationToReadToo.getAsInt()) + "), ");
                if(j % 10 == 0 && j != 0) System.out.println();
            }
            System.out.println();
        }
    }

    @Test
    void testRegisterWithAlu() {
        // Calculate: (A+B)*(A-B)
        // 1. Store A,B in Reg0 and Reg1
        // 2. Read from Register
        // 3. Call Alu on (A+B), Store in Reg2
        // 4. Call Alu on (A-B), Store in Reg3
        // 5. Loop that counts down and adds (A+B), (A-B) times
        registerFile.reset();
        int valA = 15;
        int valB = 2;
        registerFile.write(0, new Word(valA));
        registerFile.write(1, new Word(valB));
        registerFile.read(0, inA);
        registerFile.read(1, inB);
        alu.execute(inA, inB, AluOp.ADD, outResult, flagZ, flagN, flagC, flagV);
        registerFile.write(2, outResult); // (A+B) term to add <counter>-times
        alu.execute(inA, inB, AluOp.SUB, outResult, flagZ, flagN, flagC, flagV);
        registerFile.write(3, outResult); // counter (A-B)
        Word counter = new Word(0);
        Word term = new Word(0);
        Word accum = new Word(0);
        registerFile.read(2, term);
        registerFile.read(3, counter);
        assertEquals(valA - valB, counter.getAsInt(), "Counter mismatch.");
        while(counter.getAsInt() > 0) { // technically we would normally work with flags here but its not fully implemented yet as far as i know.
            // accum = accum + term
            alu.execute(accum, term, AluOp.ADD, accum, flagZ, flagN, flagC, flagV);
            // counter = counter - 1
            alu.execute(counter, new Word(1), AluOp.SUB, counter, flagZ, flagN, flagC, flagV);
            registerFile.write(3, counter);
        }
        registerFile.write(4, accum);
        registerFile.dump();

        int expected = (valA + valB) * (valA - valB);
        assertEquals(expected, accum.getAsInt(), "Output doesnt match.");
        System.out.println("Final Output: " + accum.getAsInt());
    }

    @Test
    void testReset() {
        for (int i = 0; i < 16; i++) {
            sourceToWriteFrom.set(12345 + i);
            registerFile.write(i, sourceToWriteFrom);
        }

        registerFile.reset();

        for (int i = 0; i < 16; i++) {
            registerFile.read(i, destinationToReadToo);
            assertEquals(0, destinationToReadToo.getAsInt(), "Register " + i + " was not reset to 0.");
        }
    }
}
