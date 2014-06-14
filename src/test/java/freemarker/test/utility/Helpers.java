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

package freemarker.test.utility;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Helpers {

    private Helpers() { }

    public static String arrayToString(double[] xs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append('[');
        for (double x : xs) {
            if (sb.length() != 1) sb.append(", ");
            sb.append(x);
        }
        sb.append(']');
        
        return sb.toString();
    }

    public static String arrayToString(Object[] array) {
        if (array == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String arrayToString(Object[][] arrayArray) {
        if (arrayArray == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Object[] array : arrayArray) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(arrayToString(array));
        }
        sb.append(']');
        return sb.toString();
    }
    
    public static String arrayToString(int[] array) {
        if (array == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
    
    public static String arrayToString(int[][] xss) {
        if (xss == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < xss.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(arrayToString(xss[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String arrayToString(char[] array) {
        if (array == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
    
    public static String arrayToString(boolean[] array) {
        if (array == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String listToString(List<?> list) {
        return collectionToString("", list);
    }
    
    
    public static String setToString(Set<?> list) {
        return collectionToString("Set", list);
    }
    
    private static String collectionToString(String prefix, Collection<?> list) {
        if (list == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(item instanceof Object[] ? arrayToString((Object[]) item) : item);
        }
        sb.append(']');
        return sb.toString();
    }
    
}
