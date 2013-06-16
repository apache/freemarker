package freemarker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

    static final String[] UNKNOWN_DATE_TYPE_ERROR_TIPS = new String[] {
            "Use ?time, ?date or ?datetime to tell FreeMarker which parts of the date is used.",
            "For programmers: Use java.sql.Date/Time/Timestamp instead of java.util.Date in the "
            + "data-model to avoid this ambiguity."
    };
    
    static final String[] UNKNOWN_DATE_TO_STRING_TIPS = new String[] {
            "Use ?string(format) to specify which parts to display.",
            UNKNOWN_DATE_TYPE_ERROR_TIPS[0],
            UNKNOWN_DATE_TYPE_ERROR_TIPS[1]
    };

    // Can't be instantiated
    private MessageUtil() { }
        
    static String formatLocationForSimpleParsingError(Template template, int line, int column) {
        return formatLocation("in", template, line, column);
    }

    static String formatLocationForSimpleParsingError(String templateName, int line, int column) {
        return formatLocation("in", templateName, line, column);
    }

    static String formatLocationForDependentParsingError(Template template, int line, int column) {
        return formatLocation("on", template, line, column);
    }

    static String formatLocationForDependentParsingError(String templateName, int line, int column) {
        return formatLocation("on", templateName, line, column);
    }

    static String formatLocationForEvaluationError(Template template, int line, int column) {
        return formatLocation("at", template, line, column);
    }

    static String formatLocationForEvaluationError(Macro macro, int line, int column) {
        Template t = macro.getTemplate();
        return formatLocation("at", t != null ? t.getName() : null, macro.getName(), macro.isFunction(), line, column);
    }
    
    static String formatLocationForEvaluationError(String templateName, int line, int column) {
        return formatLocation("at", templateName, line, column);
    }

    private static String formatLocation(String preposition, Template template, int line, int column) {
        return formatLocation(preposition, template != null ? template.getName() : null, line, column);
    }
    
    private static String formatLocation(String preposition, String templateName, int line, int column) {
        return formatLocation(
                preposition, templateName,
                null, false,
                line, column);
    }

    private static String formatLocation(
            String preposition, String templateName,
            String macroOrFuncName, boolean isFunction,
            int line, int column) {
        String templateDesc = templateName != null
                ? "template " + StringUtil.jQuoteNoXSS(templateName)
                : "nameless template";
        return "in " + templateDesc
              + (macroOrFuncName != null
                      ? " in " + (isFunction ? "function " : "macro ") + StringUtil.jQuote(macroOrFuncName)
                      : "")
              + " "
              + preposition + " line " + line + ", column " + column;
    }

    /**
     * Returns a single line string that is no longer than {@code maxLength}.
     * If will truncate the string at line-breaks too.
     * The truncation is always signaled with a a {@code "..."} at the end of the result string.  
     */
    static String shorten(String s, int maxLength) {
        if (maxLength < 5) maxLength = 5;
        
        boolean isTruncated = false;
        
        int brIdx = s.indexOf('\n');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        };
        brIdx = s.indexOf('\r');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        }
        
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength - 3);
            isTruncated = true;
        }
        
        if (!isTruncated) {
            return s;
        } else {
            if (s.endsWith(".")) {
                if (s.endsWith("..")) {
                    if (s.endsWith("...")) {
                        return s;
                    } else {
                        return s + ".";
                    }
                } else {
                    return s + "..";
                }
            } else {
                return s + "...";
            }
        }
    }
    
    static StringBuffer appendExpressionAsUntearable(StringBuffer sb, Expression argExp) {
        boolean needParen =
                !(argExp instanceof NumberLiteral)
                && !(argExp instanceof StringLiteral)
                && !(argExp instanceof BooleanLiteral)
                && !(argExp instanceof ListLiteral)
                && !(argExp instanceof HashLiteral)
                && !(argExp instanceof Identifier)
                && !(argExp instanceof Dot)
                && !(argExp instanceof DynamicKeyName)
                && !(argExp instanceof MethodCall)
                && !(argExp instanceof BuiltIn);
        if (needParen) sb.append('(');
        sb.append(argExp.getCanonicalForm());
        if (needParen) sb.append(')');
        return sb;
    }

    static TemplateModelException newArgCntError(String methodName, int argCnt, int expectedCnt) {
        return newArgCntError(methodName, argCnt, expectedCnt, expectedCnt);
    }
    
    static TemplateModelException newArgCntError(String methodName, int argCnt, int minCnt, int maxCnt) {
        ArrayList/*<Object>*/ desc = new ArrayList(20);
        
        desc.add(methodName);
        
        desc.add("(");
        if (maxCnt != 0) desc.add("...");
        desc.add(") expects ");
        
        if (minCnt == maxCnt) {
            if (maxCnt == 0) {
                desc.add("no");
            } else {
                desc.add(new Integer(maxCnt));
            }
        } else if (maxCnt - minCnt == 1) {
            desc.add(new Integer(minCnt));
            desc.add(" or ");
            desc.add(new Integer(maxCnt));
        } else {
            desc.add(new Integer(minCnt));
            if (maxCnt != Integer.MAX_VALUE) {
                desc.add(" to ");
                desc.add(new Integer(maxCnt));
            } else {
                desc.add(" or more (unlimited)");
            }
        }
        desc.add(" argument");
        if (maxCnt > 1) desc.add("s");
        
        desc.add(" but has received ");
        if (argCnt == 0) {
            desc.add("none");
        } else {
            desc.add(new Integer(argCnt));
        }
        desc.add(".");
        
        return new _TemplateModelException(desc.toArray());
    }

    static TemplateModelException newMethodArgMustBeStringException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "string", arg);
    }

    static TemplateModelException newMethodArgMustBeNumberException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "number", arg);
    }
    
    static TemplateModelException newMethodArgMustBeBooleanException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "boolean", arg);
    }
    
    static TemplateModelException newMethodArgMustBeExtendedHashException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "extended hash", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceOrCollectionException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence or collection", arg);
    }
    
    static TemplateModelException newMethodArgUnexpectedTypeException(
            String methodName, int argIdx, String expectedType, TemplateModel arg) {
        return new _TemplateModelException(new Object[] {
                methodName, "(...) expects ", new _DelayedAOrAn(expectedType), " as argument #", new Integer(argIdx + 1),
                ", but received ", new _DelayedAOrAn(new _DelayedFTLTypeDescription(arg)), "." });
    }

    static TemplateException newInstantiatingClassNotAllowedException(String className, Environment env) {
        return new _MiscTemplateException(env, new Object[] {
                "Instantiating ", className, " is not allowed in the template for security reasons." });
    }

    /**
     * @return "a" or "an" or "a(n)" (or "" for empty string) for an FTL type name
     */
    static String getAOrAn(String s) {
        if (s == null) return null;
        if (s.length() == 0) return "";
        
        char fc = Character.toLowerCase(s.charAt(0));
        if (fc == 'a' || fc == 'e' || fc == 'i') {
            return "an";
        } else if (fc == 'h') { 
            String ls = s.toLowerCase();
            if (ls.startsWith("has") || ls.startsWith("hi")) { 
                return "a";
            } else if (ls.startsWith("ht")) { 
                return "an";
            } else {
                return "a(n)";
            }
        } else if (fc == 'u' || fc == 'o') {
            return "a(n)";
        } else {
            char sc = (s.length() > 1) ? s.charAt(1) : '\0'; 
            if (fc == 'x' && !(sc == 'a' || sc == 'e' || sc == 'i' || sc == 'a' || sc == 'o' || sc == 'u')) {
                return "an";
            } else {
                return "a";
            }
        }
    }

    static List sortMapOfExpressions(Map/*<?, Expression>*/ map) {
        ArrayList res = new ArrayList(map.entrySet());
        Collections.sort(res, 
                new Comparator() {  // for sorting to source code order
                    public int compare(Object o1, Object o2) {
                        Map.Entry ent1 = (Map.Entry) o1;
                        Expression exp1 = (Expression) ent1.getValue();
                        
                        Map.Entry ent2 = (Map.Entry) o2;
                        Expression exp2 = (Expression) ent2.getValue();
                        
                        int res = exp1.beginLine - exp2.beginLine;
                        if (res != 0) return res;
                        res = exp1.beginColumn - exp2.beginColumn;
                        if (res != 0) return res;
                        
                        if (ent1 == ent2) return 0;
                        
                        // Should never reach this
                        return ((String) ent1.getKey()).compareTo((String) ent1.getKey()); 
                    }
            
        });
        return res;
    }
    
}
