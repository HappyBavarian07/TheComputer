package de.happybavarian07.computer.exceptions.stack;

/*
 * @Author HappyBavarian07
 * @Date August 10, 2026 | 22:39
 */
public class StackOverflowException extends RuntimeException {
    public StackOverflowException(String message) {
        super(message);
    }
}
