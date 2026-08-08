package de.happybavarian07.computer.core.arithmetic;

import de.happybavarian07.computer.core.bit.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @Author HappyBavarian07
 * @Date August 08, 2026 | 16:38
 */
class FullAdderTest {
    private Bit inA;
    private Bit inB;
    private Bit inCarry;
    private Bit outSum;
    private Bit outCarry;
    private FullAdder fullAdder;

    @BeforeEach
    void setUp() {
        reset();
        this.fullAdder = new FullAdder();
    }

    public void reset() {
        inA = new Bit(false);
        inB = new Bit(false);
        inCarry = new Bit(false);
        outSum = new Bit(false);
        outCarry = new Bit(false);
    }

    /**
     * <table border="1">
     * <tr>
     * <td>A</td> <td>B</td> <td>CarryIn</td> <td>| Sum</td> <td>CarryOut</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>false</td> <td>false</td> <td>| false</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>false</td> <td>true</td> <td>| true</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>true</td> <td>false</td> <td>| true</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>false</td> <td>true</td> <td>true</td> <td>| false</td> <td>true</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>false</td> <td>false</td> <td>| true</td> <td>false</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>false</td> <td>true</td> <td>| false</td> <td>true</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>true</td> <td>false</td> <td>| false</td> <td>true</td>
     * </tr>
     * <tr>
     * <td>true</td> <td>true</td> <td>true</td> <td>| true</td> <td>true</td>
     * </tr>
     * </table>
     */
    @Test
    void execute() {
        // 1. Test
        runTest(false, false, false, false, false);

        // 2. Test
        runTest(false, false, true, true, false);

        // 3. Test
        runTest(false, true, false, true, false);

        // 4. Test
        runTest(false, true, true, false, true);

        // 5. Test
        runTest(true, false, false, true, false);

        // 6. Test
        runTest(true, false, true, false, true);

        // 7. Test
        runTest(true, true, false, false, true);

        // 8. Test
        runTest(true, true, true, true, true);
    }

    private void runTest(boolean inABool, boolean inBBool, boolean inCarryBool, boolean outSumCorrect, boolean outCarryCorrect) {
        inA.set(inABool);
        inB.set(inBBool);
        inCarry.set(inCarryBool);
        fullAdder.execute(inA, inB, inCarry, outSum, outCarry);
        assertFields(inABool, inBBool, inCarryBool, outSumCorrect, outCarryCorrect);
        reset();
    }


    public void assertFields(boolean inACorrect, boolean inBCorrect, boolean inCarryCorrect, boolean outSumCorrect, boolean outCarryCorrect) {
        assertEquals(inACorrect, inA.getAsBool(), "inA is wrong.");
        assertEquals(inBCorrect, inB.getAsBool(), "inB is wrong.");
        assertEquals(inCarryCorrect, inCarry.getAsBool(), "inCarry is wrong.");
        assertEquals(outSumCorrect, outSum.getAsBool(), "Sum is wrong.");
        assertEquals(outCarryCorrect, outCarry.getAsBool(), "Carry is wrong.");
    }
}