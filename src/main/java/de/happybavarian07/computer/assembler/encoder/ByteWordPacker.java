package de.happybavarian07.computer.assembler.encoder;

import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import de.happybavarian07.computer.core.byteclass.Byte;

import java.util.*;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 21:46
 */
public class ByteWordPacker {
    public List<EncodedWord> packBytesToWords(ByteSink byteSink) {
        if (byteSink == null || byteSink.addressToValue().isEmpty()) return new ArrayList<>();
        Map<Integer, int[]> grouped = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : byteSink.addressToValue().entrySet()) {
            int base = entry.getKey() & ~0x3;
            int lane = entry.getKey() - base;
            grouped.computeIfAbsent(base, k -> new int[]{0, 0, 0, 0})[lane] = entry.getValue() & 0xFF;
        }

        List<EncodedWord> result = new ArrayList<>();
        List<Integer> bases = new ArrayList<>(grouped.keySet());
        Collections.sort(bases);
        for (int base : bases) {
            int[] lanes = grouped.get(base);
            int raw = (lanes[0]) | (lanes[1] << 8) | (lanes[2] << 16) | (lanes[3] << 24);
            result.add(new EncodedWord(base, raw));
        }

        return result;
    }
}
