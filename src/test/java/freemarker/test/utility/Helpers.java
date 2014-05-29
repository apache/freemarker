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
