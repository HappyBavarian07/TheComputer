package de.happybavarian07.computer.assembler;

import de.happybavarian07.computer.assembler.encoder.DirectiveDataEmitter;
import de.happybavarian07.computer.assembler.encoder.model.ByteSink;
import de.happybavarian07.computer.assembler.parser.model.Operand;
import de.happybavarian07.computer.assembler.parser.model.OperandKind;
import de.happybavarian07.computer.assembler.parser.model.SourceSpan;
import de.happybavarian07.computer.assembler.parser.model.statement.DirectiveStatement;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedOperand;
import de.happybavarian07.computer.assembler.resolver.model.ResolvedStatement;
import de.happybavarian07.computer.exceptions.assembler.EncodingException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DirectiveDataEmitterTest {

    @Test
    void unaligned_word_throws() {
        DirectiveDataEmitter emitter = new DirectiveDataEmitter();
        DirectiveStatement stmt = new DirectiveStatement(".word", List.of(new Operand(OperandKind.NUMBER, "1", 1, new SourceSpan("file",1,1,1,2))), new SourceSpan("file",1,1,1,6));
        ResolvedOperand ro = new ResolvedOperand(stmt.arguments().get(0), OperandKind.NUMBER, "1", 1);
        ResolvedStatement rs = new ResolvedStatement(stmt, 1, List.of(ro)); // base address 1 (unaligned)

        ByteSink sink = new ByteSink(new HashMap<>());
        assertThrows(EncodingException.class, () -> emitter.emit(rs, sink));
    }

    @Test
    void ascii_operands_written_to_sink() {
        DirectiveDataEmitter emitter = new DirectiveDataEmitter();
        DirectiveStatement stmt = new DirectiveStatement(".ascii", List.of(), new SourceSpan("file",1,1,1,6));
        // simulate resolved ascii bytes as operands
        Operand dummy = new Operand(OperandKind.NUMBER, "65", 65, new SourceSpan("file",1,1,1,7));
        ResolvedOperand r1 = new ResolvedOperand(dummy, OperandKind.NUMBER, "65", 65);
        ResolvedOperand r2 = new ResolvedOperand(dummy, OperandKind.NUMBER, "10", 10);
        ResolvedOperand r3 = new ResolvedOperand(dummy, OperandKind.NUMBER, "0", 0);

        ResolvedStatement rs = new ResolvedStatement(stmt, 0, List.of(r1, r2, r3));
        Map<Integer,Integer> map = new HashMap<>();
        ByteSink sink = new ByteSink(map);

        emitter.emit(rs, sink);

        assertEquals(3, sink.addressToValue().size());
        assertEquals(65, sink.addressToValue().get(0));
        assertEquals(10, sink.addressToValue().get(1));
        assertEquals(0, sink.addressToValue().get(2));
    }
}
