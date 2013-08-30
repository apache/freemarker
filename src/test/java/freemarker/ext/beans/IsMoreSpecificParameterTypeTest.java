package freemarker.ext.beans;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class IsMoreSpecificParameterTypeTest extends TestCase {

    public IsMoreSpecificParameterTypeTest(String name) {
        super(name);
    }
    
    public void testFixed() {
        assertEquals(1, MethodUtilities.isMoreOrSameSpecificParameterType(String.class, String.class, true, 0));
        assertEquals(1, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, int.class, true, 0));
        
        assertEquals(2, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Integer.class, true, 0));
        assertEquals(2, MethodUtilities.isMoreOrSameSpecificParameterType(boolean.class, Boolean.class, true, 0));
        
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, long.class, true, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, double.class, true, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Long.class, true, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Double.class, true, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Long.class, true, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Double.class, true, 0));

        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(HashMap.class, Map.class, true, 0));
        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(String.class, CharSequence.class, true, 0));
        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Number.class, true, 0));
        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Number.class, true, 0));
        
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Map.class, String.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, int.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Boolean.class, boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, String.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, BigDecimal.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Long.class, Integer.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(long.class, Integer.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Long.class, int.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, BigDecimal.class, true, 0));
    }
    
    public void testBuggy() {
        assertEquals(1, MethodUtilities.isMoreOrSameSpecificParameterType(String.class, String.class, false, 0));
        assertEquals(1, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, int.class, false, 0));
        
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Integer.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(boolean.class, Boolean.class, false, 0));
        
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, long.class, false, 0));
        assertEquals(3, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, double.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Long.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Double.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Long.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Double.class, false, 0));

        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(HashMap.class, Map.class, false, 0));
        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(String.class, CharSequence.class, false, 0));
        assertEquals(4, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, Number.class, false, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Number.class, false, 0));
        
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Map.class, String.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, int.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Boolean.class, boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, Boolean.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, String.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(int.class, BigDecimal.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Long.class, Integer.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(long.class, Integer.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Long.class, int.class, true, 0));
        assertEquals(0, MethodUtilities.isMoreOrSameSpecificParameterType(Integer.class, BigDecimal.class, true, 0));
    }

}
