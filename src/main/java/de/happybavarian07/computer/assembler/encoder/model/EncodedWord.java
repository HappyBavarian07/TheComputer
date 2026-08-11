package de.happybavarian07.computer.assembler.encoder.model;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 20:11
 */
public record EncodedWord(int byteAddress, int rawWord) implements Comparable<EncodedWord> {
    @Override
    public int compareTo(EncodedWord other) {
        return Integer.compare(byteAddress, other.byteAddress);
    }
}
