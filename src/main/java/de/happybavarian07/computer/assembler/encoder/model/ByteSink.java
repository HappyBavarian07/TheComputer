package de.happybavarian07.computer.assembler.encoder.model;

import java.util.Map;
import java.util.Objects;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 20:16
 */
public record ByteSink(Map<Integer, Integer> addressToValue) {

    public boolean containsAddress(int addr) {
        return addressToValue().containsKey(addr);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ByteSink) obj;
        return Objects.equals(this.addressToValue, that.addressToValue);
    }

    @Override
    public String toString() {
        return "ByteSink[" +
                "addressToValue=" + addressToValue + ']';
    }
}
