package de.happybavarian07.computer.exceptions.stack;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 22:42
 */
public class StackUnderflowException extends RuntimeException {
    public StackUnderflowException(String message) {
        super(message);
    }
}
