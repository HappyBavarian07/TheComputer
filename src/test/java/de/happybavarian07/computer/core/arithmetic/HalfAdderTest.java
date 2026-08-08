package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 16:40
 */
class HalfAdderTest {
    private Bit inA;
    private Bit inB;
    private Bit outSum;
    private Bit outCarry;
    private HalfAdder halfAdder;

    @BeforeEach
    void setUp() {
        reset();
        this.halfAdder = new HalfAdder();
    }

    public void reset() {
        inA = new Bit(false);
        inB = new Bit(false);
        outSum = new Bit(false);
        outCarry = new Bit(false);
    }

    /**
     * <table border="1">
     * <tr>
     * <td>A</td> <td>B</td> <td>| Sum</td> <td>Carry</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>false</td> <td>| false</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>true</td> <td>| true</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>false</td> <td>| true</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>true</td> <td>| false</td> <td>true</td>
     * </tr>
     * </table>
     */
    @Test
    void execute() {
        // 1. Test
        runTest(false, false, false ,false);

        // 2. Test
        runTest(false, true, true, false);

        // 3. Test
        runTest(true, false, true, false);

        // 4. Test
        runTest(true, true, false, true);
    }

    private void runTest(boolean inABool, boolean inBBool, boolean outSumCorrect, boolean outCarryCorrect) {
        inA.set(inABool);
        inB.set(inBBool);
        halfAdder.execute(inA, inB, outSum, outCarry);
        assertFields(inABool, inBBool, outSumCorrect, outCarryCorrect);
        reset();
    }

    public void assertFields(boolean inACorrect, boolean inBCorrect, boolean outSumCorrect, boolean outCarryCorrect) {
        assertEquals(inA.getAsBool(), inACorrect, "inA is wrong.");
        assertEquals(inB.getAsBool(), inBCorrect, "inB is wrong.");
        assertEquals(outSum.getAsBool(), outSumCorrect, "Sum is wrong.");
        assertEquals(outCarry.getAsBool(), outCarryCorrect, "Carry is wrong.");
    }
}