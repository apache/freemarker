package freemarker.template.utility;

import java.math.BigDecimal;
import java.math.BigInteger;

import junit.framework.TestCase;

public class NumberUtilTest extends TestCase {

    public NumberUtilTest(String name) {
        super(name);
    }
    
    public void testGetSignum() {
        assertEquals(1, NumberUtil.getSignum(Double.valueOf(Double.POSITIVE_INFINITY)));
        assertEquals(1, NumberUtil.getSignum(Double.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(Double.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(Double.valueOf(-3)));
        assertEquals(-1, NumberUtil.getSignum(Double.valueOf(Double.NEGATIVE_INFINITY)));
        try {
            NumberUtil.getSignum(Double.valueOf(Double.NaN));
            fail();
        } catch (ArithmeticException e) {
            // expected
        }
        
        assertEquals(1, NumberUtil.getSignum(Float.valueOf(Float.POSITIVE_INFINITY)));
        assertEquals(1, NumberUtil.getSignum(Float.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(Float.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(Float.valueOf(-3)));
        assertEquals(-1, NumberUtil.getSignum(Float.valueOf(Float.NEGATIVE_INFINITY)));
        try {
            NumberUtil.getSignum(Float.valueOf(Float.NaN));
            fail();
        } catch (ArithmeticException e) {
            // expected
        }
        
        assertEquals(1, NumberUtil.getSignum(Long.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(Long.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(Long.valueOf(-3)));
        
        assertEquals(1, NumberUtil.getSignum(Integer.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(Integer.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(Integer.valueOf(-3)));
        
        assertEquals(1, NumberUtil.getSignum(Short.valueOf((short) 3)));
        assertEquals(0, NumberUtil.getSignum(Short.valueOf((short) 0)));
        assertEquals(-1, NumberUtil.getSignum(Short.valueOf((short) -3)));
        
        assertEquals(1, NumberUtil.getSignum(Byte.valueOf((byte) 3)));
        assertEquals(0, NumberUtil.getSignum(Byte.valueOf((byte) 0)));
        assertEquals(-1, NumberUtil.getSignum(Byte.valueOf((byte) -3)));
        
        assertEquals(1, NumberUtil.getSignum(BigDecimal.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(BigDecimal.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(BigDecimal.valueOf(-3)));
        
        assertEquals(1, NumberUtil.getSignum(BigInteger.valueOf(3)));
        assertEquals(0, NumberUtil.getSignum(BigInteger.valueOf(0)));
        assertEquals(-1, NumberUtil.getSignum(BigInteger.valueOf(-3)));
    }

}
