package de.happybavarian07.computer.exceptions.isa;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 16:08
 */
public class IllegalInstructionException extends RuntimeException {
    public IllegalInstructionException(String message) {
        super(message);
    }
}
