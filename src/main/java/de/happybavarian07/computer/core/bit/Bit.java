package de.happybavarian07.computer.core.bit;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 12:53
 */
public class Bit {
    private boolean value;

    public Bit(boolean value) {
        this.value = value;
    }

    public Bit(byte value) {
        set(value);
    }

    public boolean getAsBool() {
        return value;
    }

    public byte getAsByte() {
        return (byte) (value ? 1 : 0);
    }

    public String getAsString() {
        return String.valueOf(getAsByte());
    }

    public void set(boolean value) {
        this.value = value;
    }

    public void set(byte value) {
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("A bit can only be 0 or 1.");
        }

        this.value = value == 1;
    }

    @Override
    public String toString() {
        return getAsString();
    }
}
