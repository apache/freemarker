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

package freemarker.template.utility;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import freemarker.core.BugException;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.template.Template;
import freemarker.template.Version;

/**
 *  Some text related utilities.
 */
public class StringUtil {
    private static final char[] ESCAPES = createEscapes();

    /*
     *  For better performance most methods are folded down. Don't you scream... :)
     */

    /**
     *  HTML encoding (does not convert line breaks and apostrophe-quote).
     *  Replaces all '&gt;' '&lt;' '&amp;' and '"' with entity reference, but not "'" (apostrophe-quote).
     *  The last is not escaped as back then when this was written some user agents didn't understood 
     *  "&amp;apos;" nor "&amp;#39;".
     *    
     *  @deprecated Use {@link #XHTMLEnc(String)} instead, because it escapes apostrophe-quote too.
     */
    public static String HTMLEnc(String s) {
        return XMLEncNA(s);
    }

    /**
     *  XML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     */
    public static String XMLEnc(String s) {
        return XMLOrXHTMLEnc(s, "&apos;");
    }

    /**
     *  XHTML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     *  suitable for XHTML decoding in common user agents (including legacy
     *  user agents, which do not decode "&amp;apos;" to "'", so "&amp;#39;" is used
     *  instead [see http://www.w3.org/TR/xhtml1/#C_16])
     */
    public static String XHTMLEnc(String s) {
        return XMLOrXHTMLEnc(s, "&#39;");
    }
    
    private static String XMLOrXHTMLEnc(String s, String aposReplacement) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    case '"': b.append("&quot;"); break;
                    case '\'': b.append(aposReplacement); break;
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            case '"': b.append("&quot;"); break;
                            case '\'': b.append(aposReplacement); break;
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) b.append(s.substring(next));
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }
    
    /**
     *  XML encoding without replacing apostrophes.
     *  @see #XMLEnc(String)
     */
    public static String XMLEncNA(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    case '"': b.append("&quot;"); break;
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '>' || c == '&' || c == '"') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            case '"': b.append("&quot;"); break;
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) b.append(s.substring(next));
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  XML encoding for attributes values quoted with <tt>"</tt> (not with <tt>'</tt>!).
     *  Also can be used for HTML attributes that are quoted with <tt>"</tt>.
     *  @see #XMLEnc(String)
     */
    public static String XMLEncQAttr(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '&' || c == '"') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '&': b.append("&amp;"); break;
                    case '"': b.append("&quot;"); break;
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '&' || c == '"') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '&': b.append("&amp;"); break;
                            case '"': b.append("&quot;"); break;
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  XML encoding without replacing apostrophes and quotation marks and
     *  greater-thans (except in {@code ]]>}).
     *  @see #XMLEnc(String)
     */
    public static String XMLEncNQG(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<'
                    || (c == '>' && i > 1
                            && s.charAt(i - 1) == ']'
                            && s.charAt(i - 2) == ']')
                    || c == '&') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    default: throw new BugException();
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<'
                            || (c == '>' && i > 1
                                    && s.charAt(i - 1) == ']'
                                    && s.charAt(i - 2) == ']')
                            || c == '&') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            default: throw new BugException();
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }
    
    /**
     *  Rich Text Format encoding (does not replace line breaks).
     *  Escapes all '\' '{' '}' and '"'
     */
    public static String RTFEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '\\': b.append("\\\\"); break;
                    case '{': b.append("\\{"); break;
                    case '}': b.append("\\}"); break;
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '\\' || c == '{' || c == '}') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '\\': b.append("\\\\"); break;
                            case '{': b.append("\\{"); break;
                            case '}': b.append("\\}"); break;
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) b.append(s.substring(next));
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     * URL encoding (like%20this) for query parameter values, path <em>segments</em>, fragments; this encodes all
     * characters that are reserved anywhere.
     */
    public static String URLEnc(String s, String charset) throws UnsupportedEncodingException {
        return URLEnc(s, charset, false);
    }
    
    /**
     * Like {@link #URLEnc(String, String)} but doesn't escape the slash character ({@code /}).
     * This can be used to encode a path only if you know that no folder or file name will contain {@code /}
     * character (not in the path, but in the name itself), which usually stands, as the commonly used OS-es don't
     * allow that.
     * 
     * @since 2.3.21
     */
    public static String URLPathEnc(String s, String charset) throws UnsupportedEncodingException {
        return URLEnc(s, charset, true);
    }
    
    private static String URLEnc(String s, String charset, boolean keepSlash)
            throws UnsupportedEncodingException {
        int ln = s.length();
        int i;
        for (i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (!safeInURL(c, keepSlash)) {
                break;
            }
        }
        if (i == ln) {
            // Nothing to escape
            return s;
        }

        StringBuffer b = new StringBuffer(ln + ln / 3 + 2);
        b.append(s.substring(0, i));

        int encStart = i;
        for (i++; i < ln; i++) {
            char c = s.charAt(i);
            if (safeInURL(c, keepSlash)) {
                if (encStart != -1) {
                    byte[] o = s.substring(encStart, i).getBytes(charset);
                    for (int j = 0; j < o.length; j++) {
                        b.append('%');
                        byte bc = o[j];
                        int c1 = bc & 0x0F;
                        int c2 = (bc >> 4) & 0x0F;
                        b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                        b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
                    }
                    encStart = -1;
                }
                b.append(c);
            } else {
                if (encStart == -1) {
                    encStart = i;
                }
            }
        }
        if (encStart != -1) {
            byte[] o = s.substring(encStart, i).getBytes(charset);
            for (int j = 0; j < o.length; j++) {
                b.append('%');
                byte bc = o[j];
                int c1 = bc & 0x0F;
                int c2 = (bc >> 4) & 0x0F;
                b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
            }
        }
        
        return b.toString();
    }

    private static boolean safeInURL(char c, boolean keepSlash) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                || c >= '0' && c <= '9'
                || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                || c >= '\'' && c <= '*'
                || keepSlash && c == '/';
    }
    
    private static char[] createEscapes()
    {
        char[] escapes = new char['\\' + 1];
        for(int i = 0; i < 32; ++i)
        {
            escapes[i] = 1;
        }
        escapes['\\'] = '\\';
        escapes['\''] = '\'';
        escapes['"'] = '"';
        escapes['<'] = 'l';
        escapes['>'] = 'g';
        escapes['&'] = 'a';
        escapes['\b'] = 'b';
        escapes['\t'] = 't';
        escapes['\n'] = 'n';
        escapes['\f'] = 'f';
        escapes['\r'] = 'r';
        return escapes;
    }

    /**
     * Escapes a string according the FTL string literal escaping rules, assuming the literal is quoted with
     * {@code quotation}; it doesn't add the quotation marks itself.
     * 
     * @param quotation
     *            Either {@code '"'} or {@code '\''}. It's assumed that the string literal whose part we calculate is
     *            enclosed within this kind of quotation mark. Thus, the other kind of quotation character will not be
     *            escaped in the result.
     *
     * @since 2.3.22
     */
    public static String FTLStringLiteralEnc(String s, char quotation) {
        return FTLStringLiteralEnc(s, quotation, false);
    }
    
    /**
     * Escapes a string according the FTL string literal escaping rules; it doesn't add the quotation marks. As this
     * method doesn't know if the string literal is quoted with reuglar quotation marks or apostrophe quute, it will
     * escape both.
     * 
     * @see #FTLStringLiteralEnc(String, char)
     */
    public static String FTLStringLiteralEnc(String s) {
        return FTLStringLiteralEnc(s, (char) 0, false);
    }

    private static String FTLStringLiteralEnc(String s, char quotation, boolean addQuotation)
    {
        final int ln = s.length();
        
        final char otherQuotation;
        if (quotation == 0) {
            otherQuotation = 0;
        } else if (quotation == '"') {
            otherQuotation = '\'';
        } else if (quotation == '\'') {
            otherQuotation = '"';
        } else {
            throw new IllegalArgumentException("Unsupported quotation character: " + quotation);
        }
        
        final int escLn = ESCAPES.length;
        StringBuffer buf = null;
        for(int i = 0; i < ln; i++)
        {
            char c = s.charAt(i);
            char escape =
                    c < escLn ? ESCAPES[c] :
                    c == '{' && i > 0 && isInterpolationStart(s.charAt(i - 1)) ? '{' :
                    0;
            if (escape == 0 || escape == otherQuotation) {
                if (buf != null) {
                    buf.append(c);
                }
            } else {
                if (buf == null) {
                    buf = new StringBuffer(s.length() + 4 + (addQuotation ? 2 : 0));
                    if (addQuotation) {
                        buf.append(quotation);
                    }
                    buf.append(s.substring(0, i));
                }
                if (escape == 1) {
                    // hex encoding for characters below 0x20
                    // that have no other escape representation
                    buf.append("\\x00");
                    int c2 = (c >> 4) & 0x0F;
                    c = (char) (c & 0x0F);
                    buf.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                    buf.append((char) (c < 10 ? c + '0' : c - 10 + 'A'));
                } else {
                    buf.append('\\');
                    buf.append(escape);
                }
            }
        }
        
        if (buf == null) {
            return addQuotation ? quotation + s + quotation : s;
        } else {
            if (addQuotation) {
                buf.append(quotation);
            }
            return buf.toString();
        }
    }

    private static boolean isInterpolationStart(char c) {
        return c == '$' || c == '#';
    }

    /**
     * FTL string literal decoding.
     *
     * \\, \", \', \n, \t, \r, \b and \f will be replaced according to
     * Java rules. In additional, it knows \g, \l, \a and \{ which are
     * replaced with &lt;, &gt;, &amp; and { respectively.
     * \x works as hexadecimal character code escape. The character
     * codes are interpreted according to UCS basic plane (Unicode).
     * "f\x006Fo", "f\x06Fo" and "f\x6Fo" will be "foo".
     * "f\x006F123" will be "foo123" as the maximum number of digits is 4.
     *
     * All other \X (where X is any character not mentioned above or End-of-string)
     * will cause a ParseException.
     *
     * @param s String literal <em>without</em> the surrounding quotation marks
     * @return String with all escape sequences resolved
     * @throws ParseException if there string contains illegal escapes
     */
    public static String FTLStringLiteralDec(String s) throws ParseException {

        int idx = s.indexOf('\\');
        if (idx == -1) {
            return s;
        }

        int lidx = s.length() - 1;
        int bidx = 0;
        StringBuffer buf = new StringBuffer(lidx);
        do {
            buf.append(s.substring(bidx, idx));
            if (idx >= lidx) {
                throw new ParseException("The last character of string literal is backslash", 0,0);
            }
            char c = s.charAt(idx + 1);
            switch (c) {
                case '"':
                    buf.append('"');
                    bidx = idx + 2;
                    break;
                case '\'':
                    buf.append('\'');
                    bidx = idx + 2;
                    break;
                case '\\':
                    buf.append('\\');
                    bidx = idx + 2;
                    break;
                case 'n':
                    buf.append('\n');
                    bidx = idx + 2;
                    break;
                case 'r':
                    buf.append('\r');
                    bidx = idx + 2;
                    break;
                case 't':
                    buf.append('\t');
                    bidx = idx + 2;
                    break;
                case 'f':
                    buf.append('\f');
                    bidx = idx + 2;
                    break;
                case 'b':
                    buf.append('\b');
                    bidx = idx + 2;
                    break;
                case 'g':
                    buf.append('>');
                    bidx = idx + 2;
                    break;
                case 'l':
                    buf.append('<');
                    bidx = idx + 2;
                    break;
                case 'a':
                    buf.append('&');
                    bidx = idx + 2;
                    break;
                case '{':
                    buf.append('{');
                    bidx = idx + 2;
                    break;
                case 'x': {
                    idx += 2;
                    int x = idx;
                    int y = 0;
                    int z = lidx > idx + 3 ? idx + 3 : lidx;
                    while (idx <= z) {
                        char b = s.charAt(idx);
                        if (b >= '0' && b <= '9') {
                            y <<= 4;
                            y += b - '0';
                        } else if (b >= 'a' && b <= 'f') {
                            y <<= 4;
                            y += b - 'a' + 10;
                        } else if (b >= 'A' && b <= 'F') {
                            y <<= 4;
                            y += b - 'A' + 10;
                        } else {
                            break;
                        }
                        idx++;
                    }
                    if (x < idx) {
                        buf.append((char) y);
                    } else {
                        throw new ParseException("Invalid \\x escape in a string literal",0,0);
                    }
                    bidx = idx;
                    break;
                }
                default:
                    throw new ParseException("Invalid escape sequence (\\" + c + ") in a string literal",0,0);
            }
            idx = s.indexOf('\\', bidx);
        } while (idx != -1);
        buf.append(s.substring(bidx));

        return buf.toString();
    }

    public static Locale deduceLocale(String input) {
       if (input == null) return null;
       Locale locale = Locale.getDefault();
       if (input.length() > 0 && input.charAt(0) == '"') input = input.substring(1, input.length() -1);
       StringTokenizer st = new StringTokenizer(input, ",_ ");
       String lang = "", country = "";
       if (st.hasMoreTokens()) {
          lang = st.nextToken();
       }
       if (st.hasMoreTokens()) {
          country = st.nextToken();
       }
       if (!st.hasMoreTokens()) {
          locale = new Locale(lang, country);
       }
       else {
          locale = new Locale(lang, country, st.nextToken());
       }
       return locale;
    }

    public static String capitalize(String s) {
        StringTokenizer st = new StringTokenizer(s, " \t\r\n", true);
        StringBuffer buf = new StringBuffer(s.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            buf.append(tok.substring(0, 1).toUpperCase());
            buf.append(tok.substring(1).toLowerCase());
        }
        return buf.toString();
    }

    public static boolean getYesNo(String s) {
        if (s.startsWith("\"")) {
            s = s.substring(1, s.length() -1);

        }
        if (s.equalsIgnoreCase("n")
                || s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("f")
                || s.equalsIgnoreCase("false")) {
            return false;
        }
        else if  (s.equalsIgnoreCase("y")
                || s.equalsIgnoreCase("yes")
                || s.equalsIgnoreCase("t")
                || s.equalsIgnoreCase("true")) {
            return true;
        }
        throw new IllegalArgumentException("Illegal boolean value: " + s);
    }

    /**
     * Splits a string at the specified character.
     */
    public static String[] split(String s, char c) {
        int i, b, e;
        int cnt;
        String res[];
        int ln = s.length();

        i = 0;
        cnt = 1;
        while ((i = s.indexOf(c, i)) != -1) {
            cnt++;
            i++;
        }
        res = new String[cnt];

        i = 0;
        b = 0;
        while (b <= ln) {
            e = s.indexOf(c, b);
            if (e == -1) e = ln;
            res[i++] = s.substring(b, e);
            b = e + 1;
        }
        return res;
    }

    /**
     * Splits a string at the specified string.
     */
    public static String[] split(String s, String sep, boolean caseInsensitive) {
        String splitString = caseInsensitive ? sep.toLowerCase() : sep;
        String input = caseInsensitive ? s.toLowerCase() : s;
        int i, b, e;
        int cnt;
        String res[];
        int ln = s.length();
        int sln = sep.length();

        if (sln == 0) throw new IllegalArgumentException(
                "The separator string has 0 length");

        i = 0;
        cnt = 1;
        while ((i = input.indexOf(splitString, i)) != -1) {
            cnt++;
            i += sln;
        }
        res = new String[cnt];

        i = 0;
        b = 0;
        while (b <= ln) {
            e = input.indexOf(splitString, b);
            if (e == -1) e = ln;
            res[i++] = s.substring(b, e);
            b = e + sln;
        }
        return res;
    }

    /**
     * Same as {@link #replace(String, String, String, boolean, boolean)} with two {@code false} parameters. 
     * @since 2.3.20
     */
    public static String replace(String text, String oldSub, String newSub) {
        return replace(text, oldSub, newSub, false, false);
    }
    
    /**
     * Replaces all occurrences of a sub-string in a string.
     * @param text The string where it will replace <code>oldsub</code> with
     *     <code>newsub</code>.
     * @return String The string after the replacements.
     */
    public static String replace(String text, 
                                  String oldsub, 
                                  String newsub, 
                                  boolean caseInsensitive,
                                  boolean firstOnly) 
    {
        StringBuffer buf;
        int tln;
        int oln = oldsub.length();
        
        if (oln == 0) {
            int nln = newsub.length();
            if (nln == 0) {
                return text;
            } else {
                if (firstOnly) {
                    return newsub + text;
                } else {
                    tln = text.length();
                    buf = new StringBuffer(tln + (tln + 1) * nln);
                    buf.append(newsub);
                    for (int i = 0; i < tln; i++) {
                        buf.append(text.charAt(i));
                        buf.append(newsub);
                    }
                    return buf.toString();
                }
            }
        } else {
            oldsub = caseInsensitive ? oldsub.toLowerCase() : oldsub;
            String input = caseInsensitive ? text.toLowerCase() : text;
            int e = input.indexOf(oldsub);
            if (e == -1) {
                return text;
            }
            int b = 0;
            tln = text.length();
            buf = new StringBuffer(
                    tln + Math.max(newsub.length() - oln, 0) * 3);
            do {
                buf.append(text.substring(b, e));
                buf.append(newsub);
                b = e + oln;
                e = input.indexOf(oldsub, b);
            } while (e != -1 && !firstOnly);
            buf.append(text.substring(b));
            return buf.toString();
        }
    }

    /**
     * Removes the line-break from the end of the string.
     */
    public static String chomp(String s) {
        if (s.endsWith("\r\n")) return s.substring(0, s.length() - 2);
        if (s.endsWith("\r") || s.endsWith("\n"))
                return s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Converts a 0-length string to null, leaves the string as is otherwise.
     * @param s maybe {@code null}.
     */
    public static String emptyToNull(String s) {
    	if (s == null) return null;
    	return s.length() == 0 ? null : s;
    }
    
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
        StringBuffer b = new StringBuffer(ln + 4);
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

    /**
     * Converts the parameter with <code>toString</code> (if not
     * <code>null</code>)and passes it to {@link #jQuoteNoXSS(String)}. 
     */
    public static String jQuoteNoXSS(Object obj) {
        return jQuoteNoXSS(obj != null ? obj.toString() : null);
    }
    
    /**
     * Same as {@link #jQuoteNoXSS(String)} but also escapes <code>'&lt;'</code>
     * as <code>\</code><code>u003C</code>. This is used for log messages to prevent XSS
     * on poorly written Web-based log viewers. 
     */
    public static String jQuoteNoXSS(String s) {
        if (s == null) {
            return "null";
        }
        int ln = s.length();
        StringBuffer b = new StringBuffer(ln + 4);
        b.append('"');
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"') {
                b.append("\\\"");
            } else if (c == '\\') {
                b.append("\\\\");
            } else if (c == '<') {
                b.append("\\u003C");
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
    
    /**
     * Creates a <em>quoted</em> FTL string literal from a string, using escaping where necessary. The result either
     * uses regular quotation marks (UCS 0x22) or apostrophe-quotes (UCS 0x27), depending on the string content.
     * (Currently, apostrophe-quotes will be chosen exactly when the string contains regular quotation character and
     * doesn't contain apostrophe-quote character.)
     *
     * @param s
     *            The value that should be converted to an FTL string literal whose evaluated value equals to {@code s}
     *
     * @since 2.3.22
     */
    public static String ftlQuote(String s) {
        char quotation;
        if (s.indexOf('"') != -1 && s.indexOf('\'') == -1) {
            quotation = '\'';
        } else {
            quotation = '\"';
        }
        return FTLStringLiteralEnc(s, quotation, true);
    }
    
    /**
     * Tells if a character can occur on the beginning of an FTL identifier expression (without escaping). 
     * 
     * @since 2.3.22
     */
    public static boolean isFTLIdentifierStart(final char c) {
        // This code was generated on JDK 1.8.0_20 Win64 with src/main/misc/identifierChars/IdentifierCharGenerator.java
        if (c < 0xAA) { // This branch was edited for speed.
            if (c >= 'a' && c <= 'z' || c >= '@' && c <= 'Z') {
                return true;
            } else {
                return c == '$' || c == '_'; 
            }
        } else { // c >= 0xAA
            if (c < 0xA7F8) {
                if (c < 0x2D6F) {
                    if (c < 0x2128) {
                        if (c < 0x2090) {
                            if (c < 0xD8) {
                                if (c < 0xBA) {
                                    return c == 0xAA || c == 0xB5;
                                } else { // c >= 0xBA
                                    return c == 0xBA || c >= 0xC0 && c <= 0xD6;
                                }
                            } else { // c >= 0xD8
                                if (c < 0x2071) {
                                    return c >= 0xD8 && c <= 0xF6 || c >= 0xF8 && c <= 0x1FFF;
                                } else { // c >= 0x2071
                                    return c == 0x2071 || c == 0x207F;
                                }
                            }
                        } else { // c >= 0x2090
                            if (c < 0x2115) {
                                if (c < 0x2107) {
                                    return c >= 0x2090 && c <= 0x209C || c == 0x2102;
                                } else { // c >= 0x2107
                                    return c == 0x2107 || c >= 0x210A && c <= 0x2113;
                                }
                            } else { // c >= 0x2115
                                if (c < 0x2124) {
                                    return c == 0x2115 || c >= 0x2119 && c <= 0x211D;
                                } else { // c >= 0x2124
                                    return c == 0x2124 || c == 0x2126;
                                }
                            }
                        }
                    } else { // c >= 0x2128
                        if (c < 0x2C30) {
                            if (c < 0x2145) {
                                if (c < 0x212F) {
                                    return c == 0x2128 || c >= 0x212A && c <= 0x212D;
                                } else { // c >= 0x212F
                                    return c >= 0x212F && c <= 0x2139 || c >= 0x213C && c <= 0x213F;
                                }
                            } else { // c >= 0x2145
                                if (c < 0x2183) {
                                    return c >= 0x2145 && c <= 0x2149 || c == 0x214E;
                                } else { // c >= 0x2183
                                    return c >= 0x2183 && c <= 0x2184 || c >= 0x2C00 && c <= 0x2C2E;
                                }
                            }
                        } else { // c >= 0x2C30
                            if (c < 0x2D00) {
                                if (c < 0x2CEB) {
                                    return c >= 0x2C30 && c <= 0x2C5E || c >= 0x2C60 && c <= 0x2CE4;
                                } else { // c >= 0x2CEB
                                    return c >= 0x2CEB && c <= 0x2CEE || c >= 0x2CF2 && c <= 0x2CF3;
                                }
                            } else { // c >= 0x2D00
                                if (c < 0x2D2D) {
                                    return c >= 0x2D00 && c <= 0x2D25 || c == 0x2D27;
                                } else { // c >= 0x2D2D
                                    return c == 0x2D2D || c >= 0x2D30 && c <= 0x2D67;
                                }
                            }
                        }
                    }
                } else { // c >= 0x2D6F
                    if (c < 0x31F0) {
                        if (c < 0x2DD0) {
                            if (c < 0x2DB0) {
                                if (c < 0x2DA0) {
                                    return c == 0x2D6F || c >= 0x2D80 && c <= 0x2D96;
                                } else { // c >= 0x2DA0
                                    return c >= 0x2DA0 && c <= 0x2DA6 || c >= 0x2DA8 && c <= 0x2DAE;
                                }
                            } else { // c >= 0x2DB0
                                if (c < 0x2DC0) {
                                    return c >= 0x2DB0 && c <= 0x2DB6 || c >= 0x2DB8 && c <= 0x2DBE;
                                } else { // c >= 0x2DC0
                                    return c >= 0x2DC0 && c <= 0x2DC6 || c >= 0x2DC8 && c <= 0x2DCE;
                                }
                            }
                        } else { // c >= 0x2DD0
                            if (c < 0x3031) {
                                if (c < 0x2E2F) {
                                    return c >= 0x2DD0 && c <= 0x2DD6 || c >= 0x2DD8 && c <= 0x2DDE;
                                } else { // c >= 0x2E2F
                                    return c == 0x2E2F || c >= 0x3005 && c <= 0x3006;
                                }
                            } else { // c >= 0x3031
                                if (c < 0x3040) {
                                    return c >= 0x3031 && c <= 0x3035 || c >= 0x303B && c <= 0x303C;
                                } else { // c >= 0x3040
                                    return c >= 0x3040 && c <= 0x318F || c >= 0x31A0 && c <= 0x31BA;
                                }
                            }
                        }
                    } else { // c >= 0x31F0
                        if (c < 0xA67F) {
                            if (c < 0xA4D0) {
                                if (c < 0x3400) {
                                    return c >= 0x31F0 && c <= 0x31FF || c >= 0x3300 && c <= 0x337F;
                                } else { // c >= 0x3400
                                    return c >= 0x3400 && c <= 0x4DB5 || c >= 0x4E00 && c <= 0xA48C;
                                }
                            } else { // c >= 0xA4D0
                                if (c < 0xA610) {
                                    return c >= 0xA4D0 && c <= 0xA4FD || c >= 0xA500 && c <= 0xA60C;
                                } else { // c >= 0xA610
                                    return c >= 0xA610 && c <= 0xA62B || c >= 0xA640 && c <= 0xA66E;
                                }
                            }
                        } else { // c >= 0xA67F
                            if (c < 0xA78B) {
                                if (c < 0xA717) {
                                    return c >= 0xA67F && c <= 0xA697 || c >= 0xA6A0 && c <= 0xA6E5;
                                } else { // c >= 0xA717
                                    return c >= 0xA717 && c <= 0xA71F || c >= 0xA722 && c <= 0xA788;
                                }
                            } else { // c >= 0xA78B
                                if (c < 0xA7A0) {
                                    return c >= 0xA78B && c <= 0xA78E || c >= 0xA790 && c <= 0xA793;
                                } else { // c >= 0xA7A0
                                    return c >= 0xA7A0 && c <= 0xA7AA;
                                }
                            }
                        }
                    }
                }
            } else { // c >= 0xA7F8
                if (c < 0xAB20) {
                    if (c < 0xAA44) {
                        if (c < 0xA8FB) {
                            if (c < 0xA840) {
                                if (c < 0xA807) {
                                    return c >= 0xA7F8 && c <= 0xA801 || c >= 0xA803 && c <= 0xA805;
                                } else { // c >= 0xA807
                                    return c >= 0xA807 && c <= 0xA80A || c >= 0xA80C && c <= 0xA822;
                                }
                            } else { // c >= 0xA840
                                if (c < 0xA8D0) {
                                    return c >= 0xA840 && c <= 0xA873 || c >= 0xA882 && c <= 0xA8B3;
                                } else { // c >= 0xA8D0
                                    return c >= 0xA8D0 && c <= 0xA8D9 || c >= 0xA8F2 && c <= 0xA8F7;
                                }
                            }
                        } else { // c >= 0xA8FB
                            if (c < 0xA984) {
                                if (c < 0xA930) {
                                    return c == 0xA8FB || c >= 0xA900 && c <= 0xA925;
                                } else { // c >= 0xA930
                                    return c >= 0xA930 && c <= 0xA946 || c >= 0xA960 && c <= 0xA97C;
                                }
                            } else { // c >= 0xA984
                                if (c < 0xAA00) {
                                    return c >= 0xA984 && c <= 0xA9B2 || c >= 0xA9CF && c <= 0xA9D9;
                                } else { // c >= 0xAA00
                                    return c >= 0xAA00 && c <= 0xAA28 || c >= 0xAA40 && c <= 0xAA42;
                                }
                            }
                        }
                    } else { // c >= 0xAA44
                        if (c < 0xAAC0) {
                            if (c < 0xAA80) {
                                if (c < 0xAA60) {
                                    return c >= 0xAA44 && c <= 0xAA4B || c >= 0xAA50 && c <= 0xAA59;
                                } else { // c >= 0xAA60
                                    return c >= 0xAA60 && c <= 0xAA76 || c == 0xAA7A;
                                }
                            } else { // c >= 0xAA80
                                if (c < 0xAAB5) {
                                    return c >= 0xAA80 && c <= 0xAAAF || c == 0xAAB1;
                                } else { // c >= 0xAAB5
                                    return c >= 0xAAB5 && c <= 0xAAB6 || c >= 0xAAB9 && c <= 0xAABD;
                                }
                            }
                        } else { // c >= 0xAAC0
                            if (c < 0xAAF2) {
                                if (c < 0xAADB) {
                                    return c == 0xAAC0 || c == 0xAAC2;
                                } else { // c >= 0xAADB
                                    return c >= 0xAADB && c <= 0xAADD || c >= 0xAAE0 && c <= 0xAAEA;
                                }
                            } else { // c >= 0xAAF2
                                if (c < 0xAB09) {
                                    return c >= 0xAAF2 && c <= 0xAAF4 || c >= 0xAB01 && c <= 0xAB06;
                                } else { // c >= 0xAB09
                                    return c >= 0xAB09 && c <= 0xAB0E || c >= 0xAB11 && c <= 0xAB16;
                                }
                            }
                        }
                    }
                } else { // c >= 0xAB20
                    if (c < 0xFB46) {
                        if (c < 0xFB13) {
                            if (c < 0xAC00) {
                                if (c < 0xABC0) {
                                    return c >= 0xAB20 && c <= 0xAB26 || c >= 0xAB28 && c <= 0xAB2E;
                                } else { // c >= 0xABC0
                                    return c >= 0xABC0 && c <= 0xABE2 || c >= 0xABF0 && c <= 0xABF9;
                                }
                            } else { // c >= 0xAC00
                                if (c < 0xD7CB) {
                                    return c >= 0xAC00 && c <= 0xD7A3 || c >= 0xD7B0 && c <= 0xD7C6;
                                } else { // c >= 0xD7CB
                                    return c >= 0xD7CB && c <= 0xD7FB || c >= 0xF900 && c <= 0xFB06;
                                }
                            }
                        } else { // c >= 0xFB13
                            if (c < 0xFB38) {
                                if (c < 0xFB1F) {
                                    return c >= 0xFB13 && c <= 0xFB17 || c == 0xFB1D;
                                } else { // c >= 0xFB1F
                                    return c >= 0xFB1F && c <= 0xFB28 || c >= 0xFB2A && c <= 0xFB36;
                                }
                            } else { // c >= 0xFB38
                                if (c < 0xFB40) {
                                    return c >= 0xFB38 && c <= 0xFB3C || c == 0xFB3E;
                                } else { // c >= 0xFB40
                                    return c >= 0xFB40 && c <= 0xFB41 || c >= 0xFB43 && c <= 0xFB44;
                                }
                            }
                        }
                    } else { // c >= 0xFB46
                        if (c < 0xFF21) {
                            if (c < 0xFDF0) {
                                if (c < 0xFD50) {
                                    return c >= 0xFB46 && c <= 0xFBB1 || c >= 0xFBD3 && c <= 0xFD3D;
                                } else { // c >= 0xFD50
                                    return c >= 0xFD50 && c <= 0xFD8F || c >= 0xFD92 && c <= 0xFDC7;
                                }
                            } else { // c >= 0xFDF0
                                if (c < 0xFE76) {
                                    return c >= 0xFDF0 && c <= 0xFDFB || c >= 0xFE70 && c <= 0xFE74;
                                } else { // c >= 0xFE76
                                    return c >= 0xFE76 && c <= 0xFEFC || c >= 0xFF10 && c <= 0xFF19;
                                }
                            }
                        } else { // c >= 0xFF21
                            if (c < 0xFFCA) {
                                if (c < 0xFF66) {
                                    return c >= 0xFF21 && c <= 0xFF3A || c >= 0xFF41 && c <= 0xFF5A;
                                } else { // c >= 0xFF66
                                    return c >= 0xFF66 && c <= 0xFFBE || c >= 0xFFC2 && c <= 0xFFC7;
                                }
                            } else { // c >= 0xFFCA
                                if (c < 0xFFDA) {
                                    return c >= 0xFFCA && c <= 0xFFCF || c >= 0xFFD2 && c <= 0xFFD7;
                                } else { // c >= 0xFFDA
                                    return c >= 0xFFDA && c <= 0xFFDC;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells if a character can occur in an FTL identifier expression (without escaping) as other than the first
     * character. 
     * 
     * @since 2.3.22
     */
    public static boolean isFTLIdentifierPart(final char c) {
        return isFTLIdentifierStart(c) || (c >= '0' && c <= '9');  
    }
    
    /**
     * Escapes the <code>String</code> with the escaping rules of Java language
     * string literals, so it's safe to insert the value into a string literal.
     * The resulting string will not be quoted.
     * 
     * <p>All characters under UCS code point 0x20 will be escaped.
     * Where they have no dedicated escape sequence in Java, they will
     * be replaced with hexadecimal escape (<tt>\</tt><tt>u<i>XXXX</i></tt>). 
     * 
     * @see #jQuote(String)
     */ 
    public static String javaStringEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c < 0x20) {
                StringBuffer b = new StringBuffer(ln + 4);
                b.append(s.substring(0, i));
                while (true) {
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
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'a'));
                            x = c & 0xF;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'a'));
                        }
                    } else {
                        b.append(c);
                    }
                    i++;
                    if (i >= ln) {
                        return b.toString();
                    }
                    c = s.charAt(i);
                }
            } // if has to be escaped
        } // for each characters
        return s;
    }
    
    /**
     * Escapes a {@link String} to be safely insertable into a JavaScript string literal; for more see
     * {@link #jsStringEnc(String, boolean) jsStringEnc(s, false)}.
     */
    public static String javaScriptStringEnc(String s) {
        return jsStringEnc(s, false);
    }

    /**
     * Escapes a {@link String} to be safely insertable into a JSON string literal; for more see
     * {@link #jsStringEnc(String, boolean) jsStringEnc(s, true)}.
     */
    public static String jsonStringEnc(String s) {
        return jsStringEnc(s, true);
    }

    private static final int NO_ESC = 0;
    private static final int ESC_HEXA = 1;
    private static final int ESC_BACKSLASH = 3;
    
    /**
     * Escapes a {@link String} to be safely insertable into a JavaScript or a JSON string literal.
     * The resulting string will <em>not</em> be quoted; the caller must ensure that they are there in the final
     * output. Note that for JSON, the quotation marks must be {@code "}, not {@code '}, because JSON doesn't escape
     * {@code '}.
     * 
     * <p>The escaping rules guarantee that if the inside
     * of the literal is from one or more touching sections of strings escaped with this, no character sequence will
     * occur that closes the string literal or has special meaning in HTML/XML that can terminate the script section.
     * (If, however, the escaped section is preceded by or followed by strings from other sources, this can't be
     * guaranteed in some rare cases. Like <tt>x = "&lt;/${a?js_string}"</tt> might closes the "script"
     * element if {@code a} is is {@code "script>"}.)
     * 
     * The escaped characters are:
     * 
     * <table style="width: auto; border-collapse: collapse" border="1" summary="Characters escaped by jsStringEnc">
     * <tr>
     *   <th>Input
     *   <th>Output
     * <tr>
     *   <td><tt>"</tt>
     *   <td><tt>\"</tt>
     * <tr>
     *   <td><tt>'</tt> if not in JSON-mode
     *   <td><tt>\'</tt>
     * <tr>
     *   <td><tt>\</tt>
     *   <td><tt>\\</tt>
     * <tr>
     *   <td><tt>/</tt> if the method can't know that it won't be directly after <tt>&lt;</tt>
     *   <td><tt>\/</tt>
     * <tr>
     *   <td><tt>&gt;</tt> if the method can't know that it won't be directly after <tt>]]</tt> or <tt>--</tt>
     *   <td>JavaScript: <tt>\&gt;</tt>; JSON: <tt>\</tt><tt>u003E</tt>
     * <tr>
     *   <td><tt>&lt;</tt> if the method can't know that it won't be directly followed by <tt>!</tt> or <tt>?</tt> 
     *   <td><tt><tt>\</tt>u003C</tt>
     * <tr>
     *   <td>
     *     u0000-u001f (UNICODE control characters - disallowed by JSON)<br>
     *     u007f-u009f (UNICODE control characters - disallowed by JSON)
     *   <td><tt>\n</tt>, <tt>\r</tt> and such, or if there's no such dedicated escape:
     *       JavaScript: <tt>\x<i>XX</i></tt>, JSON: <tt>\<tt>u</tt><i>XXXX</i></tt>
     * <tr>
     *   <td>
     *     u2028 (Line separator - source code line-break in ECMAScript)<br>
     *     u2029 (Paragraph separator - source code line-break in ECMAScript)<br>
     *   <td><tt>\<tt>u</tt><i>XXXX</i></tt>
     * </table>
     * 
     * @since 2.3.20
     */
    public static String jsStringEnc(String s, boolean json) {
        NullArgumentException.check("s", s);
        
        int ln = s.length();
        StringBuffer sb = null;
        for (int i = 0; i < ln; i++) {
            final char c = s.charAt(i);
            final int escapeType;  // 
            if (!(c > '>' && c < 0x7F && c != '\\') && c != ' ' && !(c >= 0xA0 && c < 0x2028))  {  // skip common chars
                if (c <= 0x1F) {  // control chars range 1
                    if (c == '\n') {
                        escapeType = 'n';
                    } else if (c == '\r') {
                        escapeType = 'r';
                    } else if (c == '\f') {
                        escapeType = 'f';
                    } else if (c == '\b') {
                        escapeType = 'b';
                    } else if (c == '\t') {
                        escapeType = 't';
                    } else {
                        escapeType = ESC_HEXA;
                    }
                } else if (c == '"') {
                    escapeType = ESC_BACKSLASH;
                } else if (c == '\'') {
                    escapeType = json ? NO_ESC : ESC_BACKSLASH; 
                } else if (c == '\\') {
                    escapeType = ESC_BACKSLASH; 
                } else if (c == '/' && (i == 0 || s.charAt(i - 1) == '<')) {  // against closing elements
                    escapeType = ESC_BACKSLASH; 
                } else if (c == '>') {  // against "]]> and "-->"
                    final boolean dangerous;
                    if (i == 0) {
                        dangerous = true;
                    } else {
                        final char prevC = s.charAt(i - 1);
                        if (prevC == ']' || prevC == '-') {
                            if (i == 1) {
                                dangerous = true;
                            } else {
                                final char prevPrevC = s.charAt(i - 2);
                                dangerous = prevPrevC == prevC;
                            }
                        } else {
                            dangerous = false;
                        }
                    }
                    escapeType = dangerous ? (json ? ESC_HEXA : ESC_BACKSLASH) : NO_ESC;
                } else if (c == '<') {  // against "<!"
                    final boolean dangerous;
                    if (i == ln - 1) {
                        dangerous = true;
                    } else {
                        char nextC = s.charAt(i + 1);
                        dangerous = nextC == '!' || nextC == '?';
                    }
                    escapeType = dangerous ? ESC_HEXA : NO_ESC;
                } else if ((c >= 0x7F && c <= 0x9F)  // control chars range 2
                            || (c == 0x2028 || c == 0x2029)  // UNICODE line terminators
                            ) {
                    escapeType = ESC_HEXA;
                } else {
                    escapeType = NO_ESC;
                }
                
                if (escapeType != NO_ESC) { // If needs escaping
                    if (sb == null) {
                        sb = new StringBuffer(ln + 6);
                        sb.append(s.substring(0, i));
                    }
                    
                    sb.append('\\');
                    if (escapeType > 0x20) {
                        sb.append((char) escapeType);
                    } else if (escapeType == ESC_HEXA) {
                        if (!json && c < 0x100) {
                            sb.append('x');
                            sb.append(toHexDigit(c >> 4));
                            sb.append(toHexDigit(c & 0xF));
                        } else {
                            sb.append('u');
                            int cp = c;
                            sb.append(toHexDigit((cp >> 12) & 0xF));
                            sb.append(toHexDigit((cp >> 8) & 0xF));
                            sb.append(toHexDigit((cp >> 4) & 0xF));
                            sb.append(toHexDigit(cp & 0xF));
                        }
                    } else {  // escapeType == ESC_BACKSLASH
                        sb.append(c);
                    }
                    continue; 
                }
                // Falls through when escapeType == NO_ESC 
            }
            // Needs no escaping
                
            if (sb != null) sb.append(c);
        } // for each characters
        
        return sb == null ? s : sb.toString();
    }

    private static char toHexDigit(int d) {
        return (char) (d < 0xA ? d + '0' : d - 0xA + 'A');
    }
    
    /**
     * Parses a name-value pair list, where the pairs are separated with comma,
     * and the name and value is separated with colon.
     * The keys and values can contain only letters, digits and <tt>_</tt>. They
     * can't be quoted. White-space around the keys and values are ignored. The
     * value can be omitted if <code>defaultValue</code> is not null. When a
     * value is omitted, then the colon after the key must be omitted as well.
     * The same key can't be used for multiple times.
     * 
     * @param s the string to parse.
     *     For example: <code>"strong:100, soft:900"</code>.
     * @param defaultValue the value used when the value is omitted in a
     *     key-value pair.
     * 
     * @return the map that contains the name-value pairs.
     * 
     * @throws java.text.ParseException if the string is not a valid name-value
     *     pair list.
     */
    public static Map parseNameValuePairList(String s, String defaultValue)
    throws java.text.ParseException {
        Map map = new HashMap();
        
        char c = ' ';
        int ln = s.length();
        int p = 0;
        int keyStart;
        int valueStart;
        String key;
        String value;
        
        fetchLoop: while (true) {
            // skip ws
            while (p < ln) {
                c = s.charAt(p);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                p++;
            }
            if (p == ln) {
                break fetchLoop;
            }
            keyStart = p;

            // seek key end
            while (p < ln) {
                c = s.charAt(p);
                if (!(Character.isLetterOrDigit(c) || c == '_')) {
                    break;
                }
                p++;
            }
            if (keyStart == p) {
                throw new java.text.ParseException(
                       "Expecting letter, digit or \"_\" "
                        + "here, (the first character of the key) but found "
                        + jQuote(String.valueOf(c))
                        + " at position " + p + ".",
                        p);
            }
            key = s.substring(keyStart, p);

            // skip ws
            while (p < ln) {
                c = s.charAt(p);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                p++;
            }
            if (p == ln) {
                if (defaultValue == null) {
                    throw new java.text.ParseException(
                            "Expecting \":\", but reached "
                            + "the end of the string "
                            + " at position " + p + ".",
                            p);
                }
                value = defaultValue;
            } else if (c != ':') {
                if (defaultValue == null || c != ',') {
                    throw new java.text.ParseException(
                            "Expecting \":\" here, but found "
                            + jQuote(String.valueOf(c))
                            + " at position " + p + ".",
                            p);
                }

                // skip ","
                p++;
                
                value = defaultValue;
            } else {
                // skip ":"
                p++;
    
                // skip ws
                while (p < ln) {
                    c = s.charAt(p);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                    p++;
                }
                if (p == ln) {
                    throw new java.text.ParseException(
                            "Expecting the value of the key "
                            + "here, but reached the end of the string "
                            + " at position " + p + ".",
                            p);
                }
                valueStart = p;
    
                // seek value end
                while (p < ln) {
                    c = s.charAt(p);
                    if (!(Character.isLetterOrDigit(c) || c == '_')) {
                        break;
                    }
                    p++;
                }
                if (valueStart == p) {
                    throw new java.text.ParseException(
                            "Expecting letter, digit or \"_\" "
                            + "here, (the first character of the value) "
                            + "but found "
                            + jQuote(String.valueOf(c))
                            + " at position " + p + ".",
                            p);
                }
                value = s.substring(valueStart, p);

                // skip ws
                while (p < ln) {
                    c = s.charAt(p);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                    p++;
                }
                
                // skip ","
                if (p < ln) {
                    if (c != ',') {
                        throw new java.text.ParseException(
                                "Excpecting \",\" or the end "
                                + "of the string here, but found "
                                + jQuote(String.valueOf(c))
                                + " at position " + p + ".",
                                p);
                    } else {
                        p++;
                    }
                }
            }
            
            // store the key-value pair
            if (map.put(key, value) != null) {
                throw new java.text.ParseException(
                        "Dublicated key: "
                        + jQuote(key), keyStart);
            }
        }
        
        return map;
    }
    
    /**
     * @return whether the name is a valid XML tagname.
     * (This routine might only be 99% accurate. Should maybe REVISIT) 
     */
    static public boolean isXMLID(String name) {
        for (int i=0; i<name.length(); i++) {
            char c = name.charAt(i);
            if (i==0) {
                if (c== '-' || c=='.' || Character.isDigit(c))
                    return false;
            }
            if (!Character.isLetterOrDigit(c) && c != ':' && c != '_' && c != '-' && c!='.') {
                return false;
            }
        }
        return true;
    }
    
    /**
     * @return whether the qname matches the combination of nodeName, nsURI, and environment prefix settings. 
     */
    
    static public boolean matchesName(String qname, String nodeName, String nsURI, Environment env) {
        String defaultNS = env.getDefaultNS();
        if ((defaultNS != null) && defaultNS.equals(nsURI)) {
            return qname.equals(nodeName) 
               || qname.equals(Template.DEFAULT_NAMESPACE_PREFIX + ":" + nodeName); 
        }
        if ("".equals(nsURI)) {
            if (defaultNS != null) {
                return qname.equals(Template.NO_NS_PREFIX + ":" + nodeName);
            } else {
                return qname.equals(nodeName) || qname.equals(Template.NO_NS_PREFIX + ":" + nodeName);
            }
        }
        String prefix = env.getPrefixForNamespace(nsURI);
        if (prefix == null) {
            return false; // Is this the right thing here???
        }
        return qname.equals(prefix + ":" + nodeName);
    }
    
    /**
     * Pads the string at the left with spaces until it reaches the desired
     * length. If the string is longer than this length, then it returns the
     * unchanged string. 
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     */
    public static String leftPad(String s, int minLength) {
        return leftPad(s, minLength, ' ');
    }
    
    /**
     * Pads the string at the left with the specified character until it reaches
     * the desired length. If the string is longer than this length, then it
     * returns the unchanged string.
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     * @param filling the filling pattern.
     */
    public static String leftPad(String s, int minLength, char filling) {
        int ln = s.length();
        if (minLength <= ln) {
            return s;
        }
        
        StringBuffer res = new StringBuffer(minLength);
        
        int dif = minLength - ln;
        for (int i = 0; i < dif; i++) {
            res.append(filling);
        }
        
        res.append(s);
        
        return res.toString();
    }

    /**
     * Pads the string at the left with a filling pattern until it reaches the
     * desired length. If the string is longer than this length, then it returns
     * the unchanged string. For example: <code>leftPad('ABC', 9, '1234')</code>
     * returns <code>"123412ABC"</code>.
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     * @param filling the filling pattern. Must be at least 1 characters long.
     *     Can't be <code>null</code>.
     */
    public static String leftPad(String s, int minLength, String filling) {
        int ln = s.length();
        if (minLength <= ln) {
            return s;
        }
        
        StringBuffer res = new StringBuffer(minLength);

        int dif = minLength - ln;
        int fln = filling.length();
        if (fln == 0) {
            throw new IllegalArgumentException(
                    "The \"filling\" argument can't be 0 length string.");
        }
        int cnt = dif / fln;
        for (int i = 0; i < cnt; i++) {
            res.append(filling);
        }
        cnt = dif % fln;
        for (int i = 0; i < cnt; i++) {
            res.append(filling.charAt(i));
        }
        
        res.append(s);
        
        return res.toString();
    }
    
    /**
     * Pads the string at the right with spaces until it reaches the desired
     * length. If the string is longer than this length, then it returns the
     * unchanged string. 
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     */
    public static String rightPad(String s, int minLength) {
        return rightPad(s, minLength, ' ');
    }
    
    /**
     * Pads the string at the right with the specified character until it
     * reaches the desired length. If the string is longer than this length,
     * then it returns the unchanged string.
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     * @param filling the filling pattern.
     */
    public static String rightPad(String s, int minLength, char filling) {
        int ln = s.length();
        if (minLength <= ln) {
            return s;
        }
        
        StringBuffer res = new StringBuffer(minLength);

        res.append(s);
        
        int dif = minLength - ln;
        for (int i = 0; i < dif; i++) {
            res.append(filling);
        }
        
        return res.toString();
    }

    /**
     * Pads the string at the right with a filling pattern until it reaches the
     * desired length. If the string is longer than this length, then it returns
     * the unchanged string. For example: <code>rightPad('ABC', 9, '1234')</code>
     * returns <code>"ABC412341"</code>. Note that the filling pattern is
     * started as if you overlay <code>"123412341"</code> with the left-aligned
     * <code>"ABC"</code>, so it starts with <code>"4"</code>.
     * 
     * @param s the string that will be padded.
     * @param minLength the length to reach.
     * @param filling the filling pattern. Must be at least 1 characters long.
     *     Can't be <code>null</code>.
     */
    public static String rightPad(String s, int minLength, String filling) {
        int ln = s.length();
        if (minLength <= ln) {
            return s;
        }
        
        StringBuffer res = new StringBuffer(minLength);

        res.append(s);

        int dif = minLength - ln;
        int fln = filling.length();
        if (fln == 0) {
            throw new IllegalArgumentException(
                    "The \"filling\" argument can't be 0 length string.");
        }
        int start = ln % fln;
        int end = fln - start <= dif
                ? fln
                : start + dif;
        for (int i = start; i < end; i++) {
            res.append(filling.charAt(i));
        }
        dif -= end - start;
        int cnt = dif / fln;
        for (int i = 0; i < cnt; i++) {
            res.append(filling);
        }
        cnt = dif % fln;
        for (int i = 0; i < cnt; i++) {
            res.append(filling.charAt(i));
        }
        
        return res.toString();
    }
    
    /**
     * Converts a version number string to an integer for easy comparison.
     * The version number must start with numbers separated with
     * dots. There can be any number of such dot-separated numbers, but only
     * the first three will be considered. After the numbers arbitrary text can
     * follow, and will be ignored.
     * 
     * The string will be trimmed before interpretation.
     * 
     * @return major * 1000000 + minor * 1000 + micro
     */
    public static int versionStringToInt(String version) {
        return new Version(version).intValue();
    }

    /**
     * Tries to run {@code toString()}, but if that fails, returns a
     * {@code "[com.example.SomeClass.toString() failed: " + e + "]"} instead. Also, it returns {@code null} for
     * {@code null} parameter.
     * 
     * @since 2.3.20
     */
    public static String tryToString(Object object) {
        if (object == null) return null;
        
        try {
            return object.toString();
        } catch (Throwable e) {
            return failedToStringSubstitute(object, e);
        }
    }

    private static String failedToStringSubstitute(Object object, Throwable e) {
        String eStr;
        try {
            eStr = e.toString();
        } catch (Throwable e2) {
            eStr = ClassUtil.getShortClassNameOfObject(e);
        }
        return "[" + ClassUtil.getShortClassNameOfObject(object) +".toString() failed: " + eStr + "]";
    }
    
    /**
     * Converts {@code 1}, {@code 2}, {@code 3} and so forth to {@code "A"}, {@code "B"}, {@code "C"} and so fort. When
     * reaching {@code "Z"}, it continues like {@code "AA"}, {@code "AB"}, etc. The lowest supported number is 1, but
     * there's no upper limit.
     * 
     * @throws IllegalArgumentException
     *             If the argument is 0 or less.
     * 
     * @since 2.3.22
     */
    public static String toUpperABC(int n) {
        return toABC(n, 'A');
    }

    /**
     * Same as {@link #toUpperABC(int)}, but produces lower case result, like {@code "ab"}.
     * 
     * @since 2.3.22
     */
    public static String toLowerABC(int n) {
        return toABC(n, 'a');
    }

    /**
     * @param oneDigit
     *            The character that stands for the value 1.
     */
    private static String toABC(final int n, char oneDigit) {
        if (n < 1) {
            throw new IllegalArgumentException("Can't convert 0 or negative "
                    + "numbers to latin-number: " + n);
        }
        
        // First find out how many "digits" will we need. We start from A, then
        // try AA, then AAA, etc. (Note that the smallest digit is "A", which is
        // 1, not 0. Hence this isn't like a usual 26-based number-system):
        int reached = 1;
        int weight = 1;
        while (true) {
            int nextWeight = weight * 26;
            int nextReached = reached + nextWeight;
            if (nextReached <= n) {
                // So we will have one more digit
                weight = nextWeight;
                reached = nextReached;
            } else {
                // No more digits
                break;
            }
        }
        
        // Increase the digits of the place values until we get as close
        // to n as possible (but don't step over it).
        StringBuffer sb = new StringBuffer();
        while (weight != 0) {
            // digitIncrease: how many we increase the digit which is already 1
            final int digitIncrease = (n - reached) / weight;
            sb.append((char) (oneDigit + digitIncrease));
            reached += digitIncrease * weight;
            
            weight /= 26;
        }
        
        return sb.toString();
    }

    /**
     * Behaves exactly like {@link String#trim()}, but works on arrays. If the resulting array would have the same
     * content after trimming, it returns the original array instance. Otherwise it returns a new array instance (or
     * {@link CollectionUtils#EMPTY_CHAR_ARRAY}).
     * 
     * @since 2.3.22
     */
    public static char[] trim(final char[] cs) {
        if (cs.length == 0) {
            return cs;
        }
        
        int start = 0;
        int end = cs.length;
        while (start < end && cs[start] <= ' ') {
            start++;
        }
        while (start < end && cs[end - 1] <= ' ') {
            end--;
        }
        
        if (start == 0 && end == cs.length) {
            return cs;
        }
        if (start == end) {
            return CollectionUtils.EMPTY_CHAR_ARRAY;
        }
        
        char[] newCs = new char[end - start];
        System.arraycopy(cs, start, newCs, 0, end - start);
        return newCs;
    }

    /**
     * Tells if {@link String#trim()} will return a 0-length string for the {@link String} equivalent of the argument.
     * 
     * @since 2.3.22
     */
    public static boolean isTrimmableToEmpty(char[] text) {
        return isTrimmableToEmpty(text, 0, text.length);
    }

    /**
     * Like {@link #isTrimmableToEmpty(char[])}, but acts on a sub-array that starts at {@code start} (inclusive index).
     * 
     * @since 2.3.23
     */
    public static boolean isTrimmableToEmpty(char[] text, int start) {
        return isTrimmableToEmpty(text, start, text.length);
    }
    
    /**
     * Like {@link #isTrimmableToEmpty(char[])}, but acts on a sub-array that starts at {@code start} (inclusive index)
     * and ends at {@code end} (exclusive index).
     * 
     * @since 2.3.23
     */
    public static boolean isTrimmableToEmpty(char[] text, int start, int end) {
        for (int i = start; i < end; i++) {
            // We follow Java's String.trim() here, which simply states that c <= ' ' is whitespace.
            if (text[i] > ' ') {
                return false;
            }
        }
        return true;
    }
    
}
