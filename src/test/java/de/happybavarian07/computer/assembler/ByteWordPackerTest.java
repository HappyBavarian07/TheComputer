package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.ByteWordPacker;
import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.encoder.model.EncodedWord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ByteWordPackerTest {

    @Test
    void pack_empty_sink_returns_empty() {
        ByteWordPacker packer = new ByteWordPacker();
        ByteSink sink = new ByteSink(new HashMap<>());
        List<EncodedWord> words = packer.packBytesToWords(sink);
        assertTrue(words.isEmpty());
    }

    @Test
    void pack_cross_boundary_bytes() {
        // addresses: 2,3,4,5,6 -> groups at base 0 and base 4
        Map<Integer, Integer> map = new HashMap<>();
        map.put(2, 1); // base 0, lane 2
        map.put(3, 2); // base 0, lane 3
        map.put(4, 3); // base 4, lane 0
        map.put(5, 4); // base 4, lane 1
        map.put(6, 5); // base 4, lane 2
        ByteSink sink = new ByteSink(map);

        ByteWordPacker packer = new ByteWordPacker();
        List<EncodedWord> words = packer.packBytesToWords(sink);

        // expect two words at base 0 and 4
        assertEquals(2, words.size());
        EncodedWord w0 = words.get(0);
        EncodedWord w4 = words.get(1);

        assertEquals(0, w0.byteAddress());
        int expected0 = (0) | (0 << 8) | (1 << 16) | (2 << 24);
        assertEquals(expected0, w0.rawWord());

        assertEquals(4, w4.byteAddress());
        int expected4 = (3) | (4 << 8) | (5 << 16) | (0 << 24);
        assertEquals(expected4, w4.rawWord());
    }
}
