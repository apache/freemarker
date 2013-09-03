package freemarker.ext.beans;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

@SuppressWarnings("boxing")
public class IsApplicableTest extends TestCase {

    public IsApplicableTest(String name) {
        super(name);
    }
    
    public void testSingle() {
        ArgumentTypes ats = new ArgumentTypes(new Object[] { new Object() }, true);
        assertApplicable(ats, Object.class);
        assertNotApplicable(ats, String.class);
        assertNotApplicable(ats, CharSequence.class);
        assertNotApplicable(ats, Integer.class);
        assertNotApplicable(ats, Integer.TYPE);
        
        ats = new ArgumentTypes(new Object[] { "" }, true);
        assertApplicable(ats, Object.class);
        assertApplicable(ats, CharSequence.class);
        assertApplicable(ats, String.class);
        assertNotApplicable(ats, Integer.class);
        assertNotApplicable(ats, Integer.TYPE);

        ats = new ArgumentTypes(new Object[] { 1 }, true);
        assertApplicable(ats, Object.class);
        assertNotApplicable(ats, CharSequence.class);
        assertNotApplicable(ats, String.class);
        assertNotApplicable(ats, Short.class);
        assertNotApplicable(ats, Short.TYPE);
        assertApplicable(ats, Integer.class);
        assertApplicable(ats, Integer.TYPE);
        assertApplicable(ats, Float.class);
        assertApplicable(ats, Float.TYPE);
        assertApplicable(ats, Double.class);
        assertApplicable(ats, Double.TYPE);
        assertApplicable(ats, BigDecimal.class);
        assertApplicable(ats, BigInteger.class);

        ats = new ArgumentTypes(new Object[] { new OverloadedNumberUtil.IntegerOrByte(1, (byte) 1) }, true);
        assertApplicable(ats, Object.class);
        assertNotApplicable(ats, CharSequence.class);
        assertNotApplicable(ats, String.class);
        assertApplicable(ats, Short.class);
        assertApplicable(ats, Short.TYPE);
        assertApplicable(ats, Integer.class);
        assertApplicable(ats, Integer.TYPE);
        assertApplicable(ats, Float.class);
        assertApplicable(ats, Float.TYPE);
        assertApplicable(ats, Double.class);
        assertApplicable(ats, Double.TYPE);
        assertApplicable(ats, BigDecimal.class);
        assertApplicable(ats, BigInteger.class);
        
        ats = new ArgumentTypes(new Object[] { 1.0f }, true);
        assertApplicable(ats, Object.class);
        assertNotApplicable(ats, CharSequence.class);
        assertNotApplicable(ats, String.class);
        assertNotApplicable(ats, Integer.class);
        assertNotApplicable(ats, Integer.TYPE);
        assertApplicable(ats, Float.class);
        assertApplicable(ats, Float.TYPE);
        assertApplicable(ats, Double.class);
        assertApplicable(ats, Double.TYPE);
        assertApplicable(ats, BigDecimal.class);
        assertNotApplicable(ats, BigInteger.class);
        
        ats = new ArgumentTypes(new Object[] { null }, true);
        assertApplicable(ats, Object.class);
        assertApplicable(ats, String.class);
        assertApplicable(ats, Integer.class);
        assertNotApplicable(ats, Integer.TYPE);
        assertNotApplicable(ats, Boolean.TYPE);
        assertNotApplicable(ats, Object.class, Object.class);
        assertNotApplicable(ats);
    }
    
    public void testMulti() {
        ArgumentTypes ats = new ArgumentTypes(new Object[] { new Object(), "", 1, true }, true);
        assertApplicable(ats, Object.class, Object.class, Object.class, Object.class);
        assertApplicable(ats, Object.class, String.class, Number.class, Boolean.class);
        assertApplicable(ats, Object.class, CharSequence.class, Integer.class, Serializable.class);
        assertApplicable(ats, Object.class, Comparable.class, Integer.TYPE, Serializable.class);
        assertNotApplicable(ats, Object.class, String.class, Number.class, Number.class);
        assertNotApplicable(ats, Object.class, StringBuffer.class, Number.class, Boolean.class);
        assertNotApplicable(ats, int.class, Object.class, Object.class, Object.class);
        assertNotApplicable(ats, Object.class, Object.class, Object.class);
        assertNotApplicable(ats, Object.class, Object.class, Object.class, Object.class, Object.class);
    }    

    public void testNoParam() {
        ArgumentTypes ats = new ArgumentTypes(new Object[] { }, true);
        assertApplicable(ats);
        assertNotApplicable(ats, Object.class);
    }

    public void testVarags() {
        Object[][] argLists = new Object[][] {
            new Object[] { "", 1, 2, 3 },
            new Object[] { "", 1, (byte) 2, 3 },
            new Object[] { "", 1},
            new Object[] { "" },
        };
        for (Object[] args : argLists) {
            ArgumentTypes ats = new ArgumentTypes(args, true);
            assertApplicable(ats, true, String.class, int[].class);
            assertApplicable(ats, true, String.class, Integer[].class);
            assertApplicable(ats, true, Object.class, Comparable[].class);
            assertApplicable(ats, true, Object.class, Object[].class);
            assertNotApplicable(ats, true, StringBuilder.class, int[].class);
            if (args.length > 1) {
                assertNotApplicable(ats, true, String.class, String[].class);
            } else {
                assertApplicable(ats, true, String.class, String[].class);
            }
        }
    }
    
    private void assertNotApplicable(ArgumentTypes ats, Class... paramTypes) {
        assertNotApplicable(ats, false, paramTypes);
    }
    
    private void assertNotApplicable(ArgumentTypes ats, boolean varargs, Class... paramTypes) {
        List tested = new ArrayList();
        tested.add(new CallableMemberDescriptor((Method) null, paramTypes));
        if (ats.getApplicables(tested, varargs).size() != 0) {
            fail("Parameter types were applicable");
        }
    }

    private void assertApplicable(ArgumentTypes ats, Class<?>... paramTypes) {
        assertApplicable(ats, false, paramTypes);
    }
    
    private void assertApplicable(ArgumentTypes ats, boolean varargs, Class<?>... paramTypes) {
        List tested = new ArrayList();
        tested.add(new CallableMemberDescriptor((Method) null, paramTypes));
        if (ats.getApplicables(tested, varargs).size() != 1) {
            fail("Parameter types weren't applicable");
        }
    }

}
