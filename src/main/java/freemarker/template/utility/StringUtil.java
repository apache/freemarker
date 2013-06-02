/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template.utility;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.core.Version;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 *  Some text related utilities.
 */
public class StringUtil {
    private static final char[] ESCAPES = createEscapes();

    /*
     *  For better performance most methods are folded down. Don't you scream... :)
     */

    /**
     *  HTML encoding (does not convert line breaks).
     *  Replaces all '&gt;' '&lt;' '&amp;' and '"' with entity reference
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
     *  user agents, which do not decode "&apos;" to "'", so "&#39;" is used
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
     *  XML encoding for attributes valies quoted with <tt>"</tt> (not with <tt>'</tt>!).
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
                    default: throw new RuntimeException("Bug: unexpected char");
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
                            default: throw new RuntimeException("Bug: unexpected char");
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
     * URL encoding (like%20this).
     */
    public static String URLEnc(String s, String charset)
            throws UnsupportedEncodingException {
        int ln = s.length();
        int i;
        for (i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*')) {
                break;
            }
        }
        if (i == ln) {
            // Nothing to escape
            return s;
        }

        StringBuffer b = new StringBuffer(ln + ln / 3 + 2);
        b.append(s.substring(0, i));

        int encstart = i;
        for (i++; i < ln; i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*') {
                if (encstart != -1) {
                    byte[] o = s.substring(encstart, i).getBytes(charset);
                    for (int j = 0; j < o.length; j++) {
                        b.append('%');
                        byte bc = o[j];
                        int c1 = bc & 0x0F;
                        int c2 = (bc >> 4) & 0x0F;
                        b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                        b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
                    }
                    encstart = -1;
                }
                b.append(c);
            } else {
                if (encstart == -1) {
                    encstart = i;
                }
            }
        }
        if (encstart != -1) {
            byte[] o = s.substring(encstart, i).getBytes(charset);
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
        escapes['$'] = '$';
        return escapes;
    }

    public static String FTLStringLiteralEnc(String s)
    {
        StringBuffer buf = null;
        int l = s.length();
        int el = ESCAPES.length;
        for(int i = 0; i < l; i++)
        {
            char c = s.charAt(i);
            if(c < el)
            {
                char escape = ESCAPES[c];
                switch(escape)
                {
                    case 0:
                    {
                        if (buf != null) {
                            buf.append(c);
                        }
                        break;
                    }
                    case 1:
                    {
                        if (buf == null) {
                            buf = new StringBuffer(s.length() + 3);
                            buf.append(s.substring(0, i));
                        }
                        // hex encoding for characters below 0x20
                        // that have no other escape representation
                        buf.append("\\x00");
                        int c2 = (c >> 4) & 0x0F;
                        c = (char) (c & 0x0F);
                        buf.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                        buf.append((char) (c < 10 ? c + '0' : c - 10 + 'A'));
                        break;
                    }
                    default:
                    {
                        if (buf == null) {
                            buf = new StringBuffer(s.length() + 2);
                            buf.append(s.substring(0, i));
                        }
                        buf.append('\\');
                        buf.append(escape);
                    }
                }
            } else {
                if (buf != null) {
                    buf.append(c);
                }
            }
        }
        return buf == null ? s : buf.toString();
    }

    /**
     * FTL string literal decoding.
     *
     * \\, \", \', \n, \t, \r, \b and \f will be replaced according to
     * Java rules. In additional, it knows \g, \l, \a and \{ which are
     * replaced with &lt;, >, &amp; and { respectively.
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
       Locale locale = Locale.getDefault();
       if (input.charAt(0) == '"') input = input.substring(1, input.length() -1);
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
     * Converts the parameter with <code>toString</code> (if not
     * <code>null</code>)and passes it to {@link #jQuote(String)}. 
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
                    b.append((char) (x < 0xA ? x + '0' : x - 0xA + 'A'));
                    x = c & 0xF;
                    b.append((char) (x < 0xA ? x + '0' : x - 0xA + 'A'));
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
     * as <code>\u003C</code>. This is used for log messages to prevent XSS
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
                    b.append((char) (x < 0xA ? x + '0' : x - 0xA + 'A'));
                    x = c & 0xF;
                    b.append((char) (x < 0xA ? x + '0' : x - 0xA + 'A'));
                }
            } else {
                b.append(c);
            }
        } // for each characters
        b.append('"');
        return b.toString();
    }
    
    /**
     * Escapes the <code>String</code> with the escaping rules of Java language
     * string literals, so it is safe to insert the value into a string literal.
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
     * Escapes a <code>String</code> according the JavaScript string literal
     * escaping rules. The resulting string will not be quoted.
     * 
     * <p>It escapes both <tt>'</tt> and <tt>"</tt>.
     * In additional it escapes <tt>></tt> as <tt>\></tt> (to avoid
     * <tt>&lt;/script></tt>).
     * 
     * <p>All characters under UCS code point 0x20 will be escaped.
     * Where they have no dedicated escape sequence in JavaScript, they will
     * be replaced with hexadecimal escape (<tt>\</tt><tt>u<i>XXXX</i></tt>). 
     */ 
    public static String javaScriptStringEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'' || c == '\\' || c == '>' || c < 0x20) {
                StringBuffer b = new StringBuffer(ln + 4);
                b.append(s.substring(0, i));
                while (true) {
                    if (c == '"') {
                        b.append("\\\"");
                    } else if (c == '\'') {
                        b.append("\\'");
                    } else if (c == '\\') {
                        b.append("\\\\");
                    } else if (c == '>') {
                        b.append("\\>");
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
                            b.append("\\x");
                            int x = c / 0x10;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
                            x = c & 0xF;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
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
     * Escapes a <code>String</code> according the JSON string literal
     * escaping rules. The resulting string will <em>not</em> be quoted;
     * the caller have to ensure that they are there in the final output.
     * 
     * <p>Beware, it doesn't escape <tt>'</tt>, as JSON string must be delimited with
     * <tt>"</tt>, and JSON has no <tt>\'</tt> escape either!
     * 
     * <p>It will escape <tt>/</tt> as <tt>\/</tt> if it's after <tt>&lt;</tt>,
     * to avoid <tt>&lt;/script></tt>.
     * 
     * <p>It will escape <tt>></tt> as <tt>\</tt><tt>u003E</tt> if it's after
     * <tt>]]</tt>, to avoid closing a CDATA section.
     * 
     * <p>All characters under UCS code point 0x20 will be escaped.
     * Where they have no dedicated escape sequence in JSON, they will
     * be replaced with hexadecimal escape (<tt>\</tt><tt>u<i>XXXX</i></tt>). 
     */
    public static String jsonStringEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c < 0x20
                    || (c == '/' && i > 0 && s.charAt(i -1) == '<')
                    || (c == '>' && i > 1
                        && s.charAt(i - 1) == ']' && s.charAt(i - 2) == ']')) {
                StringBuffer b = new StringBuffer(ln + 4);
                b.append(s.substring(0, i));
                while (true) {
                    if (c == '"') {
                        b.append("\\\"");
                    } else if (c == '\\') {
                        b.append("\\\\");
                    } else if (c == '/' && i > 0 && s.charAt(i -1) == '<') {
                        b.append("\\/");
                    } else if (c == '>' && i > 1
                            && s.charAt(i - 1) == ']' && s.charAt(i - 2) == ']') {
                        b.append("\\u003E");
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
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
                            x = c & 0xF;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
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
     * Makes {@code "&lt;#foo bar>"} or {@code "[#foo bar]"} from {@code "#foo bar"}, depending on the template syntax.
     * Mostly useful for showing examples in error messages.
     * 
     * @since 2.4
     */
    public static String encloseAsTag(Template t, String tagContent) {
        return t != null && t.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX
                ? "[" + tagContent + "]"
                : "<" + tagContent + ">";
                
    }

    /**
     * Tries to run toString(), but if that fails, returns a {@code "[toString failed: " + e + "]"} instead.
     * Also, it return {@code null} for {@code null} parameter.
     * 
     * @since 2.3.20
     */
    public static String tryToString(Object object) {
        if (object == null) return null;
        
        try {
            return object.toString();
        } catch (Throwable e) {
            try {
                return "[toString() failed: " + e + "]";
            } catch (Throwable e2) {
                return "[toString() failed: " + e.getClass().getName() + "]";
            }
        }
    }
    
}
