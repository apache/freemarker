package freemarker.ext.jsp.mocked;

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
    
}
