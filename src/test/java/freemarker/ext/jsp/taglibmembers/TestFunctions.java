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

package freemarker.ext.jsp.taglibmembers;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

public final class TestFunctions {
    
    private TestFunctions() {
        // Not meant to be instantiated
    }
    
    public static String reverse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i  = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    public static int reverse(int i) {
        return reverse(i, 10);
    }

    public static int reverse(int i, int radix) {
        final int signum = i >= 0 ? 1 : -1;
        String s = reverse(Integer.toString(i  * signum, radix));
        return Integer.parseInt(s, radix) * signum;
    }
    
    public static long sum(int... xs) {
        long sum = 0;
        for (int x : xs) {
            sum += x;
        }
        return sum;
    }

    @SuppressWarnings("boxing")
    public static String sum(Map<String, Integer> m) {
        long sum = 0;
        StringBuilder keys = new StringBuilder(); 
        for (Entry<String, Integer> e : m.entrySet()) {
            keys.append(e.getKey());
            sum += e.getValue();
        }
        return keys.append('=').append(sum).toString();
    }
    
    public static int[] testArray() {
        return new int[] { 1, 2, 3 };
    }

    @SuppressWarnings("boxing")
    public static Map<String, Integer> testMap() {
        return ImmutableMap.<String, Integer>of("a", 1, "b", 2, "c", 3);
    }
    
}
