package de.happybavarian07.computer.core.bit;

import java.util.Arrays;

/**
 * Represents a fixed-width sequence of bits.
 *
 * @author HappyBavarian07
 * @since August 08, 2026
 */
public class FixedWidthBits {

    private final int size;
    private final Bit[] bitArray;

    public FixedWidthBits(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0.");
        }

        this.size = size;
        this.bitArray = new Bit[size];
        for (int i = 0; i < size; i++) {
            this.bitArray[i] = new Bit(false);
        }
    }

    public FixedWidthBits(int size, Bit[] bitArray) {
        this(size);
        set(bitArray);
    }

    public FixedWidthBits(int size, Number number) {
        this(size);
        set(number);
    }

    public FixedWidthBits(FixedWidthBits other) {
        if (other == null) {
            throw new IllegalArgumentException("Source cannot be null.");
        }

        this.size = other.size;
        this.bitArray = new Bit[size];
        for (int i = 0; i < size; i++) {
            this.bitArray[i] = new Bit(other.bitArray[i].getAsBool());
        }
    }

    public int size() {
        return size;
    }

    public Bit[] getAsArray() {
        return Arrays.copyOf(bitArray, size);
    }

    public String getAsString() {
        StringBuilder builder = new StringBuilder(size);

        for (Bit bit : bitArray) {
            builder.append(bit);
        }

        return builder.toString();
    }

    public long getAsLong() {
        if (size > Long.SIZE) {
            throw new ArithmeticException(
                    "Cannot represent " + size + " bits as a long."
            );
        }

        return Long.parseLong(getAsString(), 2);
    }

    public int getAsInt() {
        return (int) getAsLong();
    }

    public Bit get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Bit index out of bounds: " + index
            );
        }

        return bitArray[index];
    }

    public void set(int index, Bit bit) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Bit index out of bounds: " + index
            );
        }

        if (bit == null) {
            throw new IllegalArgumentException("Bit cannot be null.");
        }

        bitArray[index].set(bit.getAsBool());
    }

    public void set(int index, boolean bit) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Bit index out of bounds: " + index
            );
        }

        bitArray[index].set(bit);
    }

    public void set(Bit[] bitArray) {
        if (bitArray == null) {
            throw new IllegalArgumentException("Bit array cannot be null.");
        }

        if (bitArray.length != size) {
            throw new IllegalArgumentException(
                    "Expected " + size + " bits, got " + bitArray.length + "."
            );
        }

        for (int i = 0; i < size; i++) {
            this.bitArray[i].set(bitArray[i].getAsBool());
        }
    }

    public void set(String stringInput) {
        if (stringInput == null) {
            throw new IllegalArgumentException("Input cannot be null.");
        }

        if (stringInput.length() != size) {
            throw new IllegalArgumentException(
                    "Expected exactly " + size + " bits, got "
                            + stringInput.length() + "."
            );
        }

        for (int i = 0; i < size; i++) {
            char c = stringInput.charAt(i);
            this.bitArray[i].set(switch (c) {
                case '0' -> false;
                case '1' -> true;
                default -> throw new IllegalArgumentException("Invalid bit: " + c);
            });
        }
    }

    public void set(Number numberInput) {
        if (numberInput == null) {
            throw new IllegalArgumentException("Number input cannot be null.");
        }

        long val = numberInput.longValue();
        for (int i = 0; i < size; i++) {
            boolean bitVal = ((val >> i) & 1L) == 1L;
            this.bitArray[size - 1 - i].set(bitVal);
        }
    }

    @Override
    public String toString() {
        return getAsString();
    }
}