package de.happybavarian07.computer.cpu.registers;

import de.happybavarian07.computer.core.word.Word;
import de.happybavarian07.computer.util.Architecture;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 20:49
 */
public class RegisterFile {
    private final Word[] registers;

    public RegisterFile() {
        registers = new Word[Architecture.GPR_COUNT];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = new Word(0);
        }
    }

    public void read(int regIndex, Word destination) {
        if(regIndex < 0 || regIndex > Architecture.GPR_COUNT - 1) throw new IllegalArgumentException("Tried to access non existent Register");

        destination.set(registers[regIndex].getAsArray());
    }

    public void write(int regIndex, Word source) {
        if(regIndex < 0 || regIndex > Architecture.GPR_COUNT - 1) throw new IllegalArgumentException("Tried to write non existent Register");

        registers[regIndex].set(source.getAsArray());
    }

    public void reset() {
        for(Word reg : registers) {
            reg.set(0);
        }
    }
    
    public void dump() {
        for (int i = 0; i < Architecture.GPR_COUNT; i++) {
            System.out.println("Reg " + i + ": " + registers[i].getAsInt() + " (" + registers[i].getAsString() + ")");
        }
    }
}
