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

package freemarker.adhoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This was used for generating the JavaCC pattern and the Java method to check if a character
 * can appear in an FTL identifier.
 */
public class IdentifierCharGenerator {
    
    public static void main(String[] args) {
        new IdentifierCharGenerator().run();
    }

    private void run() {
        List<Range> ranges = generateRanges(this::generatorPredicate);
        List<Range> ranges2 = generateRanges(IdentifierCharGenerator::isFTLIdentifierStart);
        
        if (!ranges.equals(ranges2)) {
            throw new AssertionError();
        }
        
        ranges.forEach(r -> p(toJavaCC(r) + ", "));
        
        int firstSplit = 0;
        while (firstSplit < ranges.size() && ranges.get(firstSplit).getEnd() < 0x80) {
            firstSplit++;
        }
        printJava(ranges, firstSplit, "");
    }
    
    private List<Range> generateRanges(Predicate<Integer> charCodePredicate) {
        List<Range> ranges = new ArrayList<Range>();
        
        int startedRange = -1;
        int i;
        for (i = 0; i < 0x10000; i++) {
            if (charCodePredicate.test(i)) {
                if (startedRange == -1) {
                    startedRange = i;
                }
            } else {
                if (startedRange != -1) {
                    ranges.add(new Range(startedRange, i));
                }
                startedRange = -1;
            }
        }
        if (startedRange != -1) {
            ranges.add(new Range(startedRange, i));
        }
        
        return ranges;
    }
    
    private static void printJava(List<Range> ranges, int splitUntil, String indentation) {
        final int rangeCount = ranges.size();
        if (rangeCount > 2) {
            Range secondHalfStart = ranges.get(splitUntil);
            pp(indentation);
            pp("if (c < "); pp(toJavaCharCode(secondHalfStart.getStart())); p(") {");
            {
                List<Range> firstHalf = ranges.subList(0, splitUntil);
                printJava(firstHalf, (firstHalf.size() + 1) / 2, indentation + "    ");
            }
            pp(indentation);
            pp("} else { // c >= "); p(toJavaCharCode(secondHalfStart.start));
            {
                List<Range> secondHalf = ranges.subList(splitUntil, ranges.size());
                printJava(secondHalf, (secondHalf.size() + 1) / 2, indentation + "    ");
            }
            pp(indentation);
            p("}");
        } else if (rangeCount == 2) {
            pp(indentation);
            pp("return ");
            printJavaCondition(ranges.get(0));
            pp(" || ");
            printJavaCondition(ranges.get(1));
            p(";");
        } else if (rangeCount == 1) {
            pp(indentation);
            pp("return ");
            printJavaCondition(ranges.get(0));
            p(";");
        } else {
            throw new IllegalArgumentException("Empty range list");
        }
    }
    
    private static void printJavaCondition(Range range) {
        if (range.size() > 1) {
            pp("c >= "); pp(toJavaCharCode(range.getStart()));
            pp(" && c <= "); pp(toJavaCharCode(range.getEnd() - 1));
        } else {
            pp("c == "); pp(toJavaCharCode(range.getStart()));
        }
    }

    private boolean generatorPredicate(int c) {
        return isLegacyFTLIdStartChar(c)
                || (Character.isJavaIdentifierPart(c) && Character.isLetterOrDigit(c) && !(c >= '0' && c <= '9'));
    }

    private static boolean isLegacyFTLIdStartChar(int i) {
        return i == '$' || i == '_'
                || (i >= 'a' && i <= 'z')
                || (i >= '@' && i <= 'Z')
                || (i >= '\u00c0' && i <= '\u00d6')
                || (i >= '\u00d8' && i <= '\u00f6')
                || (i >= '\u00f8' && i <= '\u1fff')
                || (i >= '\u3040' && i <= '\u318f')
                || (i >= '\u3300' && i <= '\u337f')
                || (i >= '\u3400' && i <= '\u3d2d')
                || (i >= '\u4e00' && i <= '\u9fff')
                || (i >= '\uf900' && i <= '\ufaff');
    }

    private static boolean isXML11NameChar(int i) {
        return isXML11NameStartChar(i)
                || i == '-' || i == '.' || (i >= '0' && i <= '9') || i == 0xB7
                || (i >= 0x0300 && i <= 0x036F) || (i >= 0x203F && i <= 0x2040);
    }
    
    private static boolean isXML11NameStartChar(int i) {
        return i == ':' || (i >= 'A' && i <= 'Z') || i == '_' || (i >= 'a' && i <= 'z')
                || i >= 0xC0 && i <= 0xD6
                || i >= 0xD8 && i <= 0xF6
                || i >= 0xF8 && i <= 0x2FF
                || i >= 0x370 && i <= 0x37D
                || i >= 0x37F && i <= 0x1FFF
                || i >= 0x200C && i <= 0x200D
                || i >= 0x2070 && i <= 0x218F
                || i >= 0x2C00 && i <= 0x2FEF
                || i >= 0x3001 && i <= 0xD7FF
                || i >= 0xF900 && i <= 0xFDCF
                || i >= 0xFDF0 && i <= 0xFFFD;
    }

    private static String toJavaCC(Range range) {
        final int start = range.getStart(), end = range.getEnd();
        if (start == end) { 
            throw new IllegalArgumentException("Empty range");
        }
        if (end - start == 1) {
            return toJavaCCCharString(start);
        } else {
            return toJavaCCCharString(start) + " - " + toJavaCCCharString(end - 1);
        }
    }

    private static String toJavaCCCharString(int cc) {
        StringBuilder sb = new StringBuilder();

        sb.append('"');
        if (cc < 0x7F && cc >= 0x20) {
            sb.append((char) cc);
        } else {
            sb.append("\\u");
            sb.append(toHexDigit((cc >> 12) & 0xF));
            sb.append(toHexDigit((cc >> 8) & 0xF));
            sb.append(toHexDigit((cc >> 4) & 0xF));
            sb.append(toHexDigit(cc & 0xF));
        }
        sb.append('"');

        return sb.toString();
    }

    private static String toJavaCharCode(int cc) {
        StringBuilder sb = new StringBuilder();

        if (cc < 0x7F && cc >= 0x20) {
            sb.append('\'');
            sb.append((char) cc);
            sb.append('\'');
        } else {
            sb.append("0x");
            if (cc > 0xFF) {
                sb.append(toHexDigit((cc >> 12) & 0xF));
                sb.append(toHexDigit((cc >> 8) & 0xF));
            }
            sb.append(toHexDigit((cc >> 4) & 0xF));
            sb.append(toHexDigit(cc & 0xF));
        }

        return sb.toString();
    }

    private static char toHexDigit(int d) {
        return (char) (d < 0xA ? d + '0' : d - 0xA + 'A');
    }
    
    private static class Range {
        
        final int start;
        final int end;
        
        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
        
        public int size() {
            return end - start;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Range other = (Range) obj;
            if (end != other.end) return false;
            if (start != other.start) return false;
            return true;
        }
        
    }

    static void p(final Object o) {
        System.out.println(o);
    }

    static void pp(final Object o) {
        System.out.print(o);
    }
    
    static void p(final Object[] o) {
        System.out.println("[");
        for (final Object i : o) {
            System.out.println("  " + i);
        }
        System.out.println("]");
    }

    static void p() {
        System.out.println();
    }
    
    // This is a copy of the generated code (somewhat modified), so that we can compare it with the generatorPredicate.
    public static boolean isFTLIdentifierStart(int cc) {
        char c = (char) cc;
        
        if (c < 0xAA) {
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
    
}
