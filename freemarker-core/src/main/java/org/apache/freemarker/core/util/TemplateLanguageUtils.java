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

import java.util.HashSet;
import java.util.Set;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNodeModelEx;
import org.apache.freemarker.core.model.TemplateNullModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.impl.BeanAndStringModel;
import org.apache.freemarker.core.model.impl.BeanModel;
import org.apache.freemarker.core.model.impl.JavaMethodModel;

/**
 * Static utility methods that perform tasks specific to the FreeMarker Template Language (FTL).
 * This is meant to be used from outside FreeMarker (i.e., it's an official, published API), not just from inside it.
 */
public final class TemplateLanguageUtils {

    /**
     *  Used to look up if the chars with low code needs to be escaped, but note that it gives bad result for '=', as
     *  there the it matters if it's after '['.
     */
    private static final char[] ESCAPES = createEscapes();

    private TemplateLanguageUtils() {
        // Not meant to be instantiated
    }

    private static char[] createEscapes() {
        char[] escapes = new char['\\' + 1];
        for (int i = 0; i < 32; ++i) {
            escapes[i] = 1;
        }
        escapes['\\'] = '\\';
        escapes['\''] = '\'';
        escapes['"'] = '"';
        escapes['<'] = 'l';
        // As '=' is only escaped if it's after '[', we can't handle it here
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
     * {@code quotation}; it doesn't add the quotation marks themselves. {@code '&'}, {@code '<'}, and {@code '>'}
     * characters will be escaped.
     *
     * @param quotation Either {@code '"'} or {@code '\''}. It's assumed that the string literal whose part we calculate is
     *                  enclosed within this kind of quotation mark. Thus, the other kind of quotation character will not be
     *                  escaped in the result.
     */
    public static String escapeStringLiteralPart(String s, char quotation) {
        return escapeStringLiteralPart(s, quotation, false, true, true, true);
    }

    /**
     * Escapes a string according the FTL string literal escaping rules, assuming the literal is quoted with
     * {@code quotation}; it doesn't add the quotation marks themselves.
     *
     * @param quotation See in {@link #escapeStringLiteralPart(String, char)}
     * @param escapeAmp Whether to escape {@code '&'}
     * @param escapeLT Whether to escape {@code '<'}
     * @param escapeGT Whether to escape {@code '>'}
     */
    public static String escapeStringLiteralPart(String s, char quotation,
            boolean escapeAmp, boolean escapeLT, boolean escapeGT) {
        return escapeStringLiteralPart(s, quotation, false, escapeAmp, escapeLT, escapeGT);
    }

    /**
     * Escapes a string according the FTL string literal escaping rules; it doesn't add the quotation marks themselves.
     * As this method doesn't know if the string literal is quoted with regular quotation marks or apostrophe quote, it
     * will escape both.
     *
     * @see #escapeStringLiteralPart(String, char)
     */
    public static String escapeStringLiteralPart(String s) {
        return escapeStringLiteralPart(s, (char) 0, false);
    }

    private static String escapeStringLiteralPart(String s, char quotation, boolean addQuotation) {
        return escapeStringLiteralPart(s, quotation, addQuotation, true, true, true);
    }

    private static String escapeStringLiteralPart(String s, char quotation, boolean addQuotation,
            boolean escapeAmp, boolean escapeLT, boolean escapeGT) {
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
        StringBuilder buf = null;
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            char escape;
            if (c == '=') {
                escape = i > 0 && s.charAt(i - 1) == '[' ? '=' : 0;
            } else if (c < escLn) {
                escape = ESCAPES[c]; //
            } else if (c == '{' && i > 0 && s.charAt(i - 1) == '$') {
                escape = '{';
            } else {
                escape = 0;
            }
            if (escape == 0 || escape == otherQuotation
                    || c == '&' && !escapeAmp || c == '<' && !escapeLT || c == '>' && !escapeGT) {
                if (buf != null) {
                    buf.append(c);
                }
            } else {
                if (buf == null) {
                    buf = new StringBuilder(s.length() + 4 + (addQuotation ? 2 : 0));
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

    /**
     * Unescapes a string that was escaped to be part of an FTL string literal. The string to unescape must not include
     * the two quotation marks or two apostrophe-quotes that delimit the literal.
     * <p>
     * \\, \", \', \n, \t, \r, \b and \f will be replaced according to
     * Java rules. In additional, it knows \g, \l, \a and \{ which are
     * replaced with &lt;, &gt;, &amp; and { respectively.
     * \x works as hexadecimal character code escape. The character
     * codes are interpreted according to UCS basic plane (Unicode).
     * "f\x006Fo", "f\x06Fo" and "f\x6Fo" will be "foo".
     * "f\x006F123" will be "foo123" as the maximum number of digits is 4.
     * <p>
     * All other \X (where X is any character not mentioned above or End-of-string)
     * will cause a ParseException.
     *
     * @param s String literal <em>without</em> the surrounding quotation marks
     * @return String with all escape sequences resolved
     * @throws GenericParseException if there string contains illegal escapes
     */
    public static String unescapeStringLiteralPart(String s) throws GenericParseException {

        int idx = s.indexOf('\\');
        if (idx == -1) {
            return s;
        }

        int lidx = s.length() - 1;
        int bidx = 0;
        StringBuilder buf = new StringBuilder(lidx);
        do {
            buf.append(s.substring(bidx, idx));
            if (idx >= lidx) {
                throw new GenericParseException("The last character of string literal is backslash");
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
                case '=':
                    buf.append(c);
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
                        throw new GenericParseException("Invalid \\x escape in a string literal");
                    }
                    bidx = idx;
                    break;
                }
                default:
                    throw new GenericParseException("Invalid escape sequence (\\" + c + ") in a string literal");
            }
            idx = s.indexOf('\\', bidx);
        } while (idx != -1);
        buf.append(s.substring(bidx));

        return buf.toString();
    }

    /**
     * Creates a <em>quoted</em> FTL string literal from a string, using escaping where necessary. The result either
     * uses regular quotation marks (UCS 0x22) or apostrophe-quotes (UCS 0x27), or it will be a raw string literal
     * (like {@code r"can contain backslash anywhere"}).
     * This is decided based on the number of regular quotation marks, apostrophe-quotes, and backslashes.
     *
     * @param s The value that should be converted to an FTL string literal whose evaluated value equals to {@code s}
     */
    public static String toStringLiteral(String s) {
        if (s == null) {
            return null;
        }

        int aposCnt = 0;
        int quotCnt = 0;
        int backslashCnt = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                aposCnt++;
            } else if (c == '"') {
                quotCnt++;
            } else if (c == '\\') {
                backslashCnt++;
            }
        }

        if (backslashCnt != 0) {
            if (quotCnt == 0) {
                return "r\"" + s + "\"";
            } else if (aposCnt == 0) {
                return "r\'" + s + "\'";
            }
        }

        char quotation;
        if (aposCnt < quotCnt ) {
            quotation = '\'';
        } else {
            quotation = '\"';
        }
        return escapeStringLiteralPart(s, quotation, true);
    }

    /**
     * Tells if a character can occur on the beginning of an FTL identifier expression (without escaping).
     */
    public static boolean isNonEscapedIdentifierStart(final char c) {
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
     */
    public static boolean isNonEscapedIdentifierPart(final char c) {
        return isNonEscapedIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    /**
     * Tells if a given character, for which {@link #isNonEscapedIdentifierStart(char)} and
     * {@link #isNonEscapedIdentifierPart(char)} is {@code false}, can occur in an identifier if it's preceded by a
     * backslash. Currently it return {@code true} for these: {@code '-'}, {@code '.'} and {@code ':'}.
     */
    public static boolean isEscapedIdentifierCharacter(final char c) {
        return c == '-' || c == '.' || c == ':';
    }

    /**
     * Escapes characters in the string that can only occur in FTL identifiers (variable names) escaped.
     * This means adding a backslash before any character for which {@link #isEscapedIdentifierCharacter(char)}
     * is {@code true}. Other characters will be left unescaped, even if they aren't valid in FTL identifiers.
     *
     * @param s The identifier to escape. If {@code null}, {@code null} is returned.
     */
    public static String escapeIdentifier(String s) {
        if (s == null) {
            return null;
        }

        int ln = s.length();

        // First we find out if we need to escape, and if so, what the length of the output will be:
        int firstEscIdx = -1;
        int lastEscIdx = 0;
        int plusOutLn = 0;
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (isEscapedIdentifierCharacter(c)) {
                if (firstEscIdx == -1) {
                    firstEscIdx = i;
                }
                lastEscIdx = i;
                plusOutLn++;
            } else if (i == 0 && !isNonEscapedIdentifierStart(c)
                    || i > 0 && !isNonEscapedIdentifierPart(c)) {
                // TODO [FM3] But quoting is only allowed for target variables... that's a strange syntax anyway.
                return toStringLiteral(s);
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
                if (isEscapedIdentifierCharacter(c)) {
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
     * Returns the type description of a value with FTL terms (not plain class name), as it should be used in
     * type-related error messages and for debugging purposes. The exact format is not specified and might change over
     * time, but currently it's something like {@code "string (wrapper: f.t.SimpleString)"} or
     * {@code "sequence+hash+string (ArrayList wrapped into f.e.b.CollectionModel)"}.
     *
     * @param tm The value whose type we will describe. If {@code null}, then {@code "Null"} is returned (without the
     *           quotation marks).
     */
    public static String getTypeDescription(TemplateModel tm) {
        if (tm == null) {
            return "Null";
        } else {
            Set typeNamesAppended = new HashSet();

            StringBuilder sb = new StringBuilder();

            Class primaryInterface = getPrimaryTemplateModelInterface(tm);
            if (primaryInterface != null) {
                appendTemplateModelTypeName(sb, typeNamesAppended, primaryInterface);
            }

            appendTemplateModelTypeName(sb, typeNamesAppended, tm.getClass());

            String javaClassName;
            Class unwrappedClass = getUnwrappedClass(tm);
            if (unwrappedClass != null) {
                javaClassName = _ClassUtils.getShortClassName(unwrappedClass, true);
            } else {
                javaClassName = null;
            }

            sb.append(" (");
            String modelClassName = _ClassUtils.getShortClassName(tm.getClass(), true);
            if (javaClassName == null) {
                sb.append("wrapper: ");
                sb.append(modelClassName);
            } else {
                sb.append(javaClassName);
                sb.append(" wrapped into ");
                sb.append(modelClassName);
            }
            sb.append(")");

            return sb.toString();
        }
    }

    /**
     * Return the template language type name of the given class, as it should be shown in error messages. This show
     * less information that {@link #getTypeDescription(TemplateModel)}, but needs no instance, just a class.
     *
     * @param cl
     *         The {@link TemplateModel} subclass; not {@code null}
     */
    public static String getTypeName(Class<? extends TemplateModel> cl) {
        StringBuilder sb = new StringBuilder();
        appendTemplateModelTypeName(sb, new HashSet<>(4), cl);
        return sb.toString();
    }

    /**
     * Return the template language type name of the value as it should be shown in error messages, considering {@link
     * TemplateIterableModel} subinterfaces only.
     *
     * @param callable
     *         Can't be {@code null}.
     */
    public static String getCallableTypeName(TemplateCallableModel callable) {
        String result = "callable-of-unknown-kind";

        String d = null;
        if (callable instanceof TemplateDirectiveModel) {
            d = getDirectiveTypeName((TemplateDirectiveModel) callable);
            result = d;
        }

        if (callable instanceof TemplateFunctionModel) {
            String f = getFunctionTypeName((TemplateFunctionModel) callable);
            result = d == null ? f : d + "+" + f;
        }

        return result;
    }

    public static String getCallableTypeName(TemplateCallableModel callable, boolean calledAsFunction) {
        return calledAsFunction
                ? getFunctionTypeName((TemplateFunctionModel) callable)
                : !calledAsFunction ? getDirectiveTypeName(
                        (TemplateDirectiveModel) callable)
                        : getCallableTypeName(callable);
    }

    /**
     * Return the template language type name of the value as it should be shown in error messages, considering
     * the {@link TemplateFunctionModel} implementing part only.
     *
     * @param callable
     *         Can't be {@code null}.
     */
    public static String getFunctionTypeName(TemplateFunctionModel callable) {
        _NullArgumentException.check("callable", callable);
        return callable instanceof JavaMethodModel ? "method" : "function";
    }

    /**
     * Return the template language type name of the value as it should be shown in error messages, considering
     * the {@link TemplateDirectiveModel} implementing part only.
     *
     * @param callable
     *         Can't be {@code null}.
     */
    public static String getDirectiveTypeName(TemplateDirectiveModel callable) {
        _NullArgumentException.check("callable", callable);
        return _CoreAPI.isMacro(callable.getClass()) ? "macro" : "directive";
    }

    /**
     * Returns the {@link TemplateModel} interface that is the most characteristic of the object, or {@code null}.
     */
    private static Class<? extends TemplateModel> getPrimaryTemplateModelInterface(TemplateModel tm) {
        if (tm instanceof BeanModel) {
            if (tm instanceof BeanAndStringModel) {
                Object wrapped = ((BeanModel) tm).getWrappedObject();
                return wrapped instanceof String
                        ? TemplateStringModel.class
                        : (tm instanceof TemplateHashModelEx ? TemplateHashModelEx.class : null);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static void appendTemplateModelTypeName(
            StringBuilder sb, Set<String> typeNamesAppended, Class<? extends TemplateModel> cl) {
        int initialLength = sb.length();

        if (TemplateNodeModelEx.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "extended node");
        } else if (TemplateNodeModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "node");
        }

        if (TemplateCallableModel.class.isAssignableFrom(cl)) {
            boolean recognized = false;
            if (TemplateDirectiveModel.class.isAssignableFrom(cl)) {
                appendTypeName(sb, typeNamesAppended, _CoreAPI.isMacro(cl) ? "macro" : "directive");
                recognized = true;
            }
            if (TemplateFunctionModel.class.isAssignableFrom(cl)) {
                appendTypeName(sb, typeNamesAppended,
                        JavaMethodModel.class.isAssignableFrom(cl) ? "method" : "function");
                recognized = true;
            }
            if (!recognized && _CoreAPI.isTemplateLanguageCallable(cl)) {
                appendTypeName(sb, typeNamesAppended, "macro or function defined with template language");
            }
        }

        if (TemplateSequenceModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "sequence");
        } else if (TemplateIterableModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended,
                    TemplateCollectionModel.class.isAssignableFrom(cl) ? "collection" : "iterable");
        } else if (TemplateModelIterator.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "iterator");
        }

        if (Environment.Namespace.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "namespace");
        } else if (TemplateHashModelEx.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "extendedHash");
        } else if (TemplateHashModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "hash");
        }

        if (TemplateNumberModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "number");
        }

        if (TemplateDateModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "date_or_time_or_dateTime");
        }

        if (TemplateBooleanModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "boolean");
        }

        if (TemplateStringModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "string");
        }

        if (TemplateMarkupOutputModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "markupOutput");
        }

        if (TemplateNullModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "null");
        }

        if (sb.length() == initialLength) {
            appendTypeName(sb, typeNamesAppended, "miscTemplateModel");
        }
    }

    private static Class getUnwrappedClass(TemplateModel tm) {
        Object unwrapped;
        try {
            if (tm instanceof WrapperTemplateModel) {
                unwrapped = ((WrapperTemplateModel) tm).getWrappedObject();
            } else if (tm instanceof AdapterTemplateModel) {
                unwrapped = ((AdapterTemplateModel) tm).getAdaptedObject(Object.class);
            } else {
                unwrapped = null;
            }
        } catch (Throwable e) {
            unwrapped = null;
        }
        return unwrapped != null ? unwrapped.getClass() : null;
    }

    private static void appendTypeName(StringBuilder sb, Set typeNamesAppended, String name) {
        if (!typeNamesAppended.contains(name)) {
            if (sb.length() != 0) sb.append("+");
            sb.append(name);
            typeNamesAppended.add(name);
        }
    }

}
