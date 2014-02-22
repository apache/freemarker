package freemarker.test.utility;

public class Helpers {

    private Helpers() { }

    public static String arrayToString(int[] xs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append('[');
        for (int x : xs) {
            if (sb.length() != 1) sb.append(", ");
            sb.append(x);
        }
        sb.append(']');
        
        return sb.toString();
    }

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

    public static String arrayToString(Object[] xs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append('[');
        for (Object x : xs) {
            if (sb.length() != 1) sb.append(", ");
            sb.append(x);
        }
        sb.append(']');
        
        return sb.toString();
    }
    
}
