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
        if(byteSink == null || byteSink.addressToValue().isEmpty()) return new ArrayList<>();
        Map<Integer, Byte[]> groupedBytes = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : byteSink.addressToValue().entrySet()) {
            int base = entry.getKey() & ~0x3;
            int lane = entry.getKey() - base;
            groupedBytes.putIfAbsent(base, new Byte[]{
                    new Byte(0), new Byte(0), new Byte(0), new Byte(0)
            });
            groupedBytes.get(base)[lane] = new Byte(entry.getValue() & 0xFF);
        }

        List<EncodedWord> result = new ArrayList<>();
        for (int base : groupedBytes.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
            Byte[] lanes = groupedBytes.get(base);
            int raw = lanes[0].getAsInt() |
                    (lanes[1].getAsInt() << 8) |
                    (lanes[2].getAsInt() << 16) |
                    (lanes[3].getAsInt() << 24);
            result.add(new EncodedWord(base, raw));
        }

        return result;
    }
}
