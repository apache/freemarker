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

package org.apache.freemarker.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.Version;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _StringUtils {

    private static final char[] LT = new char[] { '&', 'l', 't', ';' };
    private static final char[] GT = new char[] { '&', 'g', 't', ';' };
    private static final char[] AMP = new char[] { '&', 'a', 'm', 'p', ';' };
    private static final char[] QUOT = new char[] { '&', 'q', 'u', 'o', 't', ';' };
    private static final char[] HTML_APOS = new char[] { '&', '#', '3', '9', ';' };
    private static final char[] XML_APOS = new char[] { '&', 'a', 'p', 'o', 's', ';' };

    /**
     *  XML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     */
    public static String XMLEnc(String s) {
        return XMLOrHTMLEnc(s, true, true, XML_APOS);
    }

    /**
     * Like {@link #XMLEnc(String)}, but writes the result into a {@link Writer}.
     */
    public static void XMLEnc(String s, Writer out) throws IOException {
        XMLOrHTMLEnc(s, XML_APOS, out);
    }
    
    /**
     *  XHTML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     *  suitable for XHTML decoding in common user agents (including legacy
     *  user agents, which do not decode "&amp;apos;" to "'", so "&amp;#39;" is used
     *  instead [see http://www.w3.org/TR/xhtml1/#C_16])
     */
    public static String XHTMLEnc(String s) {
        return XMLOrHTMLEnc(s, true, true, HTML_APOS);
    }

    /**
     * Like {@link #XHTMLEnc(String)}, but writes the result into a {@link Writer}.
     */
    public static void XHTMLEnc(String s, Writer out) throws IOException {
        XMLOrHTMLEnc(s, HTML_APOS, out);
    }
    
    private static String XMLOrHTMLEnc(String s, boolean escGT, boolean escQuot, char[] apos) {
        final int ln = s.length();
        
        // First we find out if we need to escape, and if so, what the length of the output will be:
        int firstEscIdx = -1;
        int lastEscIdx = 0;
        int plusOutLn = 0;
        for (int i = 0; i < ln; i++) {
            escape: do {
                final char c = s.charAt(i);
                switch (c) {
                case '<':
                    plusOutLn += LT.length - 1;
                    break;
                case '>':
                    if (!(escGT || maybeCDataEndGT(s, i))) {
                        break escape;
                    }
                    plusOutLn += GT.length - 1;
                    break;
                case '&':
                    plusOutLn += AMP.length - 1;
                    break;
                case '"':
                    if (!escQuot) {
                        break escape;
                    }
                    plusOutLn += QUOT.length - 1;
                    break;
                case '\'': // apos
                    if (apos == null) {
                        break escape;
                    }
                    plusOutLn += apos.length - 1;
                    break;
                default:
                    break escape;
                }
                
                if (firstEscIdx == -1) {
                    firstEscIdx = i;
                }
                lastEscIdx = i;
            } while (false);
        }
        
        if (firstEscIdx == -1) {
            return s; // Nothing to escape
        } else {
            final char[] esced = new char[ln + plusOutLn];
            if (firstEscIdx != 0) {
                s.getChars(0, firstEscIdx, esced, 0);
            }
            int dst = firstEscIdx;
            scan: for (int i = firstEscIdx; i <= lastEscIdx; i++) {
                final char c = s.charAt(i);
                switch (c) {
                case '<':
                    dst = shortArrayCopy(LT, esced, dst);
                    continue scan;
                case '>':
                    if (!(escGT || maybeCDataEndGT(s, i))) {
                        break;
                    }
                    dst = shortArrayCopy(GT, esced, dst);
                    continue scan;
                case '&':
                    dst = shortArrayCopy(AMP, esced, dst);
                    continue scan;
                case '"':
                    if (!escQuot) {
                        break;
                    }
                    dst = shortArrayCopy(QUOT, esced, dst);
                    continue scan;
                case '\'': // apos
                    if (apos == null) {
                        break;
                    }
                    dst = shortArrayCopy(apos, esced, dst);
                    continue scan;
                }
                esced[dst++] = c;
            }
            if (lastEscIdx != ln - 1) {
                s.getChars(lastEscIdx + 1, ln, esced, dst);
            }
            
            return String.valueOf(esced);
        }
    }
    
    private static boolean maybeCDataEndGT(String s, int i) {
        if (i == 0) return true;
        if (s.charAt(i - 1) != ']') return false;
        return i == 1 || s.charAt(i - 2) == ']';
    }

    private static void XMLOrHTMLEnc(String s, char[] apos, Writer out) throws IOException {
        int writtenEnd = 0;  // exclusive end
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                int flushLn = i - writtenEnd;
                if (flushLn != 0) {
                    out.write(s, writtenEnd, flushLn);
                }
                writtenEnd = i + 1;
                
                switch (c) {
                case '<': out.write(LT); break;
                case '>': out.write(GT); break;
                case '&': out.write(AMP); break;
                case '"': out.write(QUOT); break;
                default: out.write(apos); break;
                }
            }
        }
        if (writtenEnd < ln) {
            out.write(s, writtenEnd, ln - writtenEnd);
        }
    }
    
    /**
     * For efficiently copying very short char arrays.
     */
    private static int shortArrayCopy(char[] src, char[] dst, int dstOffset) {
        for (char aSrc : src) {
            dst[dstOffset++] = aSrc;
        }
        return dstOffset;
    }
    
    /**
     *  XML encoding without replacing apostrophes.
     *  @see #XMLEnc(String)
     */
    public static String XMLEncNA(String s) {
        return XMLOrHTMLEnc(s, true, true, null);
    }

    /**
     *  XML encoding for attribute values quoted with <tt>"</tt> (not with <tt>'</tt>!).
     *  Also can be used for HTML attributes that are quoted with <tt>"</tt>.
     *  @see #XMLEnc(String)
     */
    public static String XMLEncQAttr(String s) {
        return XMLOrHTMLEnc(s, false, true, null);
    }

    /**
     *  XML encoding without replacing apostrophes and quotation marks and
     *  greater-thans (except in {@code ]]>}).
     *  @see #XMLEnc(String)
     */
    public static String XMLEncNQG(String s) {
        return XMLOrHTMLEnc(s, false, false, null);
    }
    
    /**
     *  Rich Text Format encoding (does not replace line breaks).
     *  Escapes all '\' '{' '}'.
     */
    public static String RTFEnc(String s) {
        int ln = s.length();
        
        // First we find out if we need to escape, and if so, what the length of the output will be:
        int firstEscIdx = -1;
        int lastEscIdx = 0;
        int plusOutLn = 0;
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\\') {
                if (firstEscIdx == -1) {
                    firstEscIdx = i;
                }
                lastEscIdx = i;
                plusOutLn++;
            }
        }
        
        if (firstEscIdx == -1) {
            return s; // Nothing to escape
        } else {
            char[] esced = new char[ln + plusOutLn];
            if (firstEscIdx != 0) {
                s.getChars(0, firstEscIdx, esced, 0);
            }
            int dst = firstEscIdx;
            for (int i = firstEscIdx; i <= lastEscIdx; i++) {
                char c = s.charAt(i);
                if (c == '{' || c == '}' || c == '\\') {
                    esced[dst++] = '\\';
                }
                esced[dst++] = c;
            }
            if (lastEscIdx != ln - 1) {
                s.getChars(lastEscIdx + 1, ln, esced, dst);
            }
            
            return String.valueOf(esced);
        }
    }
    
    /**
     * Like {@link #RTFEnc(String)}, but writes the result into a {@link Writer}.
     */
    public static void RTFEnc(String s, Writer out) throws IOException {
        int writtenEnd = 0;  // exclusive end
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\\') {
                int flushLn = i - writtenEnd;
                if (flushLn != 0) {
                    out.write(s, writtenEnd, flushLn);
                }
                out.write('\\');
                writtenEnd = i; // Not i + 1, so c will be written out later
            }
        }
        if (writtenEnd < ln) {
            out.write(s, writtenEnd, ln - writtenEnd);
        }
    }
    

    /**
     * URL encoding (like%20this) for query parameter values, path <em>segments</em>, fragments; this encodes all
     * characters that are reserved anywhere.
     */
    public static String URLEnc(String s, Charset charset) throws UnsupportedEncodingException {
        return URLEnc(s, charset, false);
    }
    
    /**
     * Like {@link #URLEnc(String, Charset)} but doesn't escape the slash character ({@code /}).
     * This can be used to encode a path only if you know that no folder or file name will contain {@code /}
     * character (not in the path, but in the name itself), which usually stands, as the commonly used OS-es don't
     * allow that.
     */
    public static String URLPathEnc(String s, Charset charset) throws UnsupportedEncodingException {
        return URLEnc(s, charset, true);
    }
    
    private static String URLEnc(String s, Charset charset, boolean keepSlash)
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

        StringBuilder b = new StringBuilder(ln + ln / 3 + 2);
        b.append(s.substring(0, i));

        int encStart = i;
        for (i++; i < ln; i++) {
            char c = s.charAt(i);
            if (safeInURL(c, keepSlash)) {
                if (encStart != -1) {
                    byte[] o = s.substring(encStart, i).getBytes(charset);
                    for (byte bc : o) {
                        b.append('%');
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
            for (byte bc : o) {
                b.append('%');
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

    public static Locale deduceLocale(String input) {
       if (input == null) return null;
       Locale locale = Locale.getDefault();
       if (input.length() > 0 && input.charAt(0) == '"') input = input.substring(1, input.length() - 1);
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
       } else {
          locale = new Locale(lang, country, st.nextToken());
       }
       return locale;
    }

    public static String capitalize(String s) {
        StringTokenizer st = new StringTokenizer(s, " \t\r\n", true);
        StringBuilder buf = new StringBuilder(s.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            buf.append(tok.substring(0, 1).toUpperCase());
            buf.append(tok.substring(1).toLowerCase());
        }
        return buf.toString();
    }

    public static boolean getYesNo(String s) {
        if (s.startsWith("\"")) {
            s = s.substring(1, s.length() - 1);

        }
        if (s.equalsIgnoreCase("n")
                || s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("f")
                || s.equalsIgnoreCase("false")) {
            return false;
        } else if (s.equalsIgnoreCase("y")
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
     * 
     * @param sep
     *            The string that separates the items of the resulting array. If this is 0 length, then each character
     *            will be a separate item in the array.
     */
    public static String[] split(String s, String sep, boolean caseInsensitive) {
        int sepLn = sep.length();

        String convertedS = caseInsensitive ? s.toLowerCase() : s;
        int sLn = s.length();
        
        if (sepLn == 0) {
            String[] res = new String[sLn];
            for (int i = 0; i < sLn; i++) {
                res[i] = String.valueOf(s.charAt(i));
            }
            return res;
        }

        String splitString = caseInsensitive ? sep.toLowerCase() : sep;
        String res[];
        
        {
            int next = 0;
            int count = 1;
            while ((next = convertedS.indexOf(splitString, next)) != -1) {
                count++;
                next += sepLn;
            }
            res = new String[count];
        }

        int dst = 0;
        int next = 0;
        while (next <= sLn) {
            int end = convertedS.indexOf(splitString, next);
            if (end == -1) end = sLn;
            res[dst++] = s.substring(next, end);
            next = end + sepLn;
        }
        return res;
    }


    /**
     * Same as {@link #replace(String, String, String, boolean, boolean)} with two {@code false} parameters. 
     */
    public static String replace(String text, String oldSub, String newSub) {
        return replace(text, oldSub, newSub, false, false);
    }
    
    /**
     * Replaces all occurrences of a sub-string in a string.
     * @param text The string where it will replace <code>oldSub</code> with
     *     <code>newSub</code>.
     * @return String The string after the replacements.
     */
    public static String replace(String text, 
                                  String oldSub,
                                  String newSub,
                                  boolean caseInsensitive,
                                  boolean firstOnly) {
        StringBuilder buf;
        int tln;
        int oln = oldSub.length();
        
        if (oln == 0) {
            int nln = newSub.length();
            if (nln == 0) {
                return text;
            } else {
                if (firstOnly) {
                    return newSub + text;
                } else {
                    tln = text.length();
                    buf = new StringBuilder(tln + (tln + 1) * nln);
                    buf.append(newSub);
                    for (int i = 0; i < tln; i++) {
                        buf.append(text.charAt(i));
                        buf.append(newSub);
                    }
                    return buf.toString();
                }
            }
        } else {
            oldSub = caseInsensitive ? oldSub.toLowerCase() : oldSub;
            String input = caseInsensitive ? text.toLowerCase() : text;
            int e = input.indexOf(oldSub);
            if (e == -1) {
                return text;
            }
            int b = 0;
            tln = text.length();
            buf = new StringBuilder(
                    tln + Math.max(newSub.length() - oln, 0) * 3);
            do {
                buf.append(text.substring(b, e));
                buf.append(newSub);
                b = e + oln;
                e = input.indexOf(oldSub, b);
            } while (e != -1 && !firstOnly);
            buf.append(text.substring(b));
            return buf.toString();
        }
    }

    /**
     * Removes a line-break from the end of the string (if there's any).
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

    /**
     * Converts the parameter with <code>toString</code> (if not
     * <code>null</code>)and passes it to {@link #jQuoteNoXSS(String)}. 
     */
    public static String jQuoteNoXSS(Object obj) {
        return jQuoteNoXSS(obj != null ? obj.toString() : null);
    }
    
    /**
     * Same as {@link #jQuote(String)} but also escapes <code>'&lt;'</code>
     * as <code>\</code><code>u003C</code>. This is used for log messages to prevent XSS
     * on poorly written Web-based log viewers. 
     */
    public static String jQuoteNoXSS(String s) {
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
                StringBuilder b = new StringBuilder(ln + 4);
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
     * <p>The escaping rules guarantee that if the inside of the JavaScript/JSON string literal is from one or more
     * touching pieces that were escaped with this, no character sequence can occur that closes the
     * JavaScript/JSON string literal, or has a meaning in HTML/XML that causes the HTML script section to be closed.
     * (If, however, the escaped section is preceded by or followed by strings from other sources, this can't be
     * guaranteed in some rare cases. Like <tt>x = "&lt;/${a?jsString}"</tt> might closes the "script"
     * element if {@code a} is {@code "script>"}.)
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
     */
    public static String jsStringEnc(String s, boolean json) {
        _NullArgumentException.check("s", s);
        
        int ln = s.length();
        StringBuilder sb = null;
        for (int i = 0; i < ln; i++) {
            final char c = s.charAt(i);
            final int escapeType;  // 
            if (!(c > '>' && c < 0x7F && c != '\\') && c != ' ' && !(c >= 0xA0 && c < 0x2028)) {  // skip common chars
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
                        sb = new StringBuilder(ln + 6);
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
                            sb.append(toHexDigit((c >> 12) & 0xF));
                            sb.append(toHexDigit((c >> 8) & 0xF));
                            sb.append(toHexDigit((c >> 4) & 0xF));
                            sb.append(toHexDigit(c & 0xF));
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
     * @return whether the qname matches the combination of nodeName, nsURI, and environment prefix settings.
     */
    static public boolean matchesQName(String qname, String nodeName, String nsURI, Environment env) {
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
        
        StringBuilder res = new StringBuilder(minLength);
        
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
        
        StringBuilder res = new StringBuilder(minLength);

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
        
        StringBuilder res = new StringBuilder(minLength);

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
        
        StringBuilder res = new StringBuilder(minLength);

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
            eStr = _ClassUtils.getShortClassNameOfObject(e);
        }
        return "[" + _ClassUtils.getShortClassNameOfObject(object) + ".toString() failed: " + eStr + "]";
    }
    
    /**
     * Converts {@code 1}, {@code 2}, {@code 3} and so forth to {@code "A"}, {@code "B"}, {@code "C"} and so fort. When
     * reaching {@code "Z"}, it continues like {@code "AA"}, {@code "AB"}, etc. The lowest supported number is 1, but
     * there's no upper limit.
     * 
     * @throws IllegalArgumentException
     *             If the argument is 0 or less.
     */
    public static String toUpperABC(int n) {
        return toABC(n, 'A');
    }

    /**
     * Same as {@link #toUpperABC(int)}, but produces lower case result, like {@code "ab"}.
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
        StringBuilder sb = new StringBuilder();
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
     * {@link _CollectionUtils#EMPTY_CHAR_ARRAY}).
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
            return _CollectionUtils.EMPTY_CHAR_ARRAY;
        }
        
        char[] newCs = new char[end - start];
        System.arraycopy(cs, start, newCs, 0, end - start);
        return newCs;
    }

    /**
     * Tells if {@link String#trim()} will return a 0-length string for the {@link String} equivalent of the argument.
     */
    public static boolean isTrimmableToEmpty(char[] text) {
        return isTrimmableToEmpty(text, 0, text.length);
    }

    /**
     * Like {@link #isTrimmableToEmpty(char[])}, but acts on a sub-array that starts at {@code start} (inclusive index).
     */
    public static boolean isTrimmableToEmpty(char[] text, int start) {
        return isTrimmableToEmpty(text, start, text.length);
    }
    
    /**
     * Like {@link #isTrimmableToEmpty(char[])}, but acts on a sub-array that starts at {@code start} (inclusive index)
     * and ends at {@code end} (exclusive index).
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

    /**
     * Same as {@link #globToRegularExpression(String, boolean)} with {@code caseInsensitive} argument {@code false}.
     */
    public static Pattern globToRegularExpression(String glob) {
        return globToRegularExpression(glob, false);
    }
    
    /**
     * Creates a regular expression from a glob. The glob must use {@code /} for as file separator, not {@code \}
     * (backslash), and is always case sensitive.
     *
     * <p>This glob implementation recognizes these special characters:
     * <ul>
     *   <li>{@code ?}: Wildcard that matches exactly one character, other than {@code /} 
     *   <li>{@code *}: Wildcard that matches zero, one or multiple characters, other than {@code /}
     *   <li>{@code **}: Wildcard that matches zero, one or multiple directories. For example, {@code **}{@code /head.f3ah}
     *       matches {@code foo/bar/head.f3ah}, {@code foo/head.f3ah} and {@code head.f3ah} too. {@code **} must be either
     *       preceded by {@code /} or be at the beginning of the glob. {@code **} must be either followed by {@code /} or be
     *       at the end of the glob. When {@code **} is at the end of the glob, it also matches file names, like
     *       {@code a/**} matches {@code a/b/c.f3ah}. If the glob only consist of a {@code **}, it will be a match for
     *       everything.
     *   <li>{@code \} (backslash): Makes the next character non-special (a literal). For example {@code How\?.f3ah} will
     *       match {@code How?.f3ah}, but not {@code HowX.f3ah}. Naturally, two backslashes produce one literal backslash. 
     *   <li>{@code [}: Reserved for future purposes; can't be used
     *   <li><code>{</code>: Reserved for future purposes; can't be used
     * </ul>
     */
    public static Pattern globToRegularExpression(String glob, boolean caseInsensitive) {
        StringBuilder regex = new StringBuilder();
        
        int nextStart = 0;
        boolean escaped = false;
        int ln = glob.length();
        for (int idx = 0; idx < ln; idx++) {
            char c = glob.charAt(idx);
            if (!escaped) {
                if (c == '?') {
                    appendLiteralGlobSection(regex, glob, nextStart, idx);
                    regex.append("[^/]");
                    nextStart = idx + 1;
                } else if (c == '*') {
                    appendLiteralGlobSection(regex, glob, nextStart, idx);
                    if (idx + 1 < ln && glob.charAt(idx + 1) == '*') {
                        if (!(idx == 0 || glob.charAt(idx - 1) == '/')) {
                            throw new IllegalArgumentException(
                                    "The \"**\" wildcard must be directly after a \"/\" or it must be at the "
                                    + "beginning, in this glob: " + glob);
                        }
                        
                        if (idx + 2 == ln) { // trailing "**"
                            regex.append(".*");
                            idx++;
                        } else { // "**/"
                            if (!(idx + 2 < ln && glob.charAt(idx + 2) == '/')) {
                                throw new IllegalArgumentException(
                                        "The \"**\" wildcard must be followed by \"/\", or must be at tehe end, "
                                        + "in this glob: " + glob);
                            }
                            regex.append("(.*?/)*");
                            idx += 2;  // "*/".length()
                        }
                    } else {
                        regex.append("[^/]*");
                    }
                    nextStart = idx + 1;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '[' || c == '{') {
                    throw new IllegalArgumentException(
                            "The \"" + c + "\" glob operator is currently unsupported "
                            + "(precede it with \\ for literal matching), "
                            + "in this glob: " + glob);
                }
            } else {
                escaped = false;
            }
        }
        appendLiteralGlobSection(regex, glob, nextStart, glob.length());
        
        return Pattern.compile(regex.toString(), caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
    }

    private static void appendLiteralGlobSection(StringBuilder regex, String glob, int start, int end) {
        if (start == end) return;
        String part = unescapeLiteralGlobSection(glob.substring(start, end));
        regex.append(Pattern.quote(part));
    }

    private static String unescapeLiteralGlobSection(String s) {
        int backslashIdx = s.indexOf('\\');
        if (backslashIdx == -1) {
            return s;
        }
        int ln = s.length();
        StringBuilder sb = new StringBuilder(ln - 1);
        int nextStart = 0; 
        do {
            sb.append(s, nextStart, backslashIdx);
            nextStart = backslashIdx + 1;
        } while ((backslashIdx = s.indexOf('\\', nextStart + 1)) != -1);
        if (nextStart < ln) {
            sb.append(s, nextStart, ln);
        }
        return sb.toString();
    }

    public static String toFTLIdentifierReferenceAfterDot(String name) {
        return TemplateLanguageUtils.escapeIdentifier(name);
    }

    public static String toFTLTopLevelIdentifierReference(String name) {
        return TemplateLanguageUtils.escapeIdentifier(name);
    }

    public static String toFTLTopLevelTragetIdentifier(final String name) {
        char quotationType = 0;
        scanForQuotationType: for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(i == 0 ? TemplateLanguageUtils.isNonEscapedIdentifierStart(c) : TemplateLanguageUtils.isNonEscapedIdentifierPart(c)) && c != '@') {
                if ((quotationType == 0 || quotationType == '\\') && (c == '-' || c == '.' || c == ':')) {
                    quotationType = '\\';
                } else {
                    quotationType = '"';
                    break scanForQuotationType;
                }
            }
        }
        switch (quotationType) {
        case 0:
            return name;
        case '"':
            return TemplateLanguageUtils.toStringLiteral(name);
        case '\\':
            return TemplateLanguageUtils.escapeIdentifier(name);
        default:
            throw new BugException();
        }
    }

    public static String snakeCaseToCamelCase(String s) {
        if (s == null) {
            return null;
        }

        int wordEndIdx = s.indexOf('_');
        if (wordEndIdx == -1) {
            return s.toLowerCase();
        }

        StringBuilder sb = new StringBuilder(s.length());
        int wordStartIdx = 0;
        do {
            if (wordStartIdx < wordEndIdx) {
                char wordStartC = s.charAt(wordStartIdx);
                sb.append(sb.length() != 0 ? Character.toUpperCase(wordStartC) : Character.toLowerCase(wordStartC));
                sb.append(s.substring(wordStartIdx + 1, wordEndIdx).toLowerCase());
            }

            wordStartIdx = wordEndIdx + 1;
            wordEndIdx = s.indexOf('_', wordStartIdx);
            if (wordEndIdx == -1) {
                wordEndIdx = s.length();
            }
        } while (wordStartIdx < s.length());
        return sb.toString();
    }

    public static boolean isASCIIDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    public static boolean isUpperUSASCII(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static final Pattern NORMALIZE_EOLS_REGEXP = Pattern.compile("\\r\\n?+");

    /**
     * Converts all non UN*X End-Of-Line character sequences (CR and CRLF) to UN*X format (LF).
     * Returns {@code null} for {@code null} input.
     */
    public static String normalizeEOLs(String s) {
        if (s == null) {
            return null;
        }
        return NORMALIZE_EOLS_REGEXP.matcher(s).replaceAll("\n");
    }

}
