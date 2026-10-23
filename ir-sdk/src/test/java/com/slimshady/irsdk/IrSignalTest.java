package com.slimshady.irsdk;
import org.junit.Test;
import static org.junit.Assert.*;
public class IrSignalTest {
    @Test public void parsesMixedSeparators() { assertArrayEquals(new int[]{9000,4500,560,560}, IrSignal.parse("38000", "9000, 4500\n560;560").getPattern()); }
    @Test public void defensiveCopies() { int[] source = {1,2}; IrSignal s = new IrSignal(38000, source); source[0] = 99; int[] out = s.getPattern(); out[1] = 99; assertArrayEquals(new int[]{1,2}, s.getPattern()); }
    @Test(expected=IllegalArgumentException.class) public void rejectsZeroFrequency() { new IrSignal(0, new int[]{1}); }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeDuration() { new IrSignal(38000, new int[]{1,-1}); }
    @Test(expected=IllegalArgumentException.class) public void rejectsEmpty() { IrSignal.parse("38000", ""); }
    @Test(expected=IllegalArgumentException.class) public void rejectsTooLong() { new IrSignal(38000, new int[]{1000000,1000000}); }
    @Test(expected=IllegalArgumentException.class) public void rejectsOverflow() { IrSignal.parse("38000", "2147483648"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsOverflowingTotal() { new IrSignal(38000, new int[]{Integer.MAX_VALUE,Integer.MAX_VALUE}); }
}
