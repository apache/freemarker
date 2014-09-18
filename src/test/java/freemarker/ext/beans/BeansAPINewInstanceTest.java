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

import junit.framework.TestCase;
import freemarker.template.Configuration;
import freemarker.test.utility.Helpers;

public class BeansAPINewInstanceTest extends TestCase {

    private BeansWrapper beansWrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();

    public BeansAPINewInstanceTest(String name) {
        super(name);
    }
    
    @SuppressWarnings("boxing")
    public void test() throws Exception {
        testClassConstructors("");
        testClassConstructors("int... [1]", 1);
        testClassConstructors("int 1, int 2", 1, 2);
        testClassConstructors("int... [1, 2, 3]", 1, 2, 3);
        testClassConstructors("int... [1, 2, 3]", 1, (byte) 2, (short) 3);
        try {
            testClassConstructors("int... [1, 2, 3]", 1, 2, (long) 3);
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
        
        testClassConstructors("int 1, int 2", (byte) 1, (short) 2);
        testClassConstructors("double 1.0, double 2.0", (long) 1, (short) 2);
        testClassConstructors("double 1.0, double 2.0", 1, 2f);
        testClassConstructors("Integer null, Integer null", null, null);
        testClassConstructors("Integer null, Integer 1", null, 1);
        testClassConstructors("int 1, String s", 1, "s");
        testClassConstructors("int 1, String null", 1, null);
        testClassConstructors("Object null, Object s", null, "s");
        testClassConstructors("Object 1.0, Object s", 1f, "s");
        
        testClassConstructors("Object s, int... [1, 2]", "s", 1, 2);
        testClassConstructors("Object s, int... []", "s");
        
        testClassConstructors2("int 1, int 2", (byte) 1, (short) 2);
        try {
            testClassConstructors2("int 1, int 2", 1, 2L);
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
        try {
            testClassConstructors2("", "", "");
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
        try {
            testClassConstructors2("int 1", 1);
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
        try {
            testClassConstructors2("");
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
    }
    
    private void testClassConstructors(String expected, Object... args) throws Exception {
        testCall(expected, Constructors.class, args); 
    }

    private void testClassConstructors2(String expected, Object... args) throws Exception {
        testCall(expected, Constructors2.class, args); 
    }
    
    private void testCall(String expected, Class cl, Object... args) throws Exception {
        Object obj = _BeansAPI.newInstance(cl, args, beansWrapper); 
        assertEquals(expected, obj.toString());        
    }
    
    public static class Constructors {
        private final String s;

        public Constructors() { s = ""; }
        
        public Constructors(int x, int y) { s = "int " + x + ", int " + y; }
        public Constructors(int x, String y) { s = "int " + x + ", String " + y; }
        public Constructors(int x, long y) { s = "int " + x + ", long " + y; }
        public Constructors(double x, double y) { s = "double " + x + ", double " + y; }
        public Constructors(Integer x, Integer y) { s = "Integer " + x + ", Integer " + y; }
        public Constructors(Object x, Object y) { s = "Object " + x + ", Object " + y; }

        public Constructors(int... xs) { s = "int... " + Helpers.arrayToString(xs); }
        public Constructors(Object x, int... ys) { s = "Object " + x + ", int... " + Helpers.arrayToString(ys); }
        
        @Override
        public String toString() {
            return s;
        }
    }

    public static class Constructors2 {
        private final String s;

        public Constructors2(int x, int y) { s = "int " + x + ", int " + y; }
        
        @Override
        public String toString() {
            return s;
        }
    }
    
}
