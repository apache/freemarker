/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        assertEquals(1, _MethodUtil.isMoreOrSameSpecificParameterType(String.class, String.class, true, 0));
        assertEquals(1, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, int.class, true, 0));
        
        assertEquals(2, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Integer.class, true, 0));
        assertEquals(2, _MethodUtil.isMoreOrSameSpecificParameterType(boolean.class, Boolean.class, true, 0));
        
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, long.class, true, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, double.class, true, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Long.class, true, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Double.class, true, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Long.class, true, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Double.class, true, 0));

        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(HashMap.class, Map.class, true, 0));
        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(String.class, CharSequence.class, true, 0));
        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Number.class, true, 0));
        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Number.class, true, 0));
        
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Map.class, String.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, int.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Boolean.class, boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, String.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, BigDecimal.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Long.class, Integer.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(long.class, Integer.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Long.class, int.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, BigDecimal.class, true, 0));
    }
    
    public void testBuggy() {
        assertEquals(1, _MethodUtil.isMoreOrSameSpecificParameterType(String.class, String.class, false, 0));
        assertEquals(1, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, int.class, false, 0));
        
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Integer.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(boolean.class, Boolean.class, false, 0));
        
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, long.class, false, 0));
        assertEquals(3, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, double.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Long.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Double.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Long.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Double.class, false, 0));

        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(HashMap.class, Map.class, false, 0));
        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(String.class, CharSequence.class, false, 0));
        assertEquals(4, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, Number.class, false, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Number.class, false, 0));
        
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Map.class, String.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, int.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Boolean.class, boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, Boolean.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, String.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(int.class, BigDecimal.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Long.class, Integer.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(long.class, Integer.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Long.class, int.class, true, 0));
        assertEquals(0, _MethodUtil.isMoreOrSameSpecificParameterType(Integer.class, BigDecimal.class, true, 0));
    }

}
