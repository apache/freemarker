/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.servlet.jsp;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

public class TaglibMethodUtilsTest {

    @Test
    public void testGetMethodByFunctionSignature() throws Exception {
        Method expected = Functions.class.getMethod("plus", int.class, int.class);
        String signature = "int plus(int, int)";
        Method method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", double.class, double.class);
        signature = "double  plus ( double , double )";
        method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", String.class, String.class);
        signature = "java.lang.String plus ( java.lang.String  ,java.lang.String  )";
        method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", double[].class, double[].class);
        signature = "double[] plus ( double[]  ,double []  )";
        method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", String[].class, String[].class);
        signature = "java.lang.String [] plus ( java.lang.String[]  ,java.lang.String []  )";
        method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("sum", double[].class);
        signature = "double sum ( double[]  )";
        method = TaglibMethodUtils.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);
    }

    @SuppressWarnings("unused")
    private static class Functions {

        public static int plus(int a, int b) {
            return a + b;
        }

        public static double plus(double a, double b) {
            return a + b;
        }

        public static String plus(String a, String b) {
            return a + b;
        }

        public static double [] plus(double [] a, double [] b) { 
            double [] sum = new double[a.length];

            for (int i = 0; i < a.length; i++) {
                sum[i] = a[i] + b[i];
            }

            return sum;
        }

        public static String [] plus(String [] a, String [] b) { 
            String [] joins = new String[a.length];

            for (int i = 0; i < a.length; i++) {
                joins[i] = a[i] + b[i];
            }

            return joins;
        }

        public static double sum(double [] a) { 
            double sum = 0.0;
            for (double i : a) {
                sum += i;
            }
            return sum;
        }
    }
    
}
