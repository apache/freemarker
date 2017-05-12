package org.apache.freemarker.test;

/**
 * To avoid circular dependency, we can't use freemarker-core {@code _StringUtil}, so we copy-paste some of
 * the methods from there.
 */
class _TStringUtil {

    /**
     * Converts the parameter with <code>toString</code> (if it's not <code>null</code>) and passes it to
     * {@link #jQuote(String)}.
     */
    public static String jQuote(Object obj) {
        return jQuote(obj != null ? obj.toString() : null);
    }

    /**
     * Quotes string as Java Language string literal.
     * Returns string <code>"null"</code> if <code>s</code>
     * is <code>null</code>.
     */
    public static String jQuote(String s) {
        if (s == null) {
            return "null";
        }
        int ln = s.length();
        StringBuilder b = new StringBuilder(ln + 4);
        b.append('"');
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"') {
                b.append("\\\"");
            } else if (c == '\\') {
                b.append("\\\\");
            } else if (c < 0x20) {
                if (c == '\n') {
                    b.append("\\n");
                } else if (c == '\r') {
                    b.append("\\r");
                } else if (c == '\f') {
                    b.append("\\f");
                } else if (c == '\b') {
                    b.append("\\b");
                } else if (c == '\t') {
                    b.append("\\t");
                } else {
                    b.append("\\u00");
                    int x = c / 0x10;
                    b.append(toHexDigit(x));
                    x = c & 0xF;
                    b.append(toHexDigit(x));
                }
            } else {
                b.append(c);
            }
        } // for each characters
        b.append('"');
        return b.toString();
    }

    private static char toHexDigit(int d) {
        return (char) (d < 0xA ? d + '0' : d - 0xA + 'A');
    }

}
