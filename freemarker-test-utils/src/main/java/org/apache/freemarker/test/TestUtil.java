package org.apache.freemarker.test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;

public final class TestUtil {

    private TestUtil() {
        // Not meant to be instantiated
    }

    /**
     * Returns the closes FreeMarker version number that doesn't exit yet (so it's illegal).
     */
    public static Version getClosestFutureVersion() {
        Version v = Configuration.getVersion();
        return new Version(v.getMajor(), v.getMinor(), v.getMicro() + 1);
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

    public static String removeTxtCopyrightComment(String s) {
        if (!s.startsWith("/*")) {
            return s;
        }

        int commentEnd = s.indexOf("*/");
        if (commentEnd == -1) {
            return s;
        }
        commentEnd += 2;
        if (commentEnd < s.length()) {
            char c = s.charAt(commentEnd);
            if (c == '\n' || c == '\r') {
                commentEnd++;
                if (c == '\r' && commentEnd < s.length()) {
                    if (s.charAt(commentEnd) == '\n') {
                        commentEnd++;
                    }
                }
            }
        }

        String comment = s.substring(0, commentEnd);
        int copyrightIdx = comment.indexOf("copyright");
        if (copyrightIdx == -1) {
            copyrightIdx = comment.indexOf("Copyright");
        }
        if (copyrightIdx == -1) {
            return s;
        }

        return s.substring(commentEnd);
    }

    public static String removeFTLCopyrightComment(String ftl) {
        if (ftl.contains("<#ftl nsPrefixes = {\"D\" : \"http://example.com/eBook\"}>")) {
            System.out.println();
        }

        int copyrightIdx = ftl.indexOf("copyright");
        if (copyrightIdx == -1) {
            copyrightIdx = ftl.indexOf("Copyright");
        }
        if (copyrightIdx == -1) {
            return ftl;
        }

        final int commentFirstIdx;
        final boolean squareBracketTagSyntax;
        {
            String ftlBeforeCopyright = ftl.substring(0, copyrightIdx);
            int abCommentStart = ftlBeforeCopyright.lastIndexOf("<#--");
            int sbCommentStart = ftlBeforeCopyright.lastIndexOf("[#--");
            squareBracketTagSyntax = sbCommentStart > abCommentStart;
            commentFirstIdx = squareBracketTagSyntax ? sbCommentStart : abCommentStart;
            if (commentFirstIdx == -1) {
                throw new AssertionError("Can't find copyright comment start");
            }
        }

        final int commentLastIdx;
        {
            int commentEndStart = ftl.indexOf(squareBracketTagSyntax ? "--]" : "-->", copyrightIdx);
            if (commentEndStart == -1) {
                throw new AssertionError("Can't find copyright comment end");
            }
            commentLastIdx = commentEndStart + 2;
        }

        final int afterCommentNLChars;
        if (commentLastIdx + 1 < ftl.length()) {
            char afterCommentChar = ftl.charAt(commentLastIdx + 1);
            if (afterCommentChar == '\n' || afterCommentChar == '\r') {
                if (afterCommentChar == '\r' && commentLastIdx + 2 < ftl.length() && ftl.charAt(commentLastIdx + 2) == '\n') {
                    afterCommentNLChars = 2;
                } else {
                    afterCommentNLChars = 1;
                }
            } else {
                afterCommentNLChars = 0;
            }
        } else {
            afterCommentNLChars = 0;
        }

        return ftl.substring(0, commentFirstIdx) + ftl.substring(commentLastIdx + afterCommentNLChars + 1);
    }
}
